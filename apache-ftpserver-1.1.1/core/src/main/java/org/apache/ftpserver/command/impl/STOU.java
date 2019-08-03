/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ftpserver.command.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;

import org.apache.ftpserver.command.AbstractCommand;
import org.apache.ftpserver.ftplet.DataConnection;
import org.apache.ftpserver.ftplet.DataConnectionFactory;
import org.apache.ftpserver.ftplet.DefaultFtpReply;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;
import org.apache.ftpserver.impl.IODataConnectionFactory;
import org.apache.ftpserver.impl.LocalizedDataTransferFtpReply;
import org.apache.ftpserver.impl.ServerFtpStatistics;
import org.apache.ftpserver.util.IoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * <code>STOU &lt;CRLF&gt;</code><br>
 * 
 * This command behaves like STOR except that the resultant file is to be
 * created in the current directory under a name unique to that directory. The
 * 150 Transfer Started response must include the name generated, See RFC1123
 * section 4.1.2.9
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class STOU extends AbstractCommand {

    private final Logger LOG = LoggerFactory.getLogger(STOU.class);

    /**
     * Execute command.
     */
    public void execute(final FtpIoSession session,
            final FtpServerContext context, final FtpRequest request)
            throws IOException, FtpException {

        try {
            // 24-10-2007 - added check if PORT or PASV is issued, see
            // https://issues.apache.org/jira/browse/FTPSERVER-110
            DataConnectionFactory connFactory = session.getDataConnection();
            if (connFactory instanceof IODataConnectionFactory) {
                InetAddress address = ((IODataConnectionFactory) connFactory)
                        .getInetAddress();
                if (address == null) {
                    session.write(new DefaultFtpReply(
                            FtpReply.REPLY_503_BAD_SEQUENCE_OF_COMMANDS,
                            "PORT or PASV must be issued first"));
                    return;
                }
            }

            // reset state variables
            session.resetState();

            String pathName = request.getArgument();

            // get filenames
            FtpFile file = null;
            try {
                String filePrefix;
                if (pathName == null) {
                    filePrefix = "ftp.dat";
                } else {
                    FtpFile dir = session.getFileSystemView().getFile(
                            pathName);
                    if (dir.isDirectory()) {
                        filePrefix = pathName + "/ftp.dat";
                    } else {
                        filePrefix = pathName;
                    }
                }

                file = session.getFileSystemView().getFile(filePrefix);
                if (file != null) {
                    file = getUniqueFile(session, file);
                }
            } catch (Exception ex) {
                LOG.debug("Exception getting file object", ex);
            }

            if (file == null) {
                session.write(LocalizedDataTransferFtpReply.translate(session, request, context,
                        FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "STOU",
                        null, null));
                return;
            }
            String fileName = file.getAbsolutePath();

            // check permission
            if (!file.isWritable()) {
                session.write(LocalizedDataTransferFtpReply.translate(session, request, context,
                        FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                        "STOU.permission", fileName, file));
                return;
            }

            // get data connection
            session.write(new DefaultFtpReply(
                    FtpReply.REPLY_150_FILE_STATUS_OKAY, "FILE: " + fileName));

            // get data from client
            boolean failure = false;
            OutputStream os = null;

            DataConnection dataConnection;
            try {
                dataConnection = session.getDataConnection().openConnection();
            } catch (Exception e) {
                LOG.debug("Exception getting the input data stream", e);
                session.write(LocalizedDataTransferFtpReply.translate(session, request, context,
                        FtpReply.REPLY_425_CANT_OPEN_DATA_CONNECTION, "STOU",
                        fileName, file));
                return;
            }

            long transSz = 0L;
            try {

                // open streams
                os = file.createOutputStream(0L);

                // transfer data
                transSz = dataConnection.transferFromClient(session.getFtpletSession(), os);

                // attempt to close the output stream so that errors in 
                // closing it will return an error to the client (FTPSERVER-119) 
                if(os != null) {
                    os.close();
                }

                LOG.info("File uploaded {}", fileName);

                // notify the statistics component
                ServerFtpStatistics ftpStat = (ServerFtpStatistics) context
                        .getFtpStatistics();
                if (ftpStat != null) {
                    ftpStat.setUpload(session, file, transSz);
                }
                
            } catch (SocketException ex) {
                LOG.debug("Socket exception during data transfer", ex);
                failure = true;
                session.write(LocalizedDataTransferFtpReply.translate(session, request, context,
                        FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED,
                        "STOU", fileName, file));
            } catch (IOException ex) {
                LOG.debug("IOException during data transfer", ex);
                failure = true;
                session
                        .write(LocalizedDataTransferFtpReply
                                .translate(
                                        session,
                                        request,
                                        context,
                                        FtpReply.REPLY_551_REQUESTED_ACTION_ABORTED_PAGE_TYPE_UNKNOWN,
                                        "STOU", fileName, file));
            } finally {
                // make sure we really close the output stream
                IoUtils.close(os);
            }

            // if data transfer ok - send transfer complete message
            if (!failure) {
                session.write(LocalizedDataTransferFtpReply.translate(session, request, context,
                        FtpReply.REPLY_226_CLOSING_DATA_CONNECTION, "STOU",
                        fileName, file, transSz));

            }
        } finally {
            session.getDataConnection().closeDataConnection();
        }

    }

    /**
     * Get unique file object.
     */
    //TODO may need synchronization
    protected FtpFile getUniqueFile(FtpIoSession session, FtpFile oldFile)
            throws FtpException {
        FtpFile newFile = oldFile;
        FileSystemView fsView = session.getFileSystemView();
        String fileName = newFile.getAbsolutePath();
        while (newFile.doesExist()) {
            newFile = fsView.getFile(fileName + '.'
                    + System.currentTimeMillis());
            if (newFile == null) {
                break;
            }
        }
        return newFile;
    }

}
