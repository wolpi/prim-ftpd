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

import java.io.File;

import org.apache.commons.net.ftp.FTPReply;
import org.apache.ftpserver.test.TestUtil;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class SizeTest extends ClientTestTemplate {
    protected static final File TEST_DIR1 = new File(ROOT_DIR, "dir1");

    protected static final File TEST_FILE1 = new File(ROOT_DIR, "file1.txt");

    protected static final byte[] TEST_DATA1 = "TESTDATA".getBytes();

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

    private void assertSizeReply(String reply, int size) {
        reply = reply.trim();
        reply = reply.substring(4);

        assertEquals(size, Integer.parseInt(reply));
    }

    public void testSizeOnFile() throws Exception {
        TestUtil.writeDataToFile(TEST_FILE1, TEST_DATA1);

        assertTrue(FTPReply.isPositiveCompletion(client.sendCommand("SIZE "
                + TEST_FILE1.getName())));
        assertSizeReply(client.getReplyString(), TEST_DATA1.length);

        assertTrue(FTPReply.isPositiveCompletion(client.sendCommand("SIZE /"
                + TEST_FILE1.getName())));
        assertSizeReply(client.getReplyString(), TEST_DATA1.length);
    }

    public void testSizeNoArgument() throws Exception {
        assertEquals(501, client.sendCommand("SIZE "));
    }

    public void testSizeNonExistigFile() throws Exception {
        assertEquals(550, client.sendCommand("SIZE foo"));
    }

    public void testSizeOnDir() throws Exception {
        TEST_DIR1.mkdirs();
        assertEquals(550, client.sendCommand("SIZE " + TEST_DIR1.getName()));
    }

}
