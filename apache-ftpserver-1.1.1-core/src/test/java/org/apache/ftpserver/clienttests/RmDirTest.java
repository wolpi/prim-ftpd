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

/**
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 */
public class RmDirTest extends ClientTestTemplate {

    private static final File TEST_DIR1 = new File(ROOT_DIR, "dir1");
    private static final File TEST_DIR_IN_DIR1 = new File(TEST_DIR1, "dir3");
    private static final File TEST_CWD = new File(ROOT_DIR, "dir4");

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

    public void testRmdir() throws Exception {
        assertTrue(TEST_DIR1.mkdirs());
        assertTrue(client.removeDirectory(TEST_DIR1.getName()));

        assertFalse(TEST_DIR1.exists());
    }

    public void testRmdirNestedDir() throws Exception {
        assertTrue(TEST_DIR_IN_DIR1.mkdirs());
        assertTrue(client.removeDirectory(TEST_DIR1.getName() + "/" + TEST_DIR_IN_DIR1.getName()));

        assertTrue(TEST_DIR1.exists());
        assertFalse(TEST_DIR_IN_DIR1.exists());
    }

    public void testRmdirNoDirectoryName() throws Exception {
        assertEquals(501, client.sendCommand("RMD"));
    }

    public void testRmdirInValidDirectoryName() throws Exception {
        assertEquals(550, client.sendCommand("RMD foo:bar;foo"));
    }

    public void testRmdirOnFile() throws Exception {
        assertTrue(TEST_DIR1.createNewFile());

        assertFalse(client.removeDirectory(TEST_DIR1.getName()));

        assertTrue(TEST_DIR1.exists());
    }

    public void testRmdirWithoutWriteAccess() throws Exception {
        client.rein();
        client.login(ANONYMOUS_USERNAME, ANONYMOUS_PASSWORD);

        assertTrue(TEST_DIR1.mkdirs());

        assertFalse(client.removeDirectory(TEST_DIR1.getName()));

        assertTrue(TEST_DIR1.exists());
    }

    public void testRmdirCurrentWorkingDirectory() throws Exception {
        assertTrue(TEST_CWD.mkdirs());

        assertTrue(client.changeWorkingDirectory("/" + TEST_CWD.getName()));

        assertEquals(450, client.sendCommand("RMD ."));
        assertTrue(TEST_CWD.exists());

        assertEquals(false, client.removeDirectory("."));
        assertTrue(TEST_CWD.exists());

        assertEquals(450, client.sendCommand("RMD " + "/" + TEST_CWD.getName()));
        assertTrue(TEST_CWD.exists());

        assertEquals(false, client.removeDirectory("/" + TEST_CWD.getName()));
        assertTrue(TEST_CWD.exists());

        assertEquals(450, client.sendCommand("RMD " + "../" + TEST_CWD.getName()));
        assertTrue(TEST_CWD.exists());

        assertEquals(false, client.removeDirectory("../" + TEST_CWD.getName()));
        assertTrue(TEST_CWD.exists());

        assertEquals(450, client.sendCommand("RMD " + "././."));
        assertTrue(TEST_CWD.exists());

        assertEquals(false, client.removeDirectory("././."));
        assertTrue(TEST_CWD.exists());

        // Test for case-insensitive servers. In case it is case sensitive we'll receive a 550 response
        // so this test should  end successfully in both cases.
        assertEquals(false, client.removeDirectory("/" + TEST_CWD.getName().toUpperCase()));
        assertTrue(TEST_CWD.exists());
    }
}
