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
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;
import org.apache.ftpserver.impl.LocalizedFtpReply;
import org.apache.ftpserver.usermanager.impl.TransferRateRequest;
import org.apache.ftpserver.usermanager.impl.WriteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * This SITE command returns the specified user information.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SITE_DESCUSER extends AbstractCommand {

    private final Logger LOG = LoggerFactory.getLogger(SITE_DESCUSER.class);

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

        // get the user name
        String argument = request.getArgument();
        int spIndex = argument.indexOf(' ');
        if (spIndex == -1) {
            session.write(LocalizedFtpReply.translate(session, request, context,
                    FtpReply.REPLY_503_BAD_SEQUENCE_OF_COMMANDS,
                    "SITE.DESCUSER", null));
            return;
        }
        String userName = argument.substring(spIndex + 1);

        // check the user existance
        UserManager usrManager = context.getUserManager();
        User user = null;
        try {
            if (usrManager.doesExist(userName)) {
                user = usrManager.getUserByName(userName);
            }
        } catch (FtpException ex) {
            LOG.debug("Exception trying to get user from user manager", ex);
        }
        if (user == null) {
            session.write(LocalizedFtpReply.translate(session, request, context,
                    FtpReply.REPLY_501_SYNTAX_ERROR_IN_PARAMETERS_OR_ARGUMENTS,
                    "SITE.DESCUSER", userName));
            return;
        }

        // send the user information
        StringBuilder sb = new StringBuilder(128);
        sb.append("\n");
        sb.append("userid          : ").append(user.getName()).append("\n");
        sb.append("userpassword    : ********\n");
        sb.append("homedirectory   : ").append(user.getHomeDirectory()).append(
                "\n");
        sb.append("writepermission : ").append(
                user.authorize(new WriteRequest()) != null).append("\n");
        sb.append("enableflag      : ").append(user.getEnabled()).append("\n");
        sb.append("idletime        : ").append(user.getMaxIdleTime()).append(
                "\n");

        TransferRateRequest transferRateRequest = new TransferRateRequest();
        transferRateRequest = (TransferRateRequest) session.getUser()
                .authorize(transferRateRequest);

        if (transferRateRequest != null) {
            sb.append("uploadrate      : ").append(
                    transferRateRequest.getMaxUploadRate()).append("\n");
            sb.append("downloadrate    : ").append(
                    transferRateRequest.getMaxDownloadRate()).append("\n");
        } else {
            sb.append("uploadrate      : 0\n");
            sb.append("downloadrate    : 0\n");
        }
        sb.append('\n');
        session.write(new DefaultFtpReply(FtpReply.REPLY_200_COMMAND_OKAY, sb
                .toString()));
    }

}
