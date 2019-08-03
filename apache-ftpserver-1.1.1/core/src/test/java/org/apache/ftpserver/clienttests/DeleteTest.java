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
public class DeleteTest extends ClientTestTemplate {
    private static final File TEST_FILE1 = new File(ROOT_DIR, "test1.txt");

    private static final File TEST_FILE_WITH_SPACE = new File(ROOT_DIR,
            "test 2.txt");

    private static final File TEST_DIR1 = new File(ROOT_DIR, "dir1");

    private static final File TEST_FILE_IN_DIR1 = new File(TEST_DIR1,
            "test2.txt");

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

    public void testDelete() throws Exception {
        TEST_FILE1.createNewFile();

        assertTrue(TEST_FILE1.exists());

        assertTrue(client.deleteFile(TEST_FILE1.getName()));

        assertFalse(TEST_FILE1.exists());
    }

    public void testDeleteNoFileName() throws Exception {
        assertEquals(501, client.sendCommand("DELE"));
    }

    public void testDeleteWithSpaceInFileName() throws Exception {
        TEST_FILE_WITH_SPACE.createNewFile();

        assertTrue(TEST_FILE_WITH_SPACE.exists());

        assertTrue(client.deleteFile(TEST_FILE_WITH_SPACE.getName()));

        assertFalse(TEST_FILE_WITH_SPACE.exists());
    }

    public void testDeleteWithoutWriteAccess() throws Exception {
        client.rein();
        client.login(ANONYMOUS_USERNAME, ANONYMOUS_PASSWORD);

        TEST_FILE1.createNewFile();

        assertTrue(TEST_FILE1.exists());

        assertFalse(client.deleteFile(TEST_FILE1.getName()));

        assertTrue(TEST_FILE1.exists());
    }

    public void testDeleteDir() throws Exception {
        TEST_DIR1.mkdirs();

        assertTrue(TEST_DIR1.exists());
        
        assertEquals(550, client.sendCommand("DELE " + TEST_DIR1.getName()));

        assertFalse(client.deleteFile(TEST_DIR1.getName()));

        assertTrue(TEST_DIR1.exists());
    }

    public void testDeleteDirWithFile() throws Exception {
        TEST_DIR1.mkdirs();
        TEST_FILE_IN_DIR1.createNewFile();

        assertTrue(TEST_DIR1.exists());
        assertTrue(TEST_FILE_IN_DIR1.exists());

        assertEquals(550, client.sendCommand("DELE " + TEST_DIR1.getName()));

        assertFalse(client.deleteFile(TEST_DIR1.getName()));

        assertTrue(TEST_DIR1.exists());
        assertTrue(TEST_FILE_IN_DIR1.exists());
    }

    public void testDeleteFileInDir() throws Exception {
        TEST_DIR1.mkdirs();
        TEST_FILE_IN_DIR1.createNewFile();

        assertTrue(TEST_DIR1.exists());
        assertTrue(TEST_FILE_IN_DIR1.exists());

        assertTrue(client.deleteFile(TEST_DIR1.getName() + '/'
                + TEST_FILE_IN_DIR1.getName()));

        assertTrue(TEST_DIR1.exists());
        assertFalse(TEST_FILE_IN_DIR1.exists());
    }
}
