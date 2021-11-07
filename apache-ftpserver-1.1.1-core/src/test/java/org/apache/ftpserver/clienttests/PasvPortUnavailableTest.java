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

import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.test.TestUtil;
import org.apache.commons.net.ftp.FTPClient;


/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class PasvPortUnavailableTest extends ClientTestTemplate {

    private int passivePort;

    @Override
    protected FtpServerFactory createServer() throws Exception {
        FtpServerFactory server = super.createServer();

        ListenerFactory listenerFactory = new ListenerFactory(server
                .getListener("default"));

        DataConnectionConfigurationFactory dccFactory = new DataConnectionConfigurationFactory();

        passivePort = TestUtil.findFreePort(12444);

        dccFactory.setPassivePorts(String.valueOf(passivePort));

        listenerFactory.setDataConnectionConfiguration(dccFactory
                .createDataConnectionConfiguration());

        server.addListener("default", listenerFactory.createListener());

        return server;
    }

    public void testPasvPortUnavailable() throws Exception {
    	FTPClient[] clients = new FTPClient[3];
    	for(int i = 0; i < 3; i ++) {
    		clients[i] = createFTPClient();
    		clients[i] .connect("localhost", getListenerPort());
    		clients[i].login(ADMIN_USERNAME, ADMIN_PASSWORD);
    		clients[i].pasv();
    		if(i < 1) {
    			assertTrue(clients[i].getReplyString(), clients[i].getReplyString().trim().startsWith("227"));
    		}
    		else {
    			assertTrue(clients[i].getReplyString(), clients[i].getReplyString().trim().startsWith("425"));
    		}
    	}
    	for(int i = 0; i < 3; i ++) {
    		if(clients[i] != null) {
    			clients[i].disconnect();
    		}
    	}
    }
}
