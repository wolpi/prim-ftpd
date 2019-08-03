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
public class RenameTest extends ClientTestTemplate {
    private static final File TEST_FILE1 = new File(ROOT_DIR, "test1.txt");

    private static final File TEST_FILE2 = new File(ROOT_DIR, "test2.txt");

    private static final File TEST_FILE3 = new File(ROOT_DIR, "test3.txt");

    private static final File TEST_DIR1 = new File(ROOT_DIR, "dir1");

    private static final File TEST_DIR2 = new File(ROOT_DIR, "dir2");

    private static final File TEST_FILE_IN_DIR1 = new File(TEST_DIR1,
            "test4.txt");

    private static final File TEST_FILE_IN_DIR2 = new File(TEST_DIR2,
            "test4.txt");

    private static final File TEST_FILE2_IN_DIR2 = new File(TEST_DIR2,
            "test5.txt");

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

    public void testRename() throws Exception {
        TEST_FILE1.createNewFile();

        assertTrue(TEST_FILE1.exists());
        assertFalse(TEST_FILE2.exists());

        assertTrue(client.rename(TEST_FILE1.getName(), TEST_FILE2.getName()));

        assertFalse(TEST_FILE1.exists());
        assertTrue(TEST_FILE2.exists());
    }

    public void testRenameEmptyDir() throws Exception {
        TEST_DIR1.mkdirs();

        assertTrue(TEST_DIR1.exists());
        assertFalse(TEST_DIR2.exists());

        assertTrue(client.rename(TEST_DIR1.getName(), TEST_DIR2.getName()));

        assertFalse(TEST_DIR1.exists());
        assertTrue(TEST_DIR2.exists());
    }

    public void testRenameDirWithFile() throws Exception {
        TEST_DIR1.mkdirs();
        TEST_FILE_IN_DIR1.createNewFile();

        assertTrue(TEST_DIR1.exists());
        assertFalse(TEST_DIR2.exists());
        assertTrue(TEST_FILE_IN_DIR1.exists());
        assertFalse(TEST_FILE_IN_DIR2.exists());

        assertTrue(client.rename(TEST_DIR1.getName(), TEST_DIR2.getName()));

        assertFalse(TEST_DIR1.exists());
        assertTrue(TEST_DIR2.exists());
        assertFalse(TEST_FILE_IN_DIR1.exists());
        assertTrue(TEST_FILE_IN_DIR2.exists());
    }

    public void testRenameWithPath() throws Exception {
        TEST_DIR1.mkdirs();
        TEST_DIR2.mkdirs();
        TEST_FILE_IN_DIR1.createNewFile();

        assertTrue(TEST_DIR1.exists());
        assertTrue(TEST_DIR2.exists());
        assertTrue(TEST_FILE_IN_DIR1.exists());
        assertFalse(TEST_FILE2_IN_DIR2.exists());

        assertTrue(client.rename(TEST_DIR1.getName() + "/"
                + TEST_FILE_IN_DIR1.getName(), TEST_DIR2.getName() + "/"
                + TEST_FILE2_IN_DIR2.getName()));

        assertTrue(TEST_DIR1.exists());
        assertTrue(TEST_DIR2.exists());
        assertFalse(TEST_FILE_IN_DIR1.exists());
        assertTrue(TEST_FILE2_IN_DIR2.exists());
    }

    public void testRenameWithoutWriteAccess() throws Exception {
        client.rein();
        client.login(ANONYMOUS_USERNAME, ANONYMOUS_PASSWORD);

        TEST_FILE1.createNewFile();

        assertTrue(TEST_FILE1.exists());
        assertFalse(TEST_FILE2.exists());

        assertFalse(client.rename(TEST_FILE1.getName(), TEST_FILE2.getName()));

        assertTrue(TEST_FILE1.exists());
        assertFalse(TEST_FILE2.exists());
    }

    public void testRenameNonExistingFile() throws Exception {

        assertFalse(TEST_FILE1.exists());
        assertFalse(TEST_FILE2.exists());

        assertFalse(client.rename(TEST_FILE1.getName(), TEST_FILE2.getName()));

        assertFalse(TEST_FILE1.exists());
        assertFalse(TEST_FILE2.exists());
    }

    public void testRenameToFileExists() throws Exception {
        TEST_FILE1.createNewFile();
        TEST_FILE2.createNewFile();

        assertTrue(TEST_FILE1.exists());
        assertTrue(TEST_FILE2.exists());

        assertFalse(client.rename(TEST_FILE1.getName(), TEST_FILE2.getName()));

        assertTrue(TEST_FILE1.exists());
        assertTrue(TEST_FILE2.exists());
    }

    public void testRenameWithNoopInBetween() throws Exception {
        TEST_FILE1.createNewFile();

        assertTrue(TEST_FILE1.exists());
        assertFalse(TEST_FILE2.exists());

        assertTrue(FTPReply.isPositiveIntermediate(client.rnfr(TEST_FILE1
                .getName())));
        assertTrue(FTPReply.isPositiveCompletion(client.noop()));
        assertTrue(FTPReply.isNegativePermanent(client.rnto(TEST_FILE2
                .getName())));

        assertTrue(TEST_FILE1.exists());
        assertFalse(TEST_FILE2.exists());
    }

    public void testRenameWithDoubleRnfr() throws Exception {
        TEST_FILE1.createNewFile();
        TEST_FILE3.createNewFile();

        assertTrue(TEST_FILE1.exists());
        assertFalse(TEST_FILE2.exists());
        assertTrue(TEST_FILE3.exists());

        assertTrue(FTPReply.isPositiveIntermediate(client.rnfr(TEST_FILE1
                .getName())));
        assertTrue(FTPReply.isPositiveIntermediate(client.rnfr(TEST_FILE3
                .getName())));
        assertTrue(FTPReply.isPositiveCompletion(client.rnto(TEST_FILE2
                .getName())));

        assertTrue(TEST_FILE1.exists());
        assertTrue(TEST_FILE2.exists());
        assertFalse(TEST_FILE3.exists());
    }

    public void testRenameOnlyRnto() throws Exception {
        assertTrue(FTPReply.isNegativePermanent(client.rnto(TEST_FILE2
                .getName())));
    }

    public void testRenameWithNullRnfrPath() throws Exception {
        TEST_FILE1.createNewFile();

        assertTrue(TEST_FILE1.exists());
        assertFalse(TEST_FILE2.exists());

        assertTrue(FTPReply.isNegativePermanent(client.rnfr(null)));

        assertTrue(TEST_FILE1.exists());
        assertFalse(TEST_FILE2.exists());
    }

    public void testRenameWithNullRntoPath() throws Exception {
        TEST_FILE1.createNewFile();

        assertTrue(TEST_FILE1.exists());
        assertFalse(TEST_FILE2.exists());

        assertTrue(FTPReply.isPositiveIntermediate(client.rnfr(TEST_FILE1
                .getName())));
        assertTrue(FTPReply.isNegativePermanent(client.rnto(null)));

        assertTrue(TEST_FILE1.exists());
        assertFalse(TEST_FILE2.exists());
    }

}
