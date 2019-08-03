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

import org.apache.ftpserver.command.AbstractCommand;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;
import org.apache.ftpserver.impl.LocalizedFileActionFtpReply;
import org.apache.ftpserver.impl.ServerFtpStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * <code>RMD  &lt;SP&gt; &lt;pathname&gt; &lt;CRLF&gt;</code><br>
 * 
 * This command causes the directory specified in the pathname to be removed as
 * a directory (if the pathname is absolute) or as a subdirectory of the current
 * working directory (if the pathname is relative).
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class RMD extends AbstractCommand {

    private final Logger LOG = LoggerFactory.getLogger(RMD.class);

    /**
     * Execute command.
     */
    public void execute(final FtpIoSession session,
            final FtpServerContext context, final FtpRequest request)
            throws IOException, FtpException {

        // reset state variables
        session.resetState();

        // argument check
        String fileName = request.getArgument();
        if (fileName == null) {
            session.write(LocalizedFileActionFtpReply.translate(session, request, context,
                    FtpReply.REPLY_501_SYNTAX_ERROR_IN_PARAMETERS_OR_ARGUMENTS,
                    "RMD", null, null));
            return;
        }

        // get file object
        FtpFile file = null;
        try {
            file = session.getFileSystemView().getFile(fileName);
        } catch (Exception ex) {
            LOG.debug("Exception getting file object", ex);
        }
        if (file == null) {
            session.write(LocalizedFileActionFtpReply.translate(session, request, context,
                    FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                    "RMD.permission", fileName, file));
            return;
        }

        fileName = file.getAbsolutePath();

        // first let's make sure the path is a directory 
        if (!file.isDirectory()) {
            session.write(LocalizedFileActionFtpReply.translate(session, request, context,
                    FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                    "RMD.invalid", fileName, file));
            return;
        }

        // then make sure that the client did not request the deletion of 
        // current working directory.   
        FtpFile cwd = session.getFileSystemView().getWorkingDirectory();
        if(file.equals(cwd)) {
            session.write(LocalizedFileActionFtpReply.translate(session, request, context,
                FtpReply.REPLY_450_REQUESTED_FILE_ACTION_NOT_TAKEN, "RMD.busy",
                fileName, file));
            return;
        }
        
        // now check to see if the user have permission to delete this directory 
        if (!file.isRemovable()) {
            session.write(LocalizedFileActionFtpReply.translate(session, request, context,
                    FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                    "RMD.permission", fileName, file));
            return;
        }

        // now delete directory
        if (file.delete()) {
            session.write(LocalizedFileActionFtpReply.translate(session, request, context,
                    FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY, "RMD",
                    fileName, file));

            // write log message
            String userName = session.getUser().getName();
            LOG.info("Directory remove : " + userName + " - " + fileName);

            // notify statistics object
            ServerFtpStatistics ftpStat = (ServerFtpStatistics) context
                    .getFtpStatistics();
            ftpStat.setRmdir(session, file);

        } else {
        	 
            session.write(LocalizedFileActionFtpReply.translate(session, request, context,
                    FtpReply.REPLY_450_REQUESTED_FILE_ACTION_NOT_TAKEN, "RMD",
                    fileName, file));
        }
    }
}
