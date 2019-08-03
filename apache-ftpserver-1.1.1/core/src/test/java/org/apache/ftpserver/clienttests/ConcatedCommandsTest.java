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

/**
 * Tests that commands sent simultaniously are handled correctly.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 */
public class ConcatedCommandsTest extends ClientTestTemplate {

    public void testLogin() throws Exception {
        // send both commands, expect a 331 response
        assertEquals(331, client.sendCommand("USER admin\r\nPASS admin"));

        // make sure we wait for the 230 to come back
        client.completePendingCommand();
        assertEquals(230, client.getReplyCode());

        assertTrue(FTPReply.isPositiveCompletion(client.noop()));
    }

}
