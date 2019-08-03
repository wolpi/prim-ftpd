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

package org.apache.ftpserver.util;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * Encodes and decodes socket addresses (IP and port) from and to the format
 * used with for example the PORT and PASV command
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SocketAddressEncoder {

    private static int convertAndValidateNumber(String s) {
        int i = Integer.parseInt(s);
        if (i < 0) {
            throw new IllegalArgumentException("Token can not be less than 0");
        } else if (i > 255) {
            throw new IllegalArgumentException(
                    "Token can not be larger than 255");
        }

        return i;
    }

    public static InetSocketAddress decode(String str)
            throws UnknownHostException {
        StringTokenizer st = new StringTokenizer(str, ",");
        if (st.countTokens() != 6) {
            throw new IllegalInetAddressException("Illegal amount of tokens");
        }

        StringBuilder sb = new StringBuilder();
        try {
            sb.append(convertAndValidateNumber(st.nextToken()));
            sb.append('.');
            sb.append(convertAndValidateNumber(st.nextToken()));
            sb.append('.');
            sb.append(convertAndValidateNumber(st.nextToken()));
            sb.append('.');
            sb.append(convertAndValidateNumber(st.nextToken()));
        } catch (IllegalArgumentException e) {
            throw new IllegalInetAddressException(e.getMessage());
        }

        InetAddress dataAddr = InetAddress.getByName(sb.toString());

        // get data server port
        int dataPort = 0;
        try {
            int hi = convertAndValidateNumber(st.nextToken());
            int lo = convertAndValidateNumber(st.nextToken());
            dataPort = (hi << 8) | lo;
        } catch (IllegalArgumentException ex) {
            throw new IllegalPortException("Invalid data port: " + str);
        }

        return new InetSocketAddress(dataAddr, dataPort);
    }

    public static String encode(InetSocketAddress address) {
        InetAddress servAddr = address.getAddress();
        int servPort = address.getPort();
        return servAddr.getHostAddress().replace('.', ',') + ','
                + (servPort >> 8) + ',' + (servPort & 0xFF);
    }

}
