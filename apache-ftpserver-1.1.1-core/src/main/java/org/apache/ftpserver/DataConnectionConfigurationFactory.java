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

package org.apache.ftpserver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;

import org.apache.ftpserver.impl.DefaultDataConnectionConfiguration;
import org.apache.ftpserver.impl.PassivePorts;
import org.apache.ftpserver.ssl.SslConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data connection factory
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DataConnectionConfigurationFactory {

    private Logger log = LoggerFactory.getLogger(DataConnectionConfigurationFactory.class);
    
    // maximum idle time in seconds
    private int idleTime = 300;
    private SslConfiguration ssl;

    private boolean activeEnabled = true;
    private String activeLocalAddress;
    private int activeLocalPort = 0;
    private boolean activeIpCheck = false;
    
    private String passiveAddress;
    private String passiveExternalAddress;
    private PassivePorts passivePorts = new PassivePorts(Collections.<Integer>emptySet(), true);
    private boolean passiveIpCheck = false;
    private boolean implicitSsl;

    /**
     * Create a {@link DataConnectionConfiguration} instance based on the 
     * configuration on this factory
     * @return The {@link DataConnectionConfiguration} instance
     */
    public DataConnectionConfiguration createDataConnectionConfiguration() {
    	checkValidAddresses();
        return new DefaultDataConnectionConfiguration(idleTime,
                ssl, activeEnabled, activeIpCheck,
                activeLocalAddress, activeLocalPort,
                passiveAddress, passivePorts,
                passiveExternalAddress, passiveIpCheck, implicitSsl);
    }
    /*
     * (Non-Javadoc)
     *  Checks if the configured addresses to be used in further data connections 
     *  are valid.
     */
    private void checkValidAddresses(){
    	try{
    		InetAddress.getByName(passiveAddress);
    		InetAddress.getByName(passiveExternalAddress);
    	}catch(UnknownHostException ex){
    		throw new FtpServerConfigurationException("Unknown host", ex);
    	}
    }
    
    /**
     * Get the maximum idle time in seconds.
     * @return The maximum idle time
     */
    public int getIdleTime() {
        return idleTime;
    }

    /**
     * Set the maximum idle time in seconds.
     * @param idleTime The maximum idle time
     */
    
    public void setIdleTime(int idleTime) {
        this.idleTime = idleTime;
    }

    /**
     * Is PORT enabled?
     * @return true if active data connections are enabled
     */
    public boolean isActiveEnabled() {
        return activeEnabled;
    }

    /**
     * Set if active data connections are enabled
     * @param activeEnabled true if active data connections are enabled
     */
    public void setActiveEnabled(boolean activeEnabled) {
        this.activeEnabled = activeEnabled;
    }

    /**
     * Check the PORT IP?
     * @return true if the client IP is verified against the PORT IP
     */
    public boolean isActiveIpCheck() {
        return activeIpCheck;
    }

    /**
     * Check the PORT IP with the client IP?
     * @param activeIpCheck true if the PORT IP should be verified
     */
    public void setActiveIpCheck(boolean activeIpCheck) {
        this.activeIpCheck = activeIpCheck;
    }

    /**
     * Get the local address for active mode data transfer.
     * @return The address used for active data connections
     */
    public String getActiveLocalAddress() {
        return activeLocalAddress;
    }

    /**
     * Set the active data connection local host.
     * @param activeLocalAddress The address for active connections
     */
    public void setActiveLocalAddress(String activeLocalAddress) {
        this.activeLocalAddress = activeLocalAddress;
    }

    /**
     * Get the active local port number.
     * @return The port used for active data connections
     */
    public int getActiveLocalPort() {
        return activeLocalPort;
    }

    /**
     * Set the active data connection local port.
     * @param activeLocalPort The active data connection local port
     */
    public void setActiveLocalPort(int activeLocalPort) {
        this.activeLocalPort = activeLocalPort;
    }

    /**
     * Get passive host.
     * @return The address used for passive data connections
     */
    public String getPassiveAddress() {
        return passiveAddress;
    }

    /**
     * Set the passive server address. 
     * @param passiveAddress The address used for passive connections
     */
    public void setPassiveAddress(String passiveAddress) {
        this.passiveAddress = passiveAddress;
    }

    /**
     * Get the passive address that will be returned to clients on the PASV
     * command.
     * 
     * @return The passive address to be returned to clients, null if not
     *         configured.
     */
    public String getPassiveExternalAddress() {
        return passiveExternalAddress;
    }

    /**
     * Set the passive address that will be returned to clients on the PASV
     * command.
     * 
     * @param passiveExternalAddress The passive address to be returned to clients
     */
    public void setPassiveExternalAddress(String passiveExternalAddress) {
        this.passiveExternalAddress = passiveExternalAddress;
    }
    
    /**
	 * Tells whether or not IP address check is performed when accepting a
	 * passive data connection.
	 * 
	 * @return <code>true</code>, if the IP address checking is enabled;
	 *         <code>false</code>, otherwise. A value of <code>true</code> means
	 *         that site to site transfers are disabled. In other words, a
	 *         passive data connection MUST be made from the same IP address
	 *         that issued the PASV command.
	 */
	public boolean isPassiveIpCheck() {
		return passiveIpCheck;
	}
	
	/**
	 * Sets whether or not IP check is performed before accepting a passive data
	 * connection.
	 * 
	 * @param passiveIpCheck
	 *            whether or not IP check is performed before accepting a
	 *            passive data connection.
	 */
	public void setPassiveIpCheck(boolean passiveIpCheck) {
		this.passiveIpCheck = passiveIpCheck;
	}
    
    /**
     * Get passive data port. Data port number zero (0) means that any available
     * port will be used.
     * @return A passive port to use
     */
    public synchronized int requestPassivePort() {
        int dataPort = -1;
        int loopTimes = 2;
        Thread currThread = Thread.currentThread();

        while ((dataPort == -1) && (--loopTimes >= 0)
                && (!currThread.isInterrupted())) {

            // search for a free port
            dataPort = passivePorts.reserveNextPort();

            // no available free port - wait for the release notification
            if (dataPort == -1) {
                try {
                    log.info("We're waiting for a passive port, might be stuck");
                    wait();
                } catch (InterruptedException ex) {
                }
            }
        }
        return dataPort;
    }

    /**
     * Retrieve the passive ports configured for this data connection
     * 
     * @return The String of passive ports
     */
    public String getPassivePorts() {
        return passivePorts.toString();
    }

    /**
     * Set the passive ports to be used for data connections. Ports can be
     * defined as single ports, closed or open ranges. Multiple definitions can
     * be separated by commas, for example:
     * <ul>
     * <li>2300 : only use port 2300 as the passive port</li>
     * <li>2300-2399 : use all ports in the range</li>
     * <li>2300- : use all ports larger than 2300</li>
     * <li>2300, 2305, 2400- : use 2300 or 2305 or any port larger than 2400</li>
     * </ul>
     * 
     * Defaults to using any available port
     * 
     * @param passivePorts The passive ports string
     */
    public void setPassivePorts(String passivePorts) {
        this.passivePorts = new PassivePorts(passivePorts, true);
    }

    
    /**
     * Release data port
     * @param port The port to release
     */
    public synchronized void releasePassivePort(final int port) {
        passivePorts.releasePort(port);

        notify();
    }

    /**
     * Get the {@link SslConfiguration} to be used by data connections
     * @return The {@link SslConfiguration} used by data connections
     */
    public SslConfiguration getSslConfiguration() {
        return ssl;
    }

    /**
     * Set the {@link SslConfiguration} to be used by data connections
     * @param ssl The {@link SslConfiguration}
     */
    public void setSslConfiguration(SslConfiguration ssl) {
        this.ssl = ssl;
    }

    /**
     * @return True if ssl is mandatory for the data connection
     */
    public boolean isImplicitSsl() {
        return implicitSsl;
    }

    /**
     * Set whether ssl is required for the data connection
     * @param implicitSsl True if ssl is mandatory for the data connection
     */
    public void setImplicitSsl(boolean implicitSsl) {
        this.implicitSsl = implicitSsl;
    }
}
