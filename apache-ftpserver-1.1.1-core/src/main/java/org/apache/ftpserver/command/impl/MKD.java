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
 * <code>MKD  &lt;SP&gt; &lt;pathname&gt; &lt;CRLF&gt;</code><br>
 * 
 * This command causes the directory specified in the pathname to be created as
 * a directory (if the pathname is absolute) or as a subdirectory of the current
 * working directory (if the pathname is relative).
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class MKD extends AbstractCommand {

    private final Logger LOG = LoggerFactory.getLogger(MKD.class);

    /**
     * Execute command.
     */
    public void execute(final FtpIoSession session,
            final FtpServerContext context, final FtpRequest request)
            throws IOException, FtpException {

        // reset state
        session.resetState();

        // argument check
        String fileName = request.getArgument();
        if (fileName == null) {
            session.write(LocalizedFileActionFtpReply.translate(session, request, context,
                    FtpReply.REPLY_501_SYNTAX_ERROR_IN_PARAMETERS_OR_ARGUMENTS,
                    "MKD", null, null));
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
                    "MKD.invalid", fileName, file));
            return;
        }

        // check permission
        fileName = file.getAbsolutePath();
        if (!file.isWritable()) {
            session.write(LocalizedFileActionFtpReply.translate(session, request, context,
                    FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                    "MKD.permission", fileName, file));
            return;
        }

        // check file existance
        if (file.doesExist()) {
            session.write(LocalizedFileActionFtpReply.translate(session, request, context,
                    FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                    "MKD.exists", fileName, file));
            return;
        }

        // now create directory
        if (file.mkdir()) {
            session.write(LocalizedFileActionFtpReply.translate(session, request, context,
                    FtpReply.REPLY_257_PATHNAME_CREATED, "MKD", fileName, file));

            // write log message
            String userName = session.getUser().getName();
            LOG.info("Directory create : " + userName + " - " + fileName);

            // notify statistics object
            ServerFtpStatistics ftpStat = (ServerFtpStatistics) context
                    .getFtpStatistics();
            ftpStat.setMkdir(session, file);

        } else {
            session.write(LocalizedFileActionFtpReply.translate(session, request, context,
                    FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "MKD",
                    fileName, file));
        }
    }
}
