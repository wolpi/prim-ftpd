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
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;
import org.apache.ftpserver.impl.LocalizedFtpReply;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * <code>SYST &lt;CRLF&gt;</code><br>
 * 
 * This command is used to find out the type of operating system at the server.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SYST extends AbstractCommand {

    /**
     * Execute command
     */
    public void execute(final FtpIoSession session,
            final FtpServerContext context, final FtpRequest request)
            throws IOException {

        // reset state variables
        session.resetState();

        // get server system info
        String systemName = System.getProperty("os.name");
        if (systemName == null) {
            systemName = "UNKNOWN";
        } else {
            systemName = systemName.toUpperCase();
            systemName = systemName.replace(' ', '-');
        }
        // print server system info
        session.write(LocalizedFtpReply.translate(session, request, context,
                FtpReply.REPLY_215_NAME_SYSTEM_TYPE, "SYST", systemName));
    }

}
