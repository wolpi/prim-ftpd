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

import java.net.InetAddress;
import java.util.List;

import org.apache.ftpserver.DataConnectionConfiguration;
import org.apache.ftpserver.ipfilter.IpFilterType;
import org.apache.ftpserver.ipfilter.RemoteIpFilter;
import org.apache.ftpserver.ipfilter.SessionFilter;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfiguration;
import org.apache.mina.filter.firewall.Subnet;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * Common base class for listener implementations
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractListener implements Listener {

    private final String serverAddress;

    private int port = 21;

    private final SslConfiguration ssl;

    private final boolean implicitSsl;
    
    private final int idleTimeout;
    
    private final List<InetAddress> blockedAddresses;

    private final List<Subnet> blockedSubnets;

    private final SessionFilter sessionFilter;

    private final DataConnectionConfiguration dataConnectionConfig;

    /**
     * @deprecated Use the constructor with IpFilter instead. 
     * Constructor for internal use, do not use directly. Instead use {@link ListenerFactory}
     */
    @Deprecated
    public AbstractListener(String serverAddress, int port, boolean implicitSsl, 
            SslConfiguration sslConfiguration, DataConnectionConfiguration dataConnectionConfig,
            int idleTimeout, List<InetAddress> blockedAddresses, List<Subnet> blockedSubnets) {
        this.serverAddress = serverAddress;
        this.port = port;
        this.implicitSsl = implicitSsl;
        this.dataConnectionConfig = dataConnectionConfig;
        this.ssl = sslConfiguration;
        this.idleTimeout = idleTimeout;
        this.sessionFilter = createBlackListFilter(blockedAddresses, blockedSubnets);
    	this.blockedAddresses = blockedAddresses;
    	this.blockedSubnets = blockedSubnets;
    }
    
    /**
     * Constructor for internal use, do not use directly. Instead use {@link ListenerFactory}
     */
    public AbstractListener(String serverAddress, int port,
            boolean implicitSsl, SslConfiguration sslConfiguration,
            DataConnectionConfiguration dataConnectionConfig, int idleTimeout,
            SessionFilter sessionFilter) {
        this.serverAddress = serverAddress;
        this.port = port;
        this.implicitSsl = implicitSsl;
        this.dataConnectionConfig = dataConnectionConfig;
        this.ssl = sslConfiguration;
        this.idleTimeout = idleTimeout;
        this.sessionFilter = sessionFilter;
        this.blockedAddresses = null;
        this.blockedSubnets = null;
    }
    
    /**
     * Creates a SessionFilter that blacklists the given IP addresses and/or
     * Subnets.
     * 
     * @param blockedAddresses
     *            the addresses to block
     * @param blockedSubnets
     *            the subnets to block
     * @return a SessionFilter that blacklists the given IP addresses and/or
     *         Subnets.
     */
    private static SessionFilter createBlackListFilter(
            List<InetAddress> blockedAddresses, List<Subnet> blockedSubnets) {
        if (blockedAddresses == null && blockedSubnets == null) {
            return null;
        }
        // Initialize the IP filter with Deny type
        RemoteIpFilter ipFilter = new RemoteIpFilter(IpFilterType.DENY);
        if (blockedSubnets != null) {
            ipFilter.addAll(blockedSubnets);
        }
        if (blockedAddresses != null) {
            for (InetAddress address : blockedAddresses) {
                ipFilter.add(new Subnet(address, 32));
            }
        }
        return ipFilter;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isImplicitSsl() {
        return implicitSsl;
    }

    /**
     * {@inheritDoc}
     */
    public int getPort() {
        return port;
    }

    /**
     * Used internally to update the port after binding
     * @param port
     */
    protected void setPort(int port) {
        this.port = port;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getServerAddress() {
        return serverAddress;
    }

    /**
     * {@inheritDoc}
     */
    public SslConfiguration getSslConfiguration() {
        return ssl;
    }

    /**
     * {@inheritDoc}
     */
    public DataConnectionConfiguration getDataConnectionConfiguration() {
        return dataConnectionConfig;
    }

    /**
     * Get the number of seconds during which no network activity 
     * is allowed before a session is closed due to inactivity.  
     * @return The idle time out
     */
    public int getIdleTimeout() {
        return idleTimeout;
    }

    /**
     * Retrives the {@link InetAddress} for which this listener blocks
     * connections
     * 
     * @return The list of {@link InetAddress}es
     */
    public List<InetAddress> getBlockedAddresses() {
        return blockedAddresses;
    }

    /**
     * Retrieves the {@link Subnet}s for this listener blocks connections
     * 
     * @return The list of {@link Subnet}s
     */
    public List<Subnet> getBlockedSubnets() {
        return blockedSubnets;
    }

    public SessionFilter getSessionFilter() {
        return sessionFilter;
    }
}
