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
import org.apache.ftpserver.ftplet.DefaultFtpReply;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpStatistics;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;
import org.apache.ftpserver.impl.LocalizedFtpReply;
import org.apache.ftpserver.util.DateUtils;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * Show all statistics information.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SITE_STAT extends AbstractCommand {

    /**
     * Execute command.
     */
    public void execute(final FtpIoSession session,
            final FtpServerContext context, final FtpRequest request)
            throws IOException, FtpException {

        // reset state variables
        session.resetState();

        // only administrator can execute this
        UserManager userManager = context.getUserManager();
        boolean isAdmin = userManager.isAdmin(session.getUser().getName());
        if (!isAdmin) {
            session.write(LocalizedFtpReply.translate(session, request, context,
                    FtpReply.REPLY_530_NOT_LOGGED_IN, "SITE", null));
            return;
        }

        // get statistics information
        FtpStatistics stat = context.getFtpStatistics();
        StringBuilder sb = new StringBuilder(256);
        sb.append('\n');
        sb.append("Start Time               : ").append(
                DateUtils.getISO8601Date(stat.getStartTime().getTime()))
                .append('\n');
        sb.append("File Upload Number       : ").append(
                stat.getTotalUploadNumber()).append('\n');
        sb.append("File Download Number     : ").append(
                stat.getTotalDownloadNumber()).append('\n');
        sb.append("File Delete Number       : ").append(
                stat.getTotalDeleteNumber()).append('\n');
        sb.append("File Upload Bytes        : ").append(
                stat.getTotalUploadSize()).append('\n');
        sb.append("File Download Bytes      : ").append(
                stat.getTotalDownloadSize()).append('\n');
        sb.append("Directory Create Number  : ").append(
                stat.getTotalDirectoryCreated()).append('\n');
        sb.append("Directory Remove Number  : ").append(
                stat.getTotalDirectoryRemoved()).append('\n');
        sb.append("Current Logins           : ").append(
                stat.getCurrentLoginNumber()).append('\n');
        sb.append("Total Logins             : ").append(
                stat.getTotalLoginNumber()).append('\n');
        sb.append("Current Anonymous Logins : ").append(
                stat.getCurrentAnonymousLoginNumber()).append('\n');
        sb.append("Total Anonymous Logins   : ").append(
                stat.getTotalAnonymousLoginNumber()).append('\n');
        sb.append("Current Connections      : ").append(
                stat.getCurrentConnectionNumber()).append('\n');
        sb.append("Total Connections        : ").append(
                stat.getTotalConnectionNumber()).append('\n');
        sb.append('\n');
        session.write(new DefaultFtpReply(FtpReply.REPLY_200_COMMAND_OKAY, sb
                .toString()));
    }

}
