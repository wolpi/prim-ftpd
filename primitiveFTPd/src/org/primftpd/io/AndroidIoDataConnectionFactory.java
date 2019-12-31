package org.primftpd.io;

import org.apache.ftpserver.DataConnectionConfiguration;
import org.apache.ftpserver.DataConnectionException;
import org.apache.ftpserver.ftplet.DataConnection;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;
import org.apache.ftpserver.impl.IODataConnectionFactory;
import org.apache.ftpserver.impl.ServerDataConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;


public class AndroidIoDataConnectionFactory extends IODataConnectionFactory implements ServerDataConnectionFactory {

    private final Logger LOG = LoggerFactory
            .getLogger(IODataConnectionFactory.class);

    private FtpServerContext serverContext;

    private InetAddress address;

    private int port = 0;

    private long requestTime = 0L;

    private boolean passive = false;

    private boolean secure = false;

    private boolean isZip = false;

    private InetAddress serverControlAddress;

    private FtpIoSession session;

    private ServerSocketChannel serverSocketChannel;
    private SocketChannel dataSocketChannel;
    private ServerSocket serverSocket;
    private Socket dataSoc;

    public AndroidIoDataConnectionFactory(
            final FtpServerContext serverContext,
            final FtpIoSession session) {
        super(serverContext, session);
        this.session = session;
        this.serverContext = serverContext;
    }

    @Override
    public void initActiveDataConnection(InetSocketAddress address) {

        // close old sockets if any
        closeDataConnection();

        // set variables
        passive = false;
        this.address = address.getAddress();
        port = address.getPort();
        requestTime = System.currentTimeMillis();
    }

    @Override
    public InetSocketAddress initPassiveDataConnection() throws DataConnectionException {
        LOG.debug("Initiating passive data connection");
        // close old sockets if any
        closeDataConnection();

        // get the passive port
        int passivePort = session.getListener()
                .getDataConnectionConfiguration().requestPassivePort();
        if (passivePort == -1) {
            //servSoc = null;
            serverSocketChannel = null;
            throw new DataConnectionException(
                    "Cannot find an available passive port.");
        }

        // open passive server socket and get parameters
        try {
            DataConnectionConfiguration dataCfg = session.getListener()
                    .getDataConnectionConfiguration();

            String passiveAddress = dataCfg.getPassiveAddress();

            if (passiveAddress == null) {
                address = serverControlAddress;
            } else {
                address = resolveAddress(dataCfg.getPassiveAddress());
            }

            LOG
                    .debug(
                            "Opening passive data connection on address \"{}\" and port {}",
                            address, passivePort);
            //servSoc = new ServerSocket(passivePort, 0, address);
            serverSocketChannel = ServerSocketChannel.open();
            serverSocket = serverSocketChannel.socket();
            SocketAddress socketAddress = new InetSocketAddress(address, passivePort);
            serverSocket.bind(socketAddress);
            LOG
                    .debug(
                            "Passive data connection created on address \"{}\" and port {}",
                            address, passivePort);
            //port = serverSocketChannel.getLocalPort();
            //servSoc.setSoTimeout(dataCfg.getIdleTime() * 1000);
            //port = serverSocket.getLocalPort();
            port = passivePort;
            serverSocket.setSoTimeout(dataCfg.getIdleTime() * 1000);


            // set different state variables
            passive = true;
            requestTime = System.currentTimeMillis();

            return new InetSocketAddress(address, port);
        } catch (Exception ex) {
            closeDataConnection();
            throw new DataConnectionException(
                    "Failed to initate passive data connection: "
                            + ex.getMessage(), ex);
        }
    }

    private InetAddress resolveAddress(String host)
            throws DataConnectionException {
        if (host == null) {
            return null;
        } else {
            try {
                return InetAddress.getByName(host);
            } catch (UnknownHostException ex) {
                throw new DataConnectionException("Failed to resolve address", ex);
            }
        }
    }

    @Override
    public boolean isTimeout(long currTime) {
        // data connection not requested - not a timeout
        if (requestTime == 0L) {
            return false;
        }

        // data connection active - not a timeout
        if (dataSoc != null) {
            return false;
        }

        // no idle time limit - not a timeout
        int maxIdleTime = session.getListener()
                .getDataConnectionConfiguration().getIdleTime() * 1000;
        if (maxIdleTime == 0) {
            return false;
        }

        // idle time is within limit - not a timeout
        if ((currTime - requestTime) < maxIdleTime) {
            return false;
        }

        return true;
    }

