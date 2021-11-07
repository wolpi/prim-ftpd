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

package org.apache.ftpserver.commands.impl.listing;

import java.io.File;

import junit.framework.TestCase;

import org.apache.ftpserver.command.impl.listing.DirectoryLister;
import org.apache.ftpserver.command.impl.listing.FileFormater;
import org.apache.ftpserver.command.impl.listing.ListArgument;
import org.apache.ftpserver.command.impl.listing.NLSTFileFormater;
import org.apache.ftpserver.filesystem.nativefs.impl.NativeFileSystemView;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.test.TestUtil;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.util.IoUtils;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class DirectoryListerTest extends TestCase {
    private static final File TEST_TMP_DIR = new File("test-tmp");

    protected static final File ROOT_DIR = new File(TEST_TMP_DIR, "ftproot");

    private static final File TEST_FILE1 = new File(ROOT_DIR, "test1.txt");

    private static final File TEST_DIR1 = new File(ROOT_DIR, "dir1");

    private static final File TEST_DIR2 = new File(ROOT_DIR, "dir2");

    private static final File TEST_FILE1_IN_DIR1 = new File(TEST_DIR1,
            "test3.txt");

    private static final File TEST_FILE2_IN_DIR1 = new File(TEST_DIR1,
            "test4.txt");

    private static final File TEST_DIR_IN_DIR1 = new File(TEST_DIR1, "dir3");

    private static final byte[] TEST_DATA = "TESTDATA".getBytes();

    private DirectoryLister directoryLister;

    private FileSystemView fileSystemView;

    @Override
    protected void setUp() throws Exception {
        BaseUser baseUser = new BaseUser();
        baseUser.setHomeDirectory(ROOT_DIR.getAbsolutePath());
        fileSystemView = new NativeFileSystemView(baseUser) {
        };
        directoryLister = new DirectoryLister();

        assertTrue(ROOT_DIR.mkdirs());
        assertTrue(TEST_DIR1.mkdirs());
        assertTrue(TEST_DIR2.mkdirs());
        TestUtil.writeDataToFile(TEST_FILE1, TEST_DATA);
        TestUtil.writeDataToFile(TEST_FILE1_IN_DIR1, TEST_DATA);
        TEST_FILE2_IN_DIR1.createNewFile();
        assertTrue(TEST_DIR_IN_DIR1.mkdir());
    }

    public void testListFiles() throws Exception {
        ListArgument arg = new ListArgument(TEST_DIR1.getName(), null, null);
        FileFormater formater = new NLSTFileFormater();

        String actual = directoryLister
                .listFiles(arg, fileSystemView, formater);

        assertEquals("dir3\r\ntest3.txt\r\ntest4.txt\r\n", actual);
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        if (TEST_TMP_DIR.exists()) {
            IoUtils.delete(TEST_TMP_DIR);
        }
    }
}