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
import org.apache.ftpserver.impl.LocalizedRenameFtpReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * <code>RNTO &lt;SP&gt; &lt;pathname&gt; &lt;CRLF&gt;</code><br>
 * 
 * This command specifies the new pathname of the file specified in the
 * immediately preceding "rename from" command. Together the two commands cause
 * a file to be renamed.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class RNTO extends AbstractCommand {

    private final Logger LOG = LoggerFactory.getLogger(RNTO.class);

    /**
     * Execute command.
     */
    public void execute(final FtpIoSession session,
            final FtpServerContext context, final FtpRequest request)
            throws IOException, FtpException {
        try {

            // argument check
            String toFileStr = request.getArgument();
            if (toFileStr == null) {
                session
                        .write(LocalizedRenameFtpReply
                                .translate(
                                        session,
                                        request,
                                        context,
                                        FtpReply.REPLY_501_SYNTAX_ERROR_IN_PARAMETERS_OR_ARGUMENTS,
                                        "RNTO", null, null, null));
                return;
            }

            // get the "rename from" file object
            FtpFile frFile = session.getRenameFrom();
            if (frFile == null) {
                session.write(LocalizedRenameFtpReply.translate(session, request, context,
                        FtpReply.REPLY_503_BAD_SEQUENCE_OF_COMMANDS, "RNTO",
                        null, null, null));
                return;
            }

            // get target file
            FtpFile toFile = null;
            try {
                toFile = session.getFileSystemView().getFile(toFileStr);
            } catch (Exception ex) {
                LOG.debug("Exception getting file object", ex);
            }
            if (toFile == null) {
                session
                        .write(LocalizedRenameFtpReply
                                .translate(
                                        session,
                                        request,
                                        context,
                                        FtpReply.REPLY_553_REQUESTED_ACTION_NOT_TAKEN_FILE_NAME_NOT_ALLOWED,
                                        "RNTO.invalid", null, frFile, toFile));
                return;
            }
            toFileStr = toFile.getAbsolutePath();

            // check permission
            if (!toFile.isWritable()) {
                session
                        .write(LocalizedRenameFtpReply
                                .translate(
                                        session,
                                        request,
                                        context,
                                        FtpReply.REPLY_553_REQUESTED_ACTION_NOT_TAKEN_FILE_NAME_NOT_ALLOWED,
                                        "RNTO.permission", null, frFile, toFile));
                return;
            }

            // check file existence
            if (!frFile.doesExist()) {
                session
                        .write(LocalizedRenameFtpReply
                                .translate(
                                        session,
                                        request,
                                        context,
                                        FtpReply.REPLY_553_REQUESTED_ACTION_NOT_TAKEN_FILE_NAME_NOT_ALLOWED,
                                        "RNTO.missing", null, frFile, toFile));
                return;
            }

            // save away the old path
            String logFrFileAbsolutePath = frFile.getAbsolutePath();
            
            // now rename
            if (frFile.move(toFile)) {
                session.write(LocalizedRenameFtpReply.translate(session, request, context,
                        FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY, "RNTO",
                        toFileStr, frFile, toFile));

                LOG.info("File rename from \"{}\" to \"{}\"", logFrFileAbsolutePath, 
                        toFile.getAbsolutePath());
            } else {
                session
                        .write(LocalizedRenameFtpReply
                                .translate(
                                        session,
                                        request,
                                        context,
                                        FtpReply.REPLY_553_REQUESTED_ACTION_NOT_TAKEN_FILE_NAME_NOT_ALLOWED,
                                        "RNTO", toFileStr, frFile, toFile));
            }

        } finally {
            session.resetState();
        }
    }

}
