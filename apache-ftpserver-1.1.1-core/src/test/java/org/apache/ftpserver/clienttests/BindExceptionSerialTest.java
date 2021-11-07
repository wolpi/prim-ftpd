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

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.ftpserver.DataConnectionConfigurationFactory;

/**
*
* From FTPSERVER-250
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class BindExceptionSerialTest extends ClientTestTemplate {
    @Override
    protected FTPClient createFTPClient() throws Exception {
        FTPClient c = super.createFTPClient();
        c.setDataTimeout(1000);
        return c;
    }

    @Override
    protected DataConnectionConfigurationFactory createDataConnectionConfigurationFactory() {
        DataConnectionConfigurationFactory factory = super.createDataConnectionConfigurationFactory();
        factory.setActiveLocalPort(2020);
        factory.setActiveLocalAddress("localhost");
        return factory;
    }

    @Override
    protected void connectClient() throws Exception {
        super.connectClient();
        client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    public void testSerialExecution() throws Exception {
        assertNotNull(client.listFiles());
        assertNotNull(client.listFiles());
    }
}