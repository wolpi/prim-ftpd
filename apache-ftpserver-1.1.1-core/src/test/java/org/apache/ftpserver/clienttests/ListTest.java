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
import java.util.Calendar;

import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.ftpserver.test.TestUtil;
import org.apache.ftpserver.util.DateUtils;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class ListTest extends ClientTestTemplate {
    private static final File TEST_FILE1 = new File(ROOT_DIR, "test1.txt");

    private static final File TEST_FILE2 = new File(ROOT_DIR, "test2.txt");

    private static final File TEST_DIR1 = new File(ROOT_DIR, "dir1");

    private static final File TEST_DIR2 = new File(ROOT_DIR, "dir2");

    private static final File TEST_FILE_IN_DIR1 = new File(TEST_DIR1,
            "test3.txt");

    private static final File TEST_DIR_IN_DIR1 = new File(TEST_DIR1, "dir3");

    private byte[] testData;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.ftpserver.clienttests.ClientTestTemplate#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        testData = "TESDATA".getBytes("UTF-8");

        FTPClientConfig config = new FTPClientConfig("UNIX");
        client.configure(config);

        client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    public void testListFilesInDir() throws Exception {

        TEST_DIR1.mkdirs();
        TEST_FILE_IN_DIR1.createNewFile();
        TEST_DIR_IN_DIR1.mkdirs();

        FTPFile[] files = client.listFiles(TEST_DIR1.getName());

        assertEquals(2, files.length);

        FTPFile file = getFile(files, TEST_FILE_IN_DIR1.getName());
        assertEquals(TEST_FILE_IN_DIR1.getName(), file.getName());
        assertEquals(0, file.getSize());
        assertEquals("group", file.getGroup());
        assertEquals("user", file.getUser());
        assertTrue(file.isFile());
        assertFalse(file.isDirectory());

        file = getFile(files, TEST_DIR_IN_DIR1.getName());
        assertEquals(TEST_DIR_IN_DIR1.getName(), file.getName());
        assertEquals(0, file.getSize());
        assertEquals("group", file.getGroup());
        assertEquals("user", file.getUser());
        assertFalse(file.isFile());
        assertTrue(file.isDirectory());

    }

    public void testListFilesInNonExistingDir() throws Exception {
        assertEquals(450, client.sendCommand("LIST", "nonexisting"));
    }
    
    public void testListFile() throws Exception {

        TEST_DIR1.mkdirs();
        TEST_FILE1.createNewFile();
        TEST_FILE2.createNewFile();

        FTPFile[] files = client.listFiles(TEST_FILE1.getName());

        assertEquals(1, files.length);

        FTPFile file = getFile(files, TEST_FILE1.getName());
        assertEquals(TEST_FILE1.getName(), file.getName());
        assertEquals(0, file.getSize());
        assertEquals("group", file.getGroup());
        assertEquals("user", file.getUser());
        assertTrue(file.isFile());
        assertFalse(file.isDirectory());
        
        Calendar expectedTimestamp = Calendar.getInstance();
        expectedTimestamp.setTimeInMillis(TEST_FILE1.lastModified());
        // server does not supply seconds and milliseconds
        expectedTimestamp.clear(Calendar.SECOND);
        expectedTimestamp.clear(Calendar.MILLISECOND);
        
        assertEquals(expectedTimestamp, file.getTimestamp());
    }

    public void testListFileNoArgument() throws Exception {
        TEST_DIR1.mkdirs();

        FTPFile[] files = client.listFiles();

        assertEquals(1, files.length);

        FTPFile file = getFile(files, TEST_DIR1.getName());
        assertEquals(TEST_DIR1.getName(), file.getName());
        assertEquals(0, file.getSize());
        assertEquals("group", file.getGroup());
        assertEquals("user", file.getUser());
        assertFalse(file.isFile());
        assertTrue(file.isDirectory());
    }

    public void testListFiles() throws Exception {
        TEST_FILE1.createNewFile();
        TEST_FILE2.createNewFile();
        TEST_DIR1.mkdirs();
        TEST_DIR2.mkdirs();

        TestUtil.writeDataToFile(TEST_FILE1, testData);

        FTPFile[] files = client.listFiles();

        assertEquals(4, files.length);
        FTPFile file = getFile(files, TEST_FILE1.getName());
        assertEquals(TEST_FILE1.getName(), file.getName());
        assertEquals(testData.length, file.getSize());
        assertEquals("group", file.getGroup());
        assertEquals("user", file.getUser());
        assertTrue(file.isFile());
        assertFalse(file.isDirectory());

        file = getFile(files, TEST_FILE2.getName());
        assertEquals(TEST_FILE2.getName(), file.getName());
        assertEquals(0, file.getSize());
        assertEquals("group", file.getGroup());
        assertEquals("user", file.getUser());
        assertTrue(file.isFile());
        assertFalse(file.isDirectory());

        file = getFile(files, TEST_DIR1.getName());
        assertEquals(TEST_DIR1.getName(), file.getName());
        assertEquals(0, file.getSize());
        assertEquals("group", file.getGroup());
        assertEquals("user", file.getUser());
        assertFalse(file.isFile());
        assertTrue(file.isDirectory());

        file = getFile(files, TEST_DIR2.getName());
        assertEquals(TEST_DIR2.getName(), file.getName());
        assertEquals(0, file.getSize());
        assertEquals("group", file.getGroup());
        assertEquals("user", file.getUser());
        assertFalse(file.isFile());
        assertTrue(file.isDirectory());
    }

    public void testListFileNonExistingFile() throws Exception {
        TEST_DIR1.mkdirs();
        assertEquals(450, client.sendCommand("LIST", TEST_DIR1.getName() + "/nonexisting"));
    }

    public void testMLST() throws Exception {
        TEST_FILE1.createNewFile();

        assertTrue(FTPReply.isPositiveCompletion(client.sendCommand("MLST "
                + TEST_FILE1.getName())));

        String[] reply = client.getReplyString().split("\\r\\n");

        assertEquals("Size=0;Modify="
                + DateUtils.getFtpDate(TEST_FILE1.lastModified())
                + ";Type=file; " + TEST_FILE1.getName(), reply[1]);
    }

    public void testOPTSMLST() throws Exception {
        TEST_FILE1.createNewFile();

        assertTrue(FTPReply.isPositiveCompletion(client
                .sendCommand("OPTS MLST Size;Modify")));
        assertTrue(FTPReply.isPositiveCompletion(client.sendCommand("MLST "
                + TEST_FILE1.getName())));

        String[] reply = client.getReplyString().split("\\r\\n");

        assertEquals("Size=0;Modify="
                + DateUtils.getFtpDate(TEST_FILE1.lastModified()) + "; "
                + TEST_FILE1.getName(), reply[1]);
    }

    public void testOPTSMLSTCaseInsensitive() throws Exception {
        TEST_FILE1.createNewFile();
        
        assertTrue(FTPReply.isPositiveCompletion(client
                .sendCommand("OPTS MLST size;Modify")));
        assertTrue(FTPReply.isPositiveCompletion(client.sendCommand("MLST "
                + TEST_FILE1.getName())));
        
        String[] reply = client.getReplyString().split("\\r\\n");
        
        assertEquals("Size=0;Modify="
                + DateUtils.getFtpDate(TEST_FILE1.lastModified()) + "; "
                + TEST_FILE1.getName(), reply[1]);
    }

    /**
     * "Facts requested that are not
     * supported, or that are inappropriate to the file or directory being
     * listed should simply be omitted from the MLSx output."
     * 
     * http://tools.ietf.org/html/rfc3659#section-7.9
     */
    public void testOPTSMLSTUnknownFact() throws Exception {
        TEST_FILE1.createNewFile();

        assertTrue(FTPReply.isPositiveCompletion(client
                .sendCommand("OPTS MLST Foo;Size")));
        
        assertTrue(FTPReply.isPositiveCompletion(client.sendCommand("MLST "
                + TEST_FILE1.getName())));
        
        String[] reply = client.getReplyString().split("\\r\\n");
        
        assertEquals("Size=0; "
                + TEST_FILE1.getName(), reply[1]);
    }

    /**
     * "Facts requested that are not
     * supported, or that are inappropriate to the file or directory being
     * listed should simply be omitted from the MLSx output."
     * 
     * http://tools.ietf.org/html/rfc3659#section-7.9
     */
    public void testOPTSMLSTNoFacts() throws Exception {
        TEST_FILE1.createNewFile();

        assertTrue(FTPReply.isPositiveCompletion(client
                .sendCommand("OPTS MLST")));
        
        assertTrue(FTPReply.isPositiveCompletion(client.sendCommand("MLST "
                + TEST_FILE1.getName())));
        
        String[] reply = client.getReplyString().split("\\r\\n");
        
        assertEquals(" "
                + TEST_FILE1.getName(), reply[1]);
    }

    
    private FTPFile getFile(FTPFile[] files, String name) {
        for (int i = 0; i < files.length; i++) {
            FTPFile file = files[i];

            if (name.equals(file.getName())) {
                return file;
            }
        }

        return null;
    }
}
