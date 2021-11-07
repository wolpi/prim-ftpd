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
import java.io.IOException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.ftpserver.test.TestUtil;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class ActiveModeReplyTest extends ClientTestTemplate {
    private static final File TEST_TMP_DIR = new File("test-tmp");

    private static final File TEST_FILE = new File(ROOT_DIR, "test.txt");

    private static final File TEST_FILE1 = new File(TEST_TMP_DIR, "test1.txt");

    private static byte[] testData;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        TEST_FILE1.createNewFile();
        assertTrue(TEST_FILE1.exists());

        testData = ("TESTDATA").getBytes("UTF-8");
        TestUtil.writeDataToFile(TEST_FILE, testData);
        assertTrue(TEST_FILE.exists());

        FTPClientConfig config = new FTPClientConfig("UNIX");
        client.configure(config);

        client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    public void testStoreInActiveModeIfNotAllowed() throws Exception {
        assertTrue(client.getDataConnectionMode() == FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE);

        sendCommand("APPE " + TEST_FILE1.getAbsolutePath());
        sendCommand("LIST");
        sendCommand("MLSD");
        sendCommand("NLST");
        sendCommand("RETR " + TEST_FILE.getName());
        sendCommand("STOR " + TEST_FILE1.getAbsolutePath());
        sendCommand("STOU");
    }

    private void sendCommand(final String command) throws IOException {
        final int returnCode = client.sendCommand(command);
        assertEquals(503, returnCode);
        assertEquals("503 PORT or PASV must be issued first", client
                .getReplyString().trim());
    }

}
