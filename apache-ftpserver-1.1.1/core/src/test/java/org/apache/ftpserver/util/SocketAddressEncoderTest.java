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

import java.net.InetSocketAddress;

import junit.framework.TestCase;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class SocketAddressEncoderTest extends TestCase {

    public void testEncodeLowPort() {
        InetSocketAddress address = new InetSocketAddress("localhost", 21);

        assertEquals("127,0,0,1,0,21", SocketAddressEncoder.encode(address));
    }

    public void testEncodeHighPort() {
        InetSocketAddress address = new InetSocketAddress("localhost", 21123);

        assertEquals("127,0,0,1,82,131", SocketAddressEncoder.encode(address));
    }

    public void testEncodeIpNumber() {
        InetSocketAddress address = new InetSocketAddress("1.2.3.4", 21);

        assertEquals("1,2,3,4,0,21", SocketAddressEncoder.encode(address));
    }

    public void testDecodeLowPort() throws Exception {
        InetSocketAddress address = new InetSocketAddress("1.2.3.4", 21);

        assertEquals(address, SocketAddressEncoder.decode("1,2,3,4,0,21"));
    }

    public void testDecodeHighPort() throws Exception {
        InetSocketAddress address = new InetSocketAddress("1.2.3.4", 21123);

        assertEquals(address, SocketAddressEncoder.decode("1,2,3,4,82,131"));
    }

    public void testDecodeTooFewTokens() throws Exception {
        try {
            SocketAddressEncoder.decode("1,2,3,4,82");
            fail("Must throw IllegalInetAddressException");
        } catch (IllegalInetAddressException e) {
            // OK
        } catch (Exception e) {
            fail("Must throw IllegalInetAddressException");
        }
    }

    public void testDecodeTooManyTokens() throws Exception {
        try {
            SocketAddressEncoder.decode("1,2,3,4,82,1,2");
            fail("Must throw IllegalInetAddressException");
        } catch (IllegalInetAddressException e) {
            // OK
        } catch (Exception e) {
            fail("Must throw IllegalInetAddressException");
        }
    }

    public void testDecodeToHighPort() {
        try {
            SocketAddressEncoder.decode("1,2,3,4,820,2");
            fail("Must throw IllegalPortException");
        } catch (IllegalPortException e) {
            // OK
        } catch (Exception e) {
            fail("Must throw IllegalPortException");
        }
    }

    public void testDecodeIPTokenNotANumber() {
        try {
            SocketAddressEncoder.decode("foo,2,3,4,5,6");
            fail("Must throw IllegalInetAddressException");
        } catch (IllegalInetAddressException e) {
            // OK
        } catch (Exception e) {
            fail("Must throw IllegalInetAddressException");
        }
    }

    public void testDecodePortTokenNotANumber() {
        try {
            SocketAddressEncoder.decode("1,2,3,4,foo,6");
            fail("Must throw IllegalPortException");
        } catch (IllegalPortException e) {
            // OK
        } catch (Exception e) {
            fail("Must throw IllegalPortException");
        }
    }
}