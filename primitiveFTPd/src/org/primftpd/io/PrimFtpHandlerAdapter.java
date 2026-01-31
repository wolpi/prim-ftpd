package org.primftpd.io;

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
 * Copied from ftpserver 1.1.4;  changed to use {@link PrimFtpIoSession}.
 *
 * @see org.apache.ftpserver.listener.nio.FtpHandlerAdapter
 */
public class PrimFtpHandlerAdapter extends IoHandlerAdapter {

    private final FtpServerContext context;

    private FtpHandler ftpHandler;

    private FtpIoSession getOrCreateFtpSession(IoSession session) {
        FtpIoSession ftpSession = (FtpIoSession) session.getAttribute(FtpIoSession.class.getName());
        if (ftpSession == null) {
            ftpSession = new PrimFtpIoSession(session, context);
            session.setAttribute(FtpIoSession.class.getName(), ftpSession);
        }
        return ftpSession;
    }

    public PrimFtpHandlerAdapter(FtpServerContext context, FtpHandler ftpHandler) {
        this.context = context;
        this.ftpHandler = ftpHandler;
    }

    public void exceptionCaught(IoSession session, Throwable cause)
            throws Exception {
        FtpIoSession ftpSession = getOrCreateFtpSession(session);
        ftpHandler.exceptionCaught(ftpSession, cause);
    }

    public void messageReceived(IoSession session, Object message)
            throws Exception {
        FtpIoSession ftpSession = getOrCreateFtpSession(session);
        FtpRequest request = new DefaultFtpRequest(message.toString());

        ftpHandler.messageReceived(ftpSession, request);
    }

    public void messageSent(IoSession session, Object message) throws Exception {
        FtpIoSession ftpSession = getOrCreateFtpSession(session);
        ftpHandler.messageSent(ftpSession, (FtpReply) message);
    }

    public void sessionClosed(IoSession session) throws Exception {
        FtpIoSession ftpSession = getOrCreateFtpSession(session);
        ftpHandler.sessionClosed(ftpSession);
    }

    public void sessionCreated(IoSession session) throws Exception {
        FtpIoSession ftpSession = getOrCreateFtpSession(session);
        MdcInjectionFilter.setProperty(session, "session", ftpSession.getSessionId().toString());

        ftpHandler.sessionCreated(ftpSession);

    }

    public void sessionIdle(IoSession session, IdleStatus status)
            throws Exception {
        FtpIoSession ftpSession = getOrCreateFtpSession(session);
        ftpHandler.sessionIdle(ftpSession, status);
    }

    public void sessionOpened(IoSession session) throws Exception {
        FtpIoSession ftpSession = getOrCreateFtpSession(session);
        ftpHandler.sessionOpened(ftpSession);
    }

    public FtpHandler getFtpHandler() {
        return ftpHandler;
    }

    public void setFtpHandler(FtpHandler handler) {
        this.ftpHandler = handler;

    }
}
