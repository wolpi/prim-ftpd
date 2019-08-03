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

import java.net.ServerSocket;

import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.test.TestUtil;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class PasvUsedPortTest extends ClientTestTemplate {

    private int passivePort;

    @Override
    protected FtpServerFactory createServer() throws Exception {
        FtpServerFactory server = super.createServer();

        ListenerFactory listenerFactory = new ListenerFactory(server
                .getListener("default"));

        DataConnectionConfigurationFactory dccFactory = new DataConnectionConfigurationFactory();

        passivePort = TestUtil.findFreePort(12444);

        dccFactory.setPassivePorts(passivePort + "-" + (passivePort + 1));

        listenerFactory.setDataConnectionConfiguration(dccFactory
                .createDataConnectionConfiguration());

        server.addListener("default", listenerFactory.createListener());

        return server;
    }

    public void testPasvWithUsedPort() throws Exception {
        // bind to the first available passive port
        ServerSocket ss = new ServerSocket(passivePort);
        
        client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
        client.pasv();
        assertEquals("227 Entering Passive Mode (127,0,0,1,48,157)", client.getReplyString().trim());
        client.quit();
        client.disconnect();

        // close blocking socket
        ss.close();
    }
}
