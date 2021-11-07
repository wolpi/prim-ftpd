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

package org.apache.ftpserver.impl;

import java.io.IOException;
import java.nio.charset.MalformedInputException;

import org.apache.ftpserver.command.Command;
import org.apache.ftpserver.command.CommandFactory;
import org.apache.ftpserver.ftplet.DefaultFtpReply;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.apache.ftpserver.ftpletcontainer.FtpletContainer;
import org.apache.ftpserver.listener.Listener;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.write.WriteToClosedSessionException;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 */
public class DefaultFtpHandler implements FtpHandler {

    private final Logger LOG = LoggerFactory.getLogger(DefaultFtpHandler.class);

    private final static String[] NON_AUTHENTICATED_COMMANDS = new String[] {
            "USER", "PASS", "AUTH", "QUIT", "PROT", "PBSZ" };

    private FtpServerContext context;

    private Listener listener;

    public void init(final FtpServerContext context, final Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void sessionCreated(final FtpIoSession session) throws Exception {
        session.setListener(listener);
        
        ServerFtpStatistics stats = ((ServerFtpStatistics) context
                .getFtpStatistics());

        if (stats != null) {
            stats.setOpenConnection(session);
        }
    }

    public void sessionOpened(final FtpIoSession session) throws Exception {
        FtpletContainer ftplets = context.getFtpletContainer();

        FtpletResult ftpletRet;
        try {
            ftpletRet = ftplets.onConnect(session.getFtpletSession());
        } catch (Exception e) {
            LOG.debug("Ftplet threw exception", e);
            ftpletRet = FtpletResult.DISCONNECT;
        }
        if (ftpletRet == FtpletResult.DISCONNECT) {
            LOG.debug("Ftplet returned DISCONNECT, session will be closed");
            session.close(false).awaitUninterruptibly(10000);
        } else {
            session.updateLastAccessTime();
            
            session.write(LocalizedFtpReply.translate(session, null, context,
                    FtpReply.REPLY_220_SERVICE_READY, null, null));
        }
    }

    public void sessionClosed(final FtpIoSession session) throws Exception {
        LOG.debug("Closing session");
        try {
            context.getFtpletContainer().onDisconnect(
                    session.getFtpletSession());
        } catch (Exception e) {
            // swallow the exception, we're closing down the session anyways
            LOG.warn("Ftplet threw an exception on disconnect", e);
        }

        // make sure we close the data connection if it happens to be open
        try {
            ServerDataConnectionFactory dc = session.getDataConnection(); 
            if(dc != null) {
                dc.closeDataConnection();
            }
        } catch (Exception e) {
            // swallow the exception, we're closing down the session anyways
            LOG.warn("Data connection threw an exception on disconnect", e);
        }
        
        FileSystemView fs = session.getFileSystemView();
        if(fs != null) {
            try  {
                fs.dispose();
            } catch (Exception e) {
                LOG.warn("FileSystemView threw an exception on disposal", e);
            }
        }

        ServerFtpStatistics stats = ((ServerFtpStatistics) context
                .getFtpStatistics());

        if (stats != null) {
            stats.setLogout(session);
            stats.setCloseConnection(session);
            LOG.debug("Statistics login and connection count decreased due to session close");
        } else {
            LOG.warn("Statistics not available in session, can not decrease login and connection count");
        }
        LOG.debug("Session closed");
    }

    public void exceptionCaught(final FtpIoSession session,
            final Throwable cause) throws Exception {
        
        if(cause instanceof ProtocolDecoderException &&
                cause.getCause() instanceof MalformedInputException) {
            // client probably sent something which is not UTF-8 and we failed to
            // decode it
            
            LOG.warn(
                    "Client sent command that could not be decoded: {}",
                    ((ProtocolDecoderException)cause).getHexdump());
            session.write(new DefaultFtpReply(FtpReply.REPLY_501_SYNTAX_ERROR_IN_PARAMETERS_OR_ARGUMENTS, "Invalid character in command"));
        } else if (cause instanceof WriteToClosedSessionException) {
            WriteToClosedSessionException writeToClosedSessionException = 
                (WriteToClosedSessionException) cause;
            LOG.warn(
                            "Client closed connection before all replies could be sent, last reply was {}",
                            writeToClosedSessionException.getRequest());
            session.close(false).awaitUninterruptibly(10000);
        } else {
            LOG.error("Exception caught, closing session", cause);
            session.close(false).awaitUninterruptibly(10000);
        }


    }

    private boolean isCommandOkWithoutAuthentication(String command) {
        boolean okay = false;
        for (String allowed : NON_AUTHENTICATED_COMMANDS) {
            if (allowed.equals(command)) {
                okay = true;
                break;
            }
        }
        return okay;
    }

    public void messageReceived(final FtpIoSession session,
            final FtpRequest request) throws Exception {
        try {
            session.updateLastAccessTime();
            
            String commandName = request.getCommand();
            CommandFactory commandFactory = context.getCommandFactory();
            Command command = commandFactory.getCommand(commandName);

            // make sure the user is authenticated before he issues commands
            if (!session.isLoggedIn()
                    && !isCommandOkWithoutAuthentication(commandName)) {
                session.write(LocalizedFtpReply.translate(session, request,
                        context, FtpReply.REPLY_530_NOT_LOGGED_IN,
                        "permission", null));
                return;
            }

            FtpletContainer ftplets = context.getFtpletContainer();

            FtpletResult ftpletRet;
            try {
                ftpletRet = ftplets.beforeCommand(session.getFtpletSession(),
                        request);
            } catch (Exception e) {
                LOG.debug("Ftplet container threw exception", e);
                ftpletRet = FtpletResult.DISCONNECT;
            }
            if (ftpletRet == FtpletResult.DISCONNECT) {
                LOG.debug("Ftplet returned DISCONNECT, session will be closed");
                session.close(false).awaitUninterruptibly(10000);
                return;
            } else if (ftpletRet != FtpletResult.SKIP) {

                if (command != null) {
                    synchronized (session) {
                        command.execute(session, context, request);
                    }
                } else {
                    session.write(LocalizedFtpReply.translate(session, request,
                            context,
                            FtpReply.REPLY_502_COMMAND_NOT_IMPLEMENTED,
                            "not.implemented", null));
                }

                try {
                    ftpletRet = ftplets.afterCommand(
                            session.getFtpletSession(), request, session
                                    .getLastReply());
                } catch (Exception e) {
                    LOG.debug("Ftplet container threw exception", e);
                    ftpletRet = FtpletResult.DISCONNECT;
                }
                if (ftpletRet == FtpletResult.DISCONNECT) {
                    LOG.debug("Ftplet returned DISCONNECT, session will be closed");

                    session.close(false).awaitUninterruptibly(10000);
                    return;
                }
            }

        } catch (Exception ex) {

            // send error reply
            try {
                session.write(LocalizedFtpReply.translate(session, request,
                        context, FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                        null, null));
            } catch (Exception ex1) {
            }

            if (ex instanceof java.io.IOException) {
                throw (IOException) ex;
            } else {
                LOG.warn("RequestHandler.service()", ex);
            }
        }

    }

    public void sessionIdle(final FtpIoSession session, final IdleStatus status)
            throws Exception {
        LOG.info("Session idle, closing");
        session.close(false).awaitUninterruptibly(10000);
    }

    public void messageSent(final FtpIoSession session, final FtpReply reply)
            throws Exception {
        // do nothing

    }
}
