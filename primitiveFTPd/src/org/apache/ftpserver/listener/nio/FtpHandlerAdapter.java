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

public class FtpHandlerAdapter extends IoHandlerAdapter {

    private final FtpServerContext context;

    private FtpHandler ftpHandler;

    private FtpIoSession createFtpIoSession(IoSession session, FtpServerContext context) {
        return new AndroidFtpIoSession(session, context);
    }

    public FtpHandlerAdapter(FtpServerContext context, FtpHandler ftpHandler) {
        this.context = context;
        this.ftpHandler = ftpHandler;
    }

    public void exceptionCaught(IoSession session, Throwable cause)
            throws Exception {
        FtpIoSession ftpSession = createFtpIoSession(session, context);
        ftpHandler.exceptionCaught(ftpSession, cause);
    }

    public void messageReceived(IoSession session, Object message)
            throws Exception {
        FtpIoSession ftpSession = createFtpIoSession(session, context);
        FtpRequest request = new DefaultFtpRequest(message.toString());

        ftpHandler.messageReceived(ftpSession, request);
    }

    public void messageSent(IoSession session, Object message) throws Exception {
        FtpIoSession ftpSession = createFtpIoSession(session, context);
        ftpHandler.messageSent(ftpSession, (FtpReply) message);
    }

    public void sessionClosed(IoSession session) throws Exception {
        FtpIoSession ftpSession = createFtpIoSession(session, context);
        ftpHandler.sessionClosed(ftpSession);
    }

    public void sessionCreated(IoSession session) throws Exception {
        FtpIoSession ftpSession = createFtpIoSession(session, context);
        MdcInjectionFilter.setProperty(session, "session", ftpSession.getSessionId().toString());

        ftpHandler.sessionCreated(ftpSession);

    }

    public void sessionIdle(IoSession session, IdleStatus status)
            throws Exception {
        FtpIoSession ftpSession = createFtpIoSession(session, context);
        ftpHandler.sessionIdle(ftpSession, status);
    }

    public void sessionOpened(IoSession session) throws Exception {
        FtpIoSession ftpSession = createFtpIoSession(session, context);
        ftpHandler.sessionOpened(ftpSession);
    }

    public FtpHandler getFtpHandler() {
        return ftpHandler;
    }

    public void setFtpHandler(FtpHandler handler) {
        this.ftpHandler = handler;

    }
}
