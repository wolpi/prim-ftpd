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

package org.apache.ftpserver.listener.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ftpserver.DataConnectionConfiguration;
import org.apache.ftpserver.FtpServerConfigurationException;
import org.apache.ftpserver.impl.DefaultFtpHandler;
import org.apache.ftpserver.impl.FtpHandler;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;
import org.apache.ftpserver.ipfilter.MinaSessionFilter;
import org.apache.ftpserver.ipfilter.SessionFilter;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.ClientAuth;
import org.apache.ftpserver.ssl.SslConfiguration;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.firewall.Subnet;
import org.apache.mina.filter.logging.MdcInjectionFilter;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * The default {@link Listener} implementation.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NioListener extends AbstractListener {

    private final Logger LOG = LoggerFactory.getLogger(NioListener.class);

    private SocketAcceptor acceptor;

    private InetSocketAddress address;

    boolean suspended = false;

    private FtpHandler handler = new DefaultFtpHandler();

    private FtpServerContext context;

    /**
     * @deprecated Use the constructor with IpFilter instead. 
     * Constructor for internal use, do not use directly. Instead use {@link ListenerFactory}
     */
    @Deprecated
    public NioListener(String serverAddress, int port,
            boolean implicitSsl,
            SslConfiguration sslConfiguration,
            DataConnectionConfiguration dataConnectionConfig, 
            int idleTimeout, List<InetAddress> blockedAddresses, List<Subnet> blockedSubnets) {
        super(serverAddress, port, implicitSsl, sslConfiguration, dataConnectionConfig, 
                idleTimeout, blockedAddresses, blockedSubnets);   
    }

    /**
     * Constructor for internal use, do not use directly. Instead use {@link ListenerFactory}
     */
    public NioListener(String serverAddress, int port, boolean implicitSsl,
            SslConfiguration sslConfiguration,
            DataConnectionConfiguration dataConnectionConfig, int idleTimeout,
            SessionFilter sessionFilter) {
        super(serverAddress, port, implicitSsl, sslConfiguration,
                dataConnectionConfig, idleTimeout, sessionFilter);
    }

    /**
     * @see Listener#start(FtpServerContext)
     */
    public synchronized void start(FtpServerContext context) {
        if(!isStopped()) {
            // listener already started, don't allow
            throw new IllegalStateException("Listener already started");
        }
        
        try {
            
            this.context = context;
    
            acceptor = new NioSocketAcceptor(Runtime.getRuntime()
                    .availableProcessors());
    
            if (getServerAddress() != null) {
                address = new InetSocketAddress(getServerAddress(), getPort());
            } else {
                address = new InetSocketAddress(getPort());
            }
    
            acceptor.setReuseAddress(true);
            acceptor.getSessionConfig().setReadBufferSize(2048);
            acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE,
                    getIdleTimeout());
            // Decrease the default receiver buffer size
            acceptor.getSessionConfig().setReceiveBufferSize(512);
    
            MdcInjectionFilter mdcFilter = new MdcInjectionFilter();
    
            acceptor.getFilterChain().addLast("mdcFilter", mdcFilter);

            SessionFilter sessionFilter = getSessionFilter();
            if (sessionFilter != null) {
                // add and IP filter to the filter chain.
                acceptor.getFilterChain().addLast("sessionFilter",
                        new MinaSessionFilter(sessionFilter));
            }
    
            acceptor.getFilterChain().addLast("threadPool",
                    new ExecutorFilter(context.getThreadPoolExecutor()));
            acceptor.getFilterChain().addLast("codec",
                    new ProtocolCodecFilter(new FtpServerProtocolCodecFactory()));
            acceptor.getFilterChain().addLast("mdcFilter2", mdcFilter);
            acceptor.getFilterChain().addLast("logger", new FtpLoggingFilter());
    
            if (isImplicitSsl()) {
                SslConfiguration ssl = getSslConfiguration();
                SslFilter sslFilter;
                try {
                    sslFilter = new SslFilter(ssl.getSSLContext());
                } catch (GeneralSecurityException e) {
                    throw new FtpServerConfigurationException("SSL could not be initialized, check configuration");
                }
    
                if (ssl.getClientAuth() == ClientAuth.NEED) {
                    sslFilter.setNeedClientAuth(true);
                } else if (ssl.getClientAuth() == ClientAuth.WANT) {
                    sslFilter.setWantClientAuth(true);
                }
    
                if (ssl.getEnabledCipherSuites() != null) {
                    sslFilter.setEnabledCipherSuites(ssl.getEnabledCipherSuites());
                }
    
                acceptor.getFilterChain().addFirst("sslFilter", sslFilter);
            }
    
            handler.init(context, this);
            acceptor.setHandler(new FtpHandlerAdapter(context, handler));
    
            try {
                acceptor.bind(address);
            } catch (IOException e) {
                throw new FtpServerConfigurationException("Failed to bind to address " + address + ", check configuration", e);
            }
            
            updatePort();
    
        } catch(RuntimeException e) {
            // clean up if we fail to start
            stop();
            
            throw e;
        }
    }
    
    private void updatePort() {
        // update the port to the real port bound by the listener
        setPort(acceptor.getLocalAddress().getPort());
    }

    /**
     * @see Listener#stop()
     */
    public synchronized void stop() {
        // close server socket
        if (acceptor != null) {
            acceptor.unbind();
            acceptor.dispose();
            acceptor = null;
        }
        context = null;
    }

    /**
     * @see Listener#isStopped()
     */
    public boolean isStopped() {
        return acceptor == null;
    }

    /**
     * @see Listener#isSuspended()
     */
    public boolean isSuspended() {
        return suspended;

    }

    /**
     * @see Listener#resume()
     */
    public synchronized void resume() {
        if (acceptor != null && suspended) {
            try {
                LOG.debug("Resuming listener");
                acceptor.bind(address);
                LOG.debug("Listener resumed");
                
                updatePort();
                
                suspended = false;
            } catch (IOException e) {
                LOG.error("Failed to resume listener", e);
            }
        }
    }

    /**
     * @see Listener#suspend()
     */
    public synchronized void suspend() {
        if (acceptor != null && !suspended) {
            LOG.debug("Suspending listener");
            acceptor.unbind();
            
            suspended = true;
            LOG.debug("Listener suspended");
        }
    }
    
    /**
     * @see Listener#getActiveSessions()
     */
    public synchronized Set<FtpIoSession> getActiveSessions() {
        Map<Long, IoSession> sessions = acceptor.getManagedSessions();

        Set<FtpIoSession> ftpSessions = new HashSet<FtpIoSession>();
        for (IoSession session : sessions.values()) {
            ftpSessions.add(new FtpIoSession(session, context));
        }
        return ftpSessions;
    }
}
