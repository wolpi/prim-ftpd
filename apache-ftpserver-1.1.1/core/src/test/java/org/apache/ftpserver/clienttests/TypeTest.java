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

import org.apache.commons.net.ftp.FTPReply;
import org.apache.ftpserver.ftplet.DataType;
import org.apache.ftpserver.ftplet.FtpSession;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class TypeTest extends ClientTestTemplate {

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.ftpserver.clienttests.ClientTestTemplate#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    private FtpSession getFtpSession() {
        return server.getListener("default").getActiveSessions().iterator().next().getFtpletSession();
    }
    
    public void testTypeIAndA() throws Exception {
        assertEquals(DataType.ASCII, getFtpSession().getDataType());
        
        // send TYPE I
        assertTrue(FTPReply.isPositiveCompletion(client.type(2)));

        assertEquals(DataType.BINARY, getFtpSession().getDataType());

        // send TYPE A
        assertTrue(FTPReply.isPositiveCompletion(client.type(0)));

        assertEquals(DataType.ASCII, getFtpSession().getDataType());
    }

    public void testUnknownType() throws Exception {
        assertEquals(DataType.ASCII, getFtpSession().getDataType());
        
        // send TYPE N, not supported by FtpServer
        assertTrue(FTPReply.isNegativePermanent(client.type(4)));

        assertEquals(DataType.ASCII, getFtpSession().getDataType());
    }

    public void testTypeNoArgument() throws Exception {
        assertEquals(DataType.ASCII, getFtpSession().getDataType());
        
        // send TYPE N, not supported by FtpServer
        assertTrue(FTPReply.isNegativePermanent(client.sendCommand("TYPE")));

        assertEquals(DataType.ASCII, getFtpSession().getDataType());
    }

}
