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
import org.apache.ftpserver.command.CommandFactoryFactory;
import org.apache.ftpserver.command.impl.PASV;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.listener.ListenerFactory;

/**
 * Test for external passive address configured as hostname rather than IP
 * address.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @version $Rev$, $Date$
 *
 */
public class PasvAddressWithOverridenHostnameGetter extends ClientTestTemplate {

    class PASVTest extends PASV{

        @Override
        protected String getPassiveExternalAddress(FtpIoSession session) {
            return "10.10.10.10";
        }
        
    }
    @Override
    protected FtpServerFactory createServer() throws Exception {
        FtpServerFactory server = super.createServer();

        ListenerFactory listenerFactory = new ListenerFactory(server.getListener("default"));
        
        DataConnectionConfigurationFactory dccFactory = new DataConnectionConfigurationFactory();

        dccFactory.setPassiveExternalAddress("127.0.0.1");

        listenerFactory.setDataConnectionConfiguration(dccFactory.createDataConnectionConfiguration());

        server.addListener("default", listenerFactory.createListener());
        CommandFactoryFactory cmFact = new CommandFactoryFactory();
        cmFact.setUseDefaultCommands(true);
        cmFact.addCommand("PASV", new PASVTest());
        server.setCommandFactory(cmFact.createCommandFactory());
        return server;
    }

    public void testPasvAddress() throws Exception {
        client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
        client.pasv();

        assertTrue(client.getReplyString().indexOf("(10,10,10,10,") > -1);
    }
}
