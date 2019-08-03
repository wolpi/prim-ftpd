/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 * 
 */
package org.apache.ftpserver.listener.nio;

import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.impl.DefaultFtpRequest;
import org.apache.ftpserver.impl.FtpHandler;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.logging.MdcInjectionFilter;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * Adapter between MINA handler and the {@link FtpHandler} interface
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 */
public class FtpHandlerAdapter extends IoHandlerAdapter {
    private final FtpServerContext context;

    private FtpHandler ftpHandler;

    public FtpHandlerAdapter(FtpServerContext context, FtpHandler ftpHandler) {
        this.context = context;
        this.ftpHandler = ftpHandler;
    }

    public void exceptionCaught(IoSession session, Throwable cause)
            throws Exception {
        FtpIoSession ftpSession = new FtpIoSession(session, context);
        ftpHandler.exceptionCaught(ftpSession, cause);
    }

    public void messageReceived(IoSession session, Object message)
            throws Exception {
        FtpIoSession ftpSession = new FtpIoSession(session, context);
        FtpRequest request = new DefaultFtpRequest(message.toString());

        ftpHandler.messageReceived(ftpSession, request);
    }

    public void messageSent(IoSession session, Object message) throws Exception {
        FtpIoSession ftpSession = new FtpIoSession(session, context);
        ftpHandler.messageSent(ftpSession, (FtpReply) message);
    }

    public void sessionClosed(IoSession session) throws Exception {
        FtpIoSession ftpSession = new FtpIoSession(session, context);
        ftpHandler.sessionClosed(ftpSession);
    }

    public void sessionCreated(IoSession session) throws Exception {
        FtpIoSession ftpSession = new FtpIoSession(session, context);
        MdcInjectionFilter.setProperty(session, "session", ftpSession.getSessionId().toString());

        ftpHandler.sessionCreated(ftpSession);

    }

    public void sessionIdle(IoSession session, IdleStatus status)
            throws Exception {
        FtpIoSession ftpSession = new FtpIoSession(session, context);
        ftpHandler.sessionIdle(ftpSession, status);
    }

    public void sessionOpened(IoSession session) throws Exception {
        FtpIoSession ftpSession = new FtpIoSession(session, context);
        ftpHandler.sessionOpened(ftpSession);
    }

    public FtpHandler getFtpHandler() {
        return ftpHandler;
    }

    public void setFtpHandler(FtpHandler handler) {
        this.ftpHandler = handler;

    }

}
