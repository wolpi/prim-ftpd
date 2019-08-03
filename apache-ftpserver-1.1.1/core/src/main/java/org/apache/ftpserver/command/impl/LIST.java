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
import java.net.InetAddress;
import java.net.SocketException;

import org.apache.ftpserver.command.AbstractCommand;
import org.apache.ftpserver.command.impl.listing.DirectoryLister;
import org.apache.ftpserver.command.impl.listing.LISTFileFormater;
import org.apache.ftpserver.command.impl.listing.ListArgument;
import org.apache.ftpserver.command.impl.listing.ListArgumentParser;
import org.apache.ftpserver.ftplet.DataConnection;
import org.apache.ftpserver.ftplet.DataConnectionFactory;
import org.apache.ftpserver.ftplet.DefaultFtpReply;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;
import org.apache.ftpserver.impl.IODataConnectionFactory;
import org.apache.ftpserver.impl.LocalizedDataTransferFtpReply;
import org.apache.ftpserver.impl.LocalizedFtpReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * <code>LIST [&lt;SP&gt; &lt;pathname&gt;] &lt;CRLF&gt;</code><br>
 * 
 * This command causes a list to be sent from the server to the passive DTP. If
 * the pathname specifies a directory or other group of files, the server should
 * transfer a list of files in the specified directory. If the pathname
 * specifies a file then the server should send current information on the file.
 * A null argument implies the user's current working or default directory. The
 * data transfer is over the data connection.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class LIST extends AbstractCommand {

    private final Logger LOG = LoggerFactory.getLogger(LIST.class);

    private static final LISTFileFormater LIST_FILE_FORMATER = new LISTFileFormater();

    private final DirectoryLister directoryLister = new DirectoryLister();

    /**
     * Execute command.
     */
    public void execute(final FtpIoSession session,
            final FtpServerContext context, final FtpRequest request)
            throws IOException, FtpException {

        try {

            // reset state variables
            session.resetState();

            // parse argument
            ListArgument parsedArg = ListArgumentParser.parse(request
                    .getArgument());

            // checl that the directory or file exists
            FtpFile file = session.getFileSystemView().getFile(parsedArg.getFile());
            
            if(!file.doesExist()) {
                LOG.debug("Listing on a non-existing file");
                session.write(LocalizedDataTransferFtpReply.translate(session, request, context,
                        FtpReply.REPLY_450_REQUESTED_FILE_ACTION_NOT_TAKEN, "LIST",
                        null, file));             
                return;
            }
            
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

            // get data connection
            session.write(LocalizedFtpReply.translate(session, request, context,
                    FtpReply.REPLY_150_FILE_STATUS_OKAY, "LIST", null));

            DataConnection dataConnection;
            try {
                dataConnection = session.getDataConnection().openConnection();
            } catch (Exception e) {
                LOG.debug("Exception getting the output data stream", e);
                session.write(LocalizedDataTransferFtpReply.translate(session, request, context,
                        FtpReply.REPLY_425_CANT_OPEN_DATA_CONNECTION, "LIST",
                        null, file));
                return;
            }

            // transfer listing data
            boolean failure = false;
            String dirList = directoryLister.listFiles(parsedArg, 
            	session.getFileSystemView(), LIST_FILE_FORMATER);
            try {
                dataConnection.transferToClient(session.getFtpletSession(), dirList);
            } catch (SocketException ex) {
                LOG.debug("Socket exception during list transfer", ex);
                failure = true;
                session.write(LocalizedDataTransferFtpReply.translate(session, request, context,
                        FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED,
                        "LIST", null, file));
            } catch (IOException ex) {
                LOG.debug("IOException during list transfer", ex);
                failure = true;
                session
                        .write(LocalizedDataTransferFtpReply
                                .translate(
                                        session,
                                        request,
                                        context,
                                        FtpReply.REPLY_551_REQUESTED_ACTION_ABORTED_PAGE_TYPE_UNKNOWN,
                                        "LIST", null, file));
            } catch (IllegalArgumentException e) {
                LOG.debug("Illegal list syntax: " + request.getArgument(), e);
                // if listing syntax error - send message
                session
                        .write(LocalizedDataTransferFtpReply
                                .translate(
                                        session,
                                        request,
                                        context,
                                        FtpReply.REPLY_501_SYNTAX_ERROR_IN_PARAMETERS_OR_ARGUMENTS,
                                        "LIST", null, file));
            }

            // if data transfer ok - send transfer complete message
            if (!failure) {
                session.write(LocalizedDataTransferFtpReply.translate(session, request, context,
                        FtpReply.REPLY_226_CLOSING_DATA_CONNECTION, "LIST",
                        null, file, dirList.length()));
            }
        } finally {
            session.getDataConnection().closeDataConnection();
        }
    }

}
