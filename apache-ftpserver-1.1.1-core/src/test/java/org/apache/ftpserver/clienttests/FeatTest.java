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

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class FeatTest extends ClientTestTemplate {

    public void test() throws Exception {
        client.login(ADMIN_USERNAME, ADMIN_PASSWORD);

        client.sendCommand("FEAT");
        String[] featReplies = client.getReplyString().split("\r\n");

        for (int i = 0; i < featReplies.length; i++) {
            if (i == 0) {
                // first must be 211-Extensions supported
                assertEquals("211-Extensions supported", featReplies[i]);
            } else if (i + 1 == featReplies.length) {
                // last must be 211 End
                assertEquals("211 End", featReplies[i]);
            } else {
                // must start with a single space
                assertEquals(' ', featReplies[i].charAt(0));
                assertTrue(featReplies[i].charAt(1) != ' ');
            }
        }
    }
}
