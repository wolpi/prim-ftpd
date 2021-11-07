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
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.ftpserver.test.TestUtil;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class FtpMd5Test extends ClientTestTemplate {
    private static final File TEST_FILE1 = new File(ROOT_DIR, "test1.txt");

    private static final File TEST_FILE_WITH_SPACE = new File(ROOT_DIR,
            "test 2.txt");

    private static final File TEST_DIR1 = new File(ROOT_DIR, "dir1");

    private static final File TEST_FILE_IN_DIR1 = new File(TEST_DIR1,
            "test4.txt");

    private static byte[] testData = null;

    private static byte[] testData2 = null;

    private static String testDataHash;

    private static String testData2Hash;

    // Enabling SSLv3 because Java 8 disable it by default...
    static
    {
        Security.setProperty( "jdk.tls.disabledAlgorithms", "RC4, MD5withRSA, DH keySize < 768" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.ftpserver.clienttests.ClientTestTemplate#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        testData = "TESTDATA".getBytes("UTF-8");
        testData2 = "Hello world".getBytes("UTF-8");
        testDataHash = DigestUtils.md5Hex(testData).toUpperCase();
        testData2Hash = DigestUtils.md5Hex(testData2).toUpperCase();

        client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    public void testMd5() throws Exception {
        TestUtil.writeDataToFile(TEST_FILE1, testData);

        assertTrue(TEST_FILE1.exists());

        String fileName = TEST_FILE1.getName();

        assertEquals(251, client.sendCommand("MD5 " + fileName));

        assertHash(testDataHash, client.getReplyString(), fileName);
    }

    public void testMd5NoFileName() throws Exception {
        assertEquals(504, client.sendCommand("MD5"));
    }

    public void testMd5NonExistingFile() throws Exception {
        assertFalse(TEST_FILE1.exists());

        String fileName = TEST_FILE1.getName();

        assertEquals(504, client.sendCommand("MD5 " + fileName));
    }

    public void testMd5WithSpaceInFileName() throws Exception {
        TestUtil.writeDataToFile(TEST_FILE_WITH_SPACE, testData);

        assertTrue(TEST_FILE_WITH_SPACE.exists());

        String fileName = TEST_FILE_WITH_SPACE.getName();

        assertEquals(251, client.sendCommand("MD5 " + fileName));

        assertHash(testDataHash, client.getReplyString(), fileName);
    }

    public void testMd5WithDirectory() throws Exception {
        TEST_DIR1.mkdirs();
        assertTrue(TEST_DIR1.exists());

        String fileName = TEST_DIR1.getName();

        assertEquals(504, client.sendCommand("MD5 " + fileName));
    }

    public void testMd5WithNonExistingFile() throws Exception {
        assertFalse(TEST_FILE1.exists());

        String fileName = TEST_FILE1.getName();

        assertEquals(504, client.sendCommand("MD5 " + fileName));
    }

    public void testMd5WithPath() throws Exception {
        TEST_DIR1.mkdirs();
        assertTrue(TEST_DIR1.exists());
        TestUtil.writeDataToFile(TEST_FILE_IN_DIR1, testData);
        assertTrue(TEST_FILE_IN_DIR1.exists());

        String fileName = TEST_DIR1.getName() + "/"
                + TEST_FILE_IN_DIR1.getName();

        assertEquals(251, client.sendCommand("MD5 " + fileName));

        assertHash(testDataHash, client.getReplyString(), fileName);
    }

    private void assertHash(String expected, String reply, String fileName) {
        Map<String, String> hashes = parseReplyHash(reply);
        assertEquals(expected, hashes.get(fileName));
    }

    public void testMMd5() throws Exception {
        TestUtil.writeDataToFile(TEST_FILE1, testData);
        TestUtil.writeDataToFile(TEST_FILE_WITH_SPACE, testData2);

        assertTrue(TEST_FILE1.exists());
        assertTrue(TEST_FILE_WITH_SPACE.exists());

        String fileNames = TEST_FILE1.getName() + ","
                + TEST_FILE_WITH_SPACE.getName();

        assertEquals(252, client.sendCommand("MMD5 " + fileNames));

        assertHash(testDataHash, client.getReplyString(), TEST_FILE1.getName());
        assertHash(testData2Hash, client.getReplyString(), TEST_FILE_WITH_SPACE
                .getName());
    }

    public void testMMd5SingleFile() throws Exception {
        TestUtil.writeDataToFile(TEST_FILE1, testData);

        assertTrue(TEST_FILE1.exists());

        String fileNames = TEST_FILE1.getName();

        assertEquals(252, client.sendCommand("MMD5 " + fileNames));

        assertHash(testDataHash, client.getReplyString(), TEST_FILE1.getName());
    }

    public void testMMd5MixedFilesAndDirs() throws Exception {
        TestUtil.writeDataToFile(TEST_FILE1, testData);
        TEST_DIR1.mkdirs();

        assertTrue(TEST_FILE1.exists());
        assertTrue(TEST_DIR1.exists());

        String fileNames = TEST_FILE1.getName() + "," + TEST_DIR1.getName();

        assertEquals(504, client.sendCommand("MMD5 " + fileNames));
    }

    private Map<String, String> parseReplyHash(String reply) {
        String s = reply.substring(4);
        s = s.trim();

        String[] tokens = s.split(",");

        Map<String, String> result = new HashMap<String, String>();
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i].trim();
            int hashStart = token.lastIndexOf(' ');

            String fileName = token.substring(0, hashStart).trim();
            if(fileName.startsWith("\"") && fileName.endsWith("\"")) {
            	fileName = fileName.substring(1, fileName.length() - 1);
            }
            String hash = token.substring(hashStart).trim();

            result.put(fileName, hash);
        }

        return result;
    }
}
