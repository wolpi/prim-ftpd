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

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class DirectoryTest extends ClientTestTemplate {
    private static final File TEST_DIR1 = new File(ROOT_DIR, "dir1");

    private static final File TEST_DIR_IN_DIR1 = new File(TEST_DIR1, "dir3");

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

    public void testMkdir() throws Exception {
        assertFalse(TEST_DIR1.exists());

        assertTrue(FTPReply.isPositiveCompletion(client
                .mkd(TEST_DIR1.getName())));

        assertTrue(TEST_DIR1.exists());
    }

    public void testMkdirNoDirectoryName() throws Exception {
        assertEquals(501, client.sendCommand("MKD"));
    }

    /**
     * FTPSERVER-233, we should not recursively create directories  
     */
    public void testMkdirDouble() throws Exception {
        assertFalse(TEST_DIR1.exists());
        assertFalse(TEST_DIR_IN_DIR1.exists());

        assertFalse(FTPReply.isPositiveCompletion(client.mkd(TEST_DIR1.getName()
                + '/' + TEST_DIR_IN_DIR1.getName())));

        assertFalse(TEST_DIR1.exists());
        assertFalse(TEST_DIR_IN_DIR1.exists());
    }

    public void testMkdirDoubleFirstExists() throws Exception {
        TEST_DIR1.mkdirs();
        assertTrue(TEST_DIR1.exists());
        assertFalse(TEST_DIR_IN_DIR1.exists());

        assertTrue(FTPReply.isPositiveCompletion(client.mkd(TEST_DIR1.getName()
                + '/' + TEST_DIR_IN_DIR1.getName())));

        assertTrue(TEST_DIR1.exists());
        assertTrue(TEST_DIR_IN_DIR1.exists());
    }

    public void testMkdirExisting() throws Exception {
        TEST_DIR1.mkdirs();

        assertTrue(TEST_DIR1.exists());

        assertTrue(FTPReply
                .isNegativePermanent(client.mkd(TEST_DIR1.getName())));

        assertTrue(TEST_DIR1.exists());
    }

    public void testMkdirExistingFile() throws Exception {
        TEST_DIR1.createNewFile();

        assertTrue(TEST_DIR1.exists());

        assertTrue(FTPReply
                .isNegativePermanent(client.mkd(TEST_DIR1.getName())));

        assertTrue(TEST_DIR1.exists());
    }

    public void testMkdirWithoutWriteAccess() throws Exception {
        client.rein();
        client.login(ANONYMOUS_USERNAME, ANONYMOUS_PASSWORD);

        assertFalse(TEST_DIR1.exists());

        assertTrue(FTPReply
                .isNegativePermanent(client.mkd(TEST_DIR1.getName())));

        assertFalse(TEST_DIR1.exists());
    }
}
