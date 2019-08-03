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

import org.apache.ftpserver.ssl.SslConfiguration;

/**
 * Data connection configuration interface.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface DataConnectionConfiguration {

    /**
     * Get the maximum idle time in seconds.
     * @return The maximum idle time
     */
    int getIdleTime();

    /**
     * Is active data connection enabled?
     * @return true if active data connections are enabled
     */
    boolean isActiveEnabled();

    /**
     * Check the PORT IP with the client IP?
     * @return true if the PORT IP is verified
     */
    boolean isActiveIpCheck();

    /**
     * Get the active data connection local host.
     * @return The {@link InetAddress} for active connections
     */
    String getActiveLocalAddress();

    /**
     * Get the active data connection local port.
     * @return The active data connection local port
     */
    int getActiveLocalPort();

    /**
     * Get passive server address. null, if not set in the configuration.
     * @return The {@link InetAddress} used for passive connections
     */
    String getPassiveAddress();

    /**
     * Get the passive address that will be returned to clients on the PASV
     * command.
     * 
     * @return The passive address to be returned to clients, null if not
     *         configured.
     */
    String getPassiveExernalAddress();

    /**
     * Get the passive ports to be used for data connections. Ports can be
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
     * @return The passive ports string
     */
    String getPassivePorts();
    
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
	boolean isPassiveIpCheck();

    /**
     * Request a passive port. Will block until a port is available
     * @return A free passive part
     */
    int requestPassivePort();

    /**
     * Release passive port.
     * @param port The port to be released
     */
    void releasePassivePort(int port);

    /**
     * Get SSL configuration for this data connection.
     * @return The {@link SslConfiguration}
     */
    SslConfiguration getSslConfiguration();
    
    /**
     * @return True if SSL is mandatory for the data channel
     */
    boolean isImplicitSsl();
}
