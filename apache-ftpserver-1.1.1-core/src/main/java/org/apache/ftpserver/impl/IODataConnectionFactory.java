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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.ftpserver.DataConnectionConfiguration;
import org.apache.ftpserver.DataConnectionException;
import org.apache.ftpserver.ftplet.DataConnection;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ssl.ClientAuth;
import org.apache.ftpserver.ssl.SslConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * We can get the FTP data connection using this class. It uses either PORT or
 * PASV command.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class IODataConnectionFactory implements ServerDataConnectionFactory {

    private final Logger LOG = LoggerFactory
            .getLogger(IODataConnectionFactory.class);

    private FtpServerContext serverContext;

    private Socket dataSoc;

    ServerSocket servSoc;

    InetAddress address;

    int port = 0;

    long requestTime = 0L;

    boolean passive = false;

    boolean secure = false;

    private boolean isZip = false;

    InetAddress serverControlAddress;

    FtpIoSession session;

    public IODataConnectionFactory(final FtpServerContext serverContext,
            final FtpIoSession session) {
        this.session = session;
        this.serverContext = serverContext;
        if ((session != null) && (session.getListener() != null) && 
            session.getListener().getDataConnectionConfiguration().isImplicitSsl()) {
            secure = true;
        }
    }

    /**
     * Close data socket.
     * This method must be idempotent as we might call it multiple times during disconnect.
     */
    public synchronized void closeDataConnection() {

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
        if (servSoc != null) {
            try {
                servSoc.close();
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

            servSoc = null;
        }

        // reset request time
        requestTime = 0L;
    }

    /**
     * Port command.
     */
    public synchronized void initActiveDataConnection(
            final InetSocketAddress address) {

        // close old sockets if any
        closeDataConnection();

        // set variables
        passive = false;
        this.address = address.getAddress();
        port = address.getPort();
        requestTime = System.currentTimeMillis();
    }

    private SslConfiguration getSslConfiguration() {
        DataConnectionConfiguration dataCfg = session.getListener()
                .getDataConnectionConfiguration();

        SslConfiguration configuration = dataCfg.getSslConfiguration();

        // fall back if no configuration has been provided on the data connection config
        if (configuration == null) {
            configuration = session.getListener().getSslConfiguration();
        }

        return configuration;
    }

    /**
     * Initiate a data connection in passive mode (server listening). 
     */
    public synchronized InetSocketAddress initPassiveDataConnection()
            throws DataConnectionException {
        LOG.debug("Initiating passive data connection");
        // close old sockets if any
        closeDataConnection();

        // get the passive port
        int passivePort = session.getListener()
                .getDataConnectionConfiguration().requestPassivePort();
        if (passivePort == -1) {
            servSoc = null;
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

            if (secure) {
                LOG
                        .debug(
                                "Opening SSL passive data connection on address \"{}\" and port {}",
                                address, passivePort);
                SslConfiguration ssl = getSslConfiguration();
                if (ssl == null) {
                    throw new DataConnectionException(
                            "Data connection SSL required but not configured.");
                }

                // this method does not actually create the SSL socket, due to a JVM bug 
                // (https://issues.apache.org/jira/browse/FTPSERVER-241).
                // Instead, it creates a regular
                // ServerSocket that will be wrapped as a SSL socket in createDataSocket()
                servSoc = new ServerSocket(passivePort, 0, address);
                LOG
                        .debug(
                                "SSL Passive data connection created on address \"{}\" and port {}",
                                address, passivePort);
            } else {
                LOG
                        .debug(
                                "Opening passive data connection on address \"{}\" and port {}",
                                address, passivePort);
                servSoc = new ServerSocket(passivePort, 0, address);
                LOG
                        .debug(
                                "Passive data connection created on address \"{}\" and port {}",
                                address, passivePort);
            }
            port = servSoc.getLocalPort();
            servSoc.setSoTimeout(dataCfg.getIdleTime() * 1000);

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

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.ftpserver.FtpDataConnectionFactory2#getInetAddress()
     */
    public InetAddress getInetAddress() {
        return address;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.ftpserver.FtpDataConnectionFactory2#getPort()
     */
    public int getPort() {
        return port;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.ftpserver.FtpDataConnectionFactory2#openConnection()
     */
    public DataConnection openConnection() throws Exception {
        return new IODataConnection(createDataSocket(), session, this);
    }

    /**
     * Get the data socket. In case of error returns null.
     */
    private synchronized Socket createDataSocket() throws Exception {

        // get socket depending on the selection
        dataSoc = null;
        DataConnectionConfiguration dataConfig = session.getListener()
                .getDataConnectionConfiguration();
        try {
            if (!passive) {
                if (secure) {
                    LOG.debug("Opening secure active data connection");
                    SslConfiguration ssl = getSslConfiguration();
                    if (ssl == null) {
                        throw new FtpException(
                                "Data connection SSL not configured");
                    }

                    // get socket factory
                    SSLSocketFactory socFactory = ssl.getSocketFactory();

                    // create socket
                    SSLSocket ssoc = (SSLSocket) socFactory.createSocket();
                    ssoc.setUseClientMode(false);

                    // initialize socket
                    if (ssl.getEnabledCipherSuites() != null) {
                        ssoc.setEnabledCipherSuites(ssl.getEnabledCipherSuites());
                    }
                    dataSoc = ssoc;
                } else {
                    LOG.debug("Opening active data connection");
                    dataSoc = new Socket();
                }

                dataSoc.setReuseAddress(true);

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

                if (secure) {
                    LOG.debug("Opening secure passive data connection");
                    // this is where we wrap the unsecured socket as a SSLSocket. This is 
                    // due to the JVM bug described in FTPSERVER-241.

                    // get server socket factory
                    SslConfiguration ssl = getSslConfiguration();
                    
                    // we've already checked this, but let's do it again
                    if (ssl == null) {
                        throw new FtpException(
                                "Data connection SSL not configured");
                    }

                    SSLSocketFactory ssocketFactory = ssl.getSocketFactory();

                    Socket serverSocket = servSoc.accept();

                    SSLSocket sslSocket = (SSLSocket) ssocketFactory
                            .createSocket(serverSocket, serverSocket
                                    .getInetAddress().getHostAddress(),
                                    serverSocket.getPort(), true);
                    sslSocket.setUseClientMode(false);

                    // initialize server socket
                    if (ssl.getClientAuth() == ClientAuth.NEED) {
                        sslSocket.setNeedClientAuth(true);
                    } else if (ssl.getClientAuth() == ClientAuth.WANT) {
                        sslSocket.setWantClientAuth(true);
                    }

                    if (ssl.getEnabledCipherSuites() != null) {
                        sslSocket.setEnabledCipherSuites(ssl
                                .getEnabledCipherSuites());
                    }

                    dataSoc = sslSocket;
                } else {
                    LOG.debug("Opening passive data connection");

                    dataSoc = servSoc.accept();
                }
                
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

        // Make sure we initiate the SSL handshake, or we'll
        // get an error if we turn out not to send any data
        // e.g. during the listing of an empty directory
        if (dataSoc instanceof SSLSocket) {
            ((SSLSocket) dataSoc).startHandshake();
        }

        return dataSoc;
    }

    /*
     *  (non-Javadoc)
     *   Returns an InetAddress object from a hostname or IP address.
     */
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

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.ftpserver.DataConnectionFactory#isSecure()
     */
    public boolean isSecure() {
        return secure;
    }

    /**
     * Set the security protocol.
     */
    public void setSecure(final boolean secure) {
        this.secure = secure;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.ftpserver.DataConnectionFactory#isZipMode()
     */
    public boolean isZipMode() {
        return isZip;
    }

    /**
     * Set zip mode.
     */
    public void setZipMode(final boolean zip) {
        isZip = zip;
    }

    /**
     * Check the data connection idle status.
     */
    public synchronized boolean isTimeout(final long currTime) {

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

    /**
     * Dispose data connection - close all the sockets.
     */
    public void dispose() {
        closeDataConnection();
    }

    /**
     * Sets the server's control address.
     */
    public void setServerControlAddress(final InetAddress serverControlAddress) {
        this.serverControlAddress = serverControlAddress;
    }
}
