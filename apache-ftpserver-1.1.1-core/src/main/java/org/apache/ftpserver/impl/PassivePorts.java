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

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * Provides support for parsing a passive ports string as well as keeping track
 * of reserved passive ports.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class PassivePorts {

    private Logger log = LoggerFactory.getLogger(PassivePorts.class);

    private static final int MAX_PORT = 65535;

    private static final Integer MAX_PORT_INTEGER = Integer.valueOf(MAX_PORT);

    private List<Integer> freeList;

    private Set<Integer> usedList;

    private Random r = new Random();

    private String passivePortsString;

    private boolean checkIfBound;

    /**
     * Parse a string containing passive ports
     * 
     * @param portsString
     *            A string of passive ports, can contain a single port (as an
     *            integer), multiple ports seperated by commas (e.g.
     *            123,124,125) or ranges of ports, including open ended ranges
     *            (e.g. 123-125, 30000-, -1023). Combinations for single ports
     *            and ranges is also supported.
     * @return A list of Integer objects, based on the parsed string
     * @throws IllegalArgumentException
     *             If any of of the ports in the string is invalid (e.g. not an
     *             integer or too large for a port number)
     */
    private static Set<Integer> parse(final String portsString) {
        Set<Integer> passivePortsList = new HashSet<Integer>();

        boolean inRange = false;
        Integer lastPort = Integer.valueOf(1);
        StringTokenizer st = new StringTokenizer(portsString, ",;-", true);
        while (st.hasMoreTokens()) {
            String token = st.nextToken().trim();

            if (",".equals(token) || ";".equals(token)) {
                if (inRange) {
                    fillRange(passivePortsList, lastPort, MAX_PORT_INTEGER);
                }

                // reset state
                lastPort = Integer.valueOf(1);
                inRange = false;
            } else if ("-".equals(token)) {
                inRange = true;
            } else if (token.length() == 0) {
                // ignore whitespace
            } else {
                Integer port = Integer.valueOf(token);

                verifyPort(port);

                if (inRange) {
                    // add all numbers from last int
                    fillRange(passivePortsList, lastPort, port);

                    inRange = false;
                }

                addPort(passivePortsList, port);

                lastPort = port;
            }
        }

        if (inRange) {
            fillRange(passivePortsList, lastPort, MAX_PORT_INTEGER);
        }

        return passivePortsList;
    }

    /**
     * Fill a range of ports
     */
    private static void fillRange(final Set<Integer> passivePortsList, final Integer beginPort, final Integer endPort) {
        for (int i = beginPort; i <= endPort; i++) {
            addPort(passivePortsList, Integer.valueOf(i));
        }
    }

    /**
     * Add a single port if not already in list
     */
    private static void addPort(final Set<Integer> passivePortsList, final Integer port) {
        passivePortsList.add(port);
    }

    /**
     * Verify that the port is within the range of allowed ports
     */
    private static void verifyPort(final int port) {
        if (port < 0) {
            throw new IllegalArgumentException("Port can not be negative: " + port);
        } else if (port > MAX_PORT) {
            throw new IllegalArgumentException("Port too large: " + port);
        }
    }

    public PassivePorts(final String passivePorts, boolean checkIfBound) {
        this(parse(passivePorts), checkIfBound);

        this.passivePortsString = passivePorts;
    }

    public PassivePorts(Set<Integer> passivePorts, boolean checkIfBound) {
        if (passivePorts == null) {
            throw new NullPointerException("passivePorts can not be null");
        } else if(passivePorts.isEmpty()) {
            passivePorts = new HashSet<Integer>();
            passivePorts.add(0);
        }

        this.freeList = new ArrayList<Integer>(passivePorts);
        this.usedList = new HashSet<Integer>(passivePorts.size());

        this.checkIfBound = checkIfBound;
    }

    /**
     * Checks that the port of not bound by another application
     */
    private boolean checkPortUnbound(int port) {
        // is this check disabled?
        if (!checkIfBound) {
            return true;
        }

        // if using 0 port, it will always be available
        if (port == 0) {
            return true;
        }

        ServerSocket ss = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            // port probably in use, check next
            return false;
        } finally {
            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    // could not close, check next
                    return false;
                }
            }
        }
    }

    public synchronized int reserveNextPort() {
        // create a copy of the free ports, so that we can keep track of the tested ports
        List<Integer> freeCopy = new ArrayList<Integer>(freeList);
        
        // Loop until we have found a port, or exhausted all available ports
        while (freeCopy.size() > 0) {
            // Otherwise, pick one at random
            int i = r.nextInt(freeCopy.size());
            Integer ret = freeCopy.get(i);

            if (ret == 0) {
                // "Any" port should not be removed from our free list,
                // nor added to the used list
                return 0;

            } else if (checkPortUnbound(ret)) {
                // Not used by someone else, so lets reserve it and return it
                freeList.remove(ret);
                usedList.add(ret);
                return ret;

            } else {
                freeCopy.remove(i);
                // log port unavailable, but left in pool
                log.warn("Passive port in use by another process: " + ret);
            }
        }

        return -1;
    }

    public synchronized void releasePort(final int port) {
        if (port == 0) {
            // Ignore port 0 being released,
            // since its not put on the used list

        } else if (usedList.remove(port)) {
            freeList.add(port);

        } else {
            // log attempt to release unused port
            log.warn("Releasing unreserved passive port: " + port);
        }
    }

    @Override
    public String toString() {
        if (passivePortsString != null) {
            return passivePortsString;
        }

        StringBuilder sb = new StringBuilder();

        for (Integer port : freeList) {
            sb.append(port);
            sb.append(",");
        }
        // remove the last ,
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

}