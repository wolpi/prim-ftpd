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

package org.apache.ftpserver.ipfilter;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.firewall.Subnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the <code>SessionFilter</code> interface, to filter
 * sessions based on the remote IP address.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */

public class RemoteIpFilter extends CopyOnWriteArraySet<Subnet> implements
        SessionFilter {

    /**
     * Logger
     */
    Logger LOGGER = LoggerFactory.getLogger(RemoteIpFilter.class);

    /**
     * Serial version UID
     */
    private static final long serialVersionUID = 4887092372700628783L;

    /**
     * filter type
     */
    private IpFilterType type = null;

    /**
     * Creates a new instance of <code>RemoteIpFilter</code>.
     * 
     * @param type
     *            the filter type
     */
    public RemoteIpFilter(IpFilterType type) {
        this(type, new HashSet<Subnet>(0));
    }

    /**
     * Creates a new instance of <code>RemoteIpFilter</code>.
     * 
     * @param type
     *            the filter type
     * @param collection
     *            a collection of <code>Subnet</code>s to filter out/in.
     */
    public RemoteIpFilter(IpFilterType type,
            Collection<? extends Subnet> collection) {
        super(collection);
        this.type = type;
    }

    /**
     * Creates a new instance of <code>RemoteIpFilter</code>.
     * 
     * @param type
     *            the filter type
     * @param addresses
     *            a comma, space, tab, CR, LF separated list of IP
     *            addresses/CIDRs.
     * @throws UnknownHostException
     *             propagated
     * @throws NumberFormatException
     *             propagated
     */
    public RemoteIpFilter(IpFilterType type, String addresses)
            throws NumberFormatException, UnknownHostException {
        super();
        this.type = type;
        if (addresses != null) {
            String[] tokens = addresses.split("[\\s,]+");
            for (String token : tokens) {
                if (token.trim().length() > 0) {
                    add(token);
                }
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "Created DefaultIpFilter of type {} with the subnets {}",
                    type, this);
        }
    }

    /**
     * Returns the type of this filter.
     * 
     * @return the type of this filter.
     */
    public IpFilterType getType() {
        return type;
    }

    /**
     * Sets the type of this filter.
     * 
     * @param type
     *            the type of this filter.
     */
    public void setType(IpFilterType type) {
        this.type = type;
    }

    /**
     * Adds the given string representation of InetAddress or CIDR notation to
     * this filter.
     * 
     * @param str
     *            the string representation of InetAddress or CIDR notation
     * @return if the given element was added or not. <code>true</code>, if the
     *         given element was added to the filter; <code>false</code>, if the
     *         element already exists in the filter.
     * @throws NumberFormatException
     *             propagated
     * @throws UnknownHostException
     *             propagated
     */
    public boolean add(String str) throws NumberFormatException,
            UnknownHostException {
        // This is required so we do not block loopback address if some one adds
        // a string with blanks as the InetAddress class assumes loopback
        // address on a blank string.
        if (str.trim().length() < 1) {
            throw new IllegalArgumentException("Invalid IP Address or Subnet: "
                    + str);
        }
        String[] tokens = str.split("/");
        if (tokens.length == 2) {
            return add(new Subnet(InetAddress.getByName(tokens[0]), Integer
                    .parseInt(tokens[1])));
        } else {
            return add(new Subnet(InetAddress.getByName(tokens[0]), 32));
        }
    }

    public boolean accept(IoSession session) {
        InetAddress address = ((InetSocketAddress) session.getRemoteAddress())
                .getAddress();
        switch (type) {
        case ALLOW:
            for (Subnet subnet : this) {
                if (subnet.inSubnet(address)) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER
                                .debug(
                                        "Allowing connection from {} because it matches with the whitelist subnet {}",
                                        new Object[] { address, subnet });
                    }
                    return true;
                }
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER
                        .debug(
                                "Denying connection from {} because it does not match any of the whitelist subnets",
                                new Object[] { address });
            }
            return false;
        case DENY:
            if (isEmpty()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER
                            .debug(
                                    "Allowing connection from {} because blacklist is empty",
                                    new Object[] { address });
                }
                return true;
            }
            for (Subnet subnet : this) {
                if (subnet.inSubnet(address)) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER
                                .debug(
                                        "Denying connection from {} because it matches with the blacklist subnet {}",
                                        new Object[] { address, subnet });
                    }
                    return false;
                }
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER
                        .debug(
                                "Allowing connection from {} because it does not match any of the blacklist subnets",
                                new Object[] { address });
            }
            return true;
        default:
            throw new RuntimeException("Unknown or unimplemented filter type: "
                    + type);
        }
    }
}
