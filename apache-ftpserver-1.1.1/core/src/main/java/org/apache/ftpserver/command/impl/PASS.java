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
import java.net.InetSocketAddress;

import org.apache.ftpserver.command.AbstractCommand;
import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;
import org.apache.ftpserver.impl.LocalizedFtpReply;
import org.apache.ftpserver.impl.ServerFtpStatistics;
import org.apache.ftpserver.usermanager.AnonymousAuthentication;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.usermanager.impl.UserMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * <code>PASS &lt;SP&gt; <password> &lt;CRLF&gt;</code><br>
 * 
 * The argument field is a Telnet string specifying the user's password. This
 * command must be immediately preceded by the user name command.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class PASS extends AbstractCommand {

    private final Logger LOG = LoggerFactory.getLogger(PASS.class);

    /**
     * Execute command.
     */
    public void execute(final FtpIoSession session,
            final FtpServerContext context, final FtpRequest request)
            throws IOException, FtpException {

        boolean success = false;

        ServerFtpStatistics stat = (ServerFtpStatistics) context
                .getFtpStatistics();
        try {

            // reset state variables
            session.resetState();

            // argument check
            String password = request.getArgument();


            // check user name
            String userName = session.getUserArgument();

            if (userName == null && session.getUser() == null) {
                session.write(LocalizedFtpReply.translate(session, request, context,
                        FtpReply.REPLY_503_BAD_SEQUENCE_OF_COMMANDS, "PASS",
                        null));
                return;
            }

            // already logged-in
            if (session.isLoggedIn()) {
                session.write(LocalizedFtpReply.translate(session, request, context,
                        FtpReply.REPLY_202_COMMAND_NOT_IMPLEMENTED, "PASS",
                        null));
                return;
            }

            // anonymous login limit check

            boolean anonymous = userName != null
                    && userName.equals("anonymous");
            if (anonymous) {
                int currAnonLogin = stat.getCurrentAnonymousLoginNumber();
                int maxAnonLogin = context.getConnectionConfig()
                        .getMaxAnonymousLogins();
                if(maxAnonLogin == 0) {
                    LOG.debug("Currently {} anonymous users logged in, unlimited allowed", currAnonLogin);
                } else {
                    LOG.debug("Currently {} out of {} anonymous users logged in", currAnonLogin, maxAnonLogin);
                }

                if (currAnonLogin >= maxAnonLogin) {
                    LOG.debug("Too many anonymous users logged in, user will be disconnected");
                    session
                            .write(LocalizedFtpReply
                                    .translate(
                                            session,
                                            request,
                                            context,
                                            FtpReply.REPLY_421_SERVICE_NOT_AVAILABLE_CLOSING_CONTROL_CONNECTION,
                                            "PASS.anonymous", null));
                    return;
                }
            }

            // login limit check
            int currLogin = stat.getCurrentLoginNumber();
            int maxLogin = context.getConnectionConfig().getMaxLogins();
            if(maxLogin == 0) {
                LOG.debug("Currently {} users logged in, unlimited allowed", currLogin);
            } else {
                LOG.debug("Currently {} out of {} users logged in", currLogin, maxLogin);
            }
            if (maxLogin != 0 && currLogin >= maxLogin) {
                LOG.debug("Too many users logged in, user will be disconnected");
                session
                        .write(LocalizedFtpReply
                                .translate(
                                        session,
                                        request,
                                        context,
                                        FtpReply.REPLY_421_SERVICE_NOT_AVAILABLE_CLOSING_CONTROL_CONNECTION,
                                        "PASS.login", null));
                return;
            }

            // authenticate user
            UserManager userManager = context.getUserManager();
            User authenticatedUser = null;
            try {
                UserMetadata userMetadata = new UserMetadata();

                if (session.getRemoteAddress() instanceof InetSocketAddress) {
                    userMetadata.setInetAddress(((InetSocketAddress) session
                            .getRemoteAddress()).getAddress());
                }
                userMetadata.setCertificateChain(session
                        .getClientCertificates());

                Authentication auth;
                if (anonymous) {
                    auth = new AnonymousAuthentication(userMetadata);
                } else {
                    auth = new UsernamePasswordAuthentication(userName,
                            password, userMetadata);
                }

                authenticatedUser = userManager.authenticate(auth);
            } catch (AuthenticationFailedException e) {
                LOG.warn("User failed to log in");
            } catch (Exception e) {
                authenticatedUser = null;
                LOG.warn("PASS.execute()", e);
            }

            // first save old values so that we can reset them if Ftplets
            // tell us to fail
            User oldUser = session.getUser();
            String oldUserArgument = session.getUserArgument();
            int oldMaxIdleTime = session.getMaxIdleTime();

            if (authenticatedUser != null) {
                if(!authenticatedUser.getEnabled()) {
                    session
                    .write(LocalizedFtpReply
                            .translate(
                                    session,
                                    request,
                                    context,
                                    FtpReply.REPLY_530_NOT_LOGGED_IN,
                                    "PASS", null));
                    return;
                }

                
                session.setUser(authenticatedUser);
                session.setUserArgument(null);
                session.setMaxIdleTime(authenticatedUser.getMaxIdleTime());
                success = true;
            } else {
                session.setUser(null);
            }

            if (!success) {
                // reset due to failure
                session.setUser(oldUser);
                session.setUserArgument(oldUserArgument);
                session.setMaxIdleTime(oldMaxIdleTime);

                delayAfterLoginFailure(context.getConnectionConfig()
                        .getLoginFailureDelay());

                LOG.warn("Login failure - " + userName);
                session.write(LocalizedFtpReply.translate(session, request, context,
                        FtpReply.REPLY_530_NOT_LOGGED_IN, "PASS", userName));
                stat.setLoginFail(session);

                session.increaseFailedLogins();

                // kick the user if the max number of failed logins is reached
                int maxAllowedLoginFailues = context.getConnectionConfig()
                        .getMaxLoginFailures();
                if (maxAllowedLoginFailues != 0
                        && session.getFailedLogins() >= maxAllowedLoginFailues) {
                    LOG.warn("User exceeded the number of allowed failed logins, session will be closed");

                    session.close(false).awaitUninterruptibly(10000);
                }

                return;
            }

            // update different objects
            FileSystemFactory fmanager = context.getFileSystemManager();
            FileSystemView fsview = fmanager
                    .createFileSystemView(authenticatedUser);
            session.setLogin(fsview);
            stat.setLogin(session);

            // everything is fine - send login ok message
            session.write(LocalizedFtpReply.translate(session, request, context,
                    FtpReply.REPLY_230_USER_LOGGED_IN, "PASS", userName));
            if (anonymous) {
                LOG.info("Anonymous login success - " + password);
            } else {
                LOG.info("Login success - " + userName);
            }

        } finally {

            // if login failed - reset user
            if (!success) {
                session.reinitialize();
            }
        }
    }

    private void delayAfterLoginFailure(final int loginFailureDelay) {

        if (loginFailureDelay > 0) {
            LOG.debug("Waiting for " + loginFailureDelay
                    + " milliseconds due to login failure");

            try {
                Thread.sleep(loginFailureDelay);
            } catch (InterruptedException e) {
                // ignore and go on
            }
        }
    }
}
