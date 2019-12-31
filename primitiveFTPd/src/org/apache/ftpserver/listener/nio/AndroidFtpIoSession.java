package org.apache.ftpserver.listener.nio;

import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;
import org.apache.ftpserver.impl.IODataConnectionFactory;
import org.apache.ftpserver.impl.ServerDataConnectionFactory;
import org.apache.mina.core.session.IoSession;
import org.primftpd.io.AndroidIoDataConnectionFactory;

import java.net.InetSocketAddress;

public class AndroidFtpIoSession extends FtpIoSession {

    private static final String ATTRIBUTE_DATA_CONNECTION = ATTRIBUTE_PREFIX
            + "data-connection";

    private final FtpServerContext context;

    public AndroidFtpIoSession(IoSession wrappedSession, FtpServerContext context) {
        super(wrappedSession, context);
        this.context = context;
   }

   public synchronized ServerDataConnectionFactory getDataConnection() {
        if (containsAttribute(ATTRIBUTE_DATA_CONNECTION)) {
            return (ServerDataConnectionFactory) getAttribute(ATTRIBUTE_DATA_CONNECTION);
        } else {
            IODataConnectionFactory dataCon = new AndroidIoDataConnectionFactory(
                    context, this);
            dataCon.setServerControlAddress(((InetSocketAddress) getLocalAddress()).getAddress());
            setAttribute(ATTRIBUTE_DATA_CONNECTION, dataCon);

            return dataCon;
        }
    }
}