    @Override
    public void dispose() {
        closeDataConnection();
    }

    @Override
    public DataConnection openConnection() throws Exception {
        return new AndroidIoDataConnection(createDataSocket(), session, this);
    }

    private synchronized SocketChannel createDataSocket() throws Exception {

        // get socket depending on the selection
        dataSoc = null;
        DataConnectionConfiguration dataConfig = session.getListener()
                .getDataConnectionConfiguration();
        try {
            if (!passive) {
                LOG.debug("Opening active data connection");
                //dataSoc = new Socket();
                dataSocketChannel = SocketChannel.open();
                dataSoc = dataSocketChannel.socket();

                dataSoc.setReuseAddress(true);

                // note: this will likely not work
                InetAddress localAddr = resolveAddress(dataConfig
                        .getActiveLocalAddress());

                // if no local address has been configured, make sure we use the same as the client connects from
                if(localAddr == null) {
                    localAddr = ((InetSocketAddress)session.getLocalAddress()).getAddress();
                }

                SocketAddress localSocketAddress = new InetSocketAddress(localAddr, dataConfig.getActiveLocalPort());

                LOG.debug("Binding active data connection to {}", localSocketAddress);
                dataSoc.bind(localSocketAddress);

                dataSoc.connect(new InetSocketAddress(address, port));
            } else {


                LOG.debug("Opening passive data connection");

                //dataSoc = servSoc.accept();
                dataSocketChannel = serverSocketChannel.accept();
                dataSoc = dataSocketChannel.socket();

                if (dataConfig.isPassiveIpCheck()) {
                    // Let's make sure we got the connection from the same
                    // client that we are expecting
                    InetAddress remoteAddress = ((InetSocketAddress) session.getRemoteAddress()).getAddress();
                    InetAddress dataSocketAddress = dataSoc.getInetAddress();
                    if (!dataSocketAddress.equals(remoteAddress)) {
                        LOG.warn("Passive IP Check failed. Closing data connection from "
                                + dataSocketAddress
                                + " as it does not match the expected address "
                                + remoteAddress);
                        closeDataConnection();
                        return null;
                    }
                }

                DataConnectionConfiguration dataCfg = session.getListener()
                        .getDataConnectionConfiguration();

                dataSoc.setSoTimeout(dataCfg.getIdleTime() * 1000);
                LOG.debug("Passive data connection opened");
            }
        } catch (Exception ex) {
            closeDataConnection();
            LOG.warn("FtpDataConnection.getDataSocket()", ex);
            throw ex;
        }
        dataSoc.setSoTimeout(dataConfig.getIdleTime() * 1000);

        return dataSocketChannel;
    }

    @Override
    public void closeDataConnection() {
        // close client socket if any
        if (dataSoc != null) {
            try {
                dataSoc.close();
            } catch (Exception ex) {
                LOG.warn("FtpDataConnection.closeDataSocket()", ex);
            }
            dataSoc = null;
        }

        // close server socket if any
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (Exception ex) {
                LOG.warn("FtpDataConnection.closeDataSocket()", ex);
            }

            if (session != null) {
                DataConnectionConfiguration dcc = session.getListener()
                        .getDataConnectionConfiguration();
                if (dcc != null) {
                    dcc.releasePassivePort(port);
                }
            }

            serverSocket = null;
        }

        // reset request time
        requestTime = 0L;

        if (serverSocketChannel != null) {
            try {
                serverSocketChannel.close();
            } catch (Exception ex) {
                LOG.warn("close serverSocketChannel", ex);
            }
            serverSocketChannel = null;
        }
        if (dataSocketChannel != null) {
            try {
                dataSocketChannel.close();
            } catch (Exception ex) {
                LOG.warn("close dataSocketChannel", ex);
            }
            dataSocketChannel = null;
        }
    }

    @Override
    public void setSecure(boolean secure) {
        // TLS not supported
        //this.secure = secure;
    }

    @Override
    public void setServerControlAddress(InetAddress serverControlAddress) {
        this.serverControlAddress = serverControlAddress;
    }

    @Override
    public void setZipMode(boolean zip) {
        this.isZip = zip;
    }

    @Override
    public boolean isZipMode() {
        return isZip;
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public InetAddress getInetAddress() {
        return serverControlAddress;
    }

    @Override
    public int getPort() {
        return port;
    }

}
