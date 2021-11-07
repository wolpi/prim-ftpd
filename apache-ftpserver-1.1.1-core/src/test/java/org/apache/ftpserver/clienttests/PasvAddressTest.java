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

package org.apache.ftpserver.clienttests;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.apache.ftpserver.DataConnectionConfiguration;
import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.test.TestUtil;
import org.apache.ftpserver.util.SocketAddressEncoder;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class PasvAddressTest extends ClientTestTemplate {

    private String passiveAddress;

    @Override
    protected FtpServerFactory createServer() throws Exception {
        FtpServerFactory server = super.createServer();

        ListenerFactory listenerFactory = new ListenerFactory(server.getListener("default"));
        
        DataConnectionConfigurationFactory dccFactory = new DataConnectionConfigurationFactory();
        
        passiveAddress = TestUtil.findNonLocalhostIp().getHostAddress();
        dccFactory.setPassiveAddress(passiveAddress);
        dccFactory.setPassivePorts("12347");
        DataConnectionConfiguration dcc=dccFactory.createDataConnectionConfiguration();
        
        listenerFactory.setDataConnectionConfiguration(dcc);
        
        server.addListener("default", listenerFactory.createListener());
        return server;
    }

    public void testPasvAddress() throws Exception {
        client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
        client.pasv();

        String reply = client.getReplyString();

        String ipEncoded = SocketAddressEncoder.encode(new InetSocketAddress(
                InetAddress.getByName(passiveAddress), 12347));

        assertTrue("The PASV address should contain \"" + ipEncoded
                + "\" but was \"" + reply + "\"", reply.indexOf(ipEncoded) > -1);
    }
}
