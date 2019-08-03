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
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;
import org.apache.ftpserver.impl.LocalizedFileActionFtpReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * <code>CDUP &lt;CRLF&gt;</code><br>
 * 
 * This command is a special case of CWD, and is included to simplify the
 * implementation of programs for transferring directory trees between operating
 * systems having different syntaxes for naming the parent directory. The reply
 * codes shall be identical to the reply codes of CWD.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class CDUP extends AbstractCommand {

    private final Logger LOG = LoggerFactory.getLogger(CDUP.class);

    /**
     * Execute command.
     */
    public void execute(final FtpIoSession session,
            final FtpServerContext context, final FtpRequest request)
            throws IOException, FtpException {

        // reset state variables
        session.resetState();

        // change directory
        FileSystemView fsview = session.getFileSystemView();
        boolean success = false;
        try {
            success = fsview.changeWorkingDirectory("..");
        } catch (Exception ex) {
            LOG.debug("Failed to change directory in file system", ex);
        }
        FtpFile cwd = fsview.getWorkingDirectory();
        if (success) {
            String dirName = cwd.getAbsolutePath();
            session.write(LocalizedFileActionFtpReply.translate(session, request, context,
                    FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY, "CDUP",
                    dirName, cwd));
        } else {
            session.write(LocalizedFileActionFtpReply
                    .translate(session, request, context,
                            FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                            "CDUP", null, cwd));
        }
    }
}
