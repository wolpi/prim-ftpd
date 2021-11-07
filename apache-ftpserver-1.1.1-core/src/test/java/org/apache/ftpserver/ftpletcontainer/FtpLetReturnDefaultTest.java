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

package org.apache.ftpserver.ftpletcontainer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.ftp.FTPReply;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.clienttests.ClientTestTemplate;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.test.TestUtil;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class FtpLetReturnDefaultTest extends ClientTestTemplate {
    private static final byte[] TESTDATA = "TESTDATA".getBytes();

    private static final byte[] DOUBLE_TESTDATA = "TESTDATATESTDATA".getBytes();

    private static final File TEST_FILE1 = new File(ROOT_DIR, "test1.txt");

    private static final File TEST_FILE2 = new File(ROOT_DIR, "test2.txt");

    private static final File TEST_DIR1 = new File(ROOT_DIR, "dir1");

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.ftpserver.clienttests.ClientTestTemplate#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        MockFtplet.callback = new MockFtpletCallback();
        MockFtpletCallback.returnValue = FtpletResult.DEFAULT;

        initDirs();

        initServer();

        connectClient();

    }

    @Override
    protected FtpServerFactory createServer() throws Exception {
        FtpServerFactory server = super.createServer();

        Map<String, Ftplet> ftplets = new HashMap<String, Ftplet>();
        ftplets.put("f1", new MockFtplet());
        
        server.setFtplets(ftplets);
        
        return server;
    }

    public void testLogin() throws Exception {
    	final LinkedBlockingQueue<User> loggedInUser = new LinkedBlockingQueue<User>();
    	
        MockFtplet.callback = new MockFtpletCallback() {
            public FtpletResult onLogin(FtpSession session, FtpRequest request)
                    throws FtpException, IOException {
                loggedInUser.add(session.getUser());
                
                return super.onLogin(session, request);
            }

        };
        MockFtpletCallback.returnValue = FtpletResult.DEFAULT;

        assertTrue(client.login(ADMIN_USERNAME, ADMIN_PASSWORD));

        assertNotNull(loggedInUser.poll(2000, TimeUnit.MILLISECONDS));
    }

    public void testDelete() throws Exception {
        TestUtil.writeDataToFile(TEST_FILE1, TESTDATA);

        client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
        assertTrue(client.deleteFile(TEST_FILE1.getName()));
        assertFalse(TEST_FILE1.exists());
    }

    public void testMkdir() throws Exception {
        client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
        assertTrue(client.makeDirectory(TEST_DIR1.getName()));
        assertTrue(TEST_DIR1.exists());
    }

    public void testRmdir() throws Exception {
        TEST_DIR1.mkdirs();

        client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
        assertTrue(client.removeDirectory(TEST_DIR1.getName()));
        assertFalse(TEST_DIR1.exists());
    }

    public void testSite() throws Exception {
        client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
        assertTrue(FTPReply.isPositiveCompletion(client.site("HELP")));
    }

    public void testRename() throws Exception {
        TestUtil.writeDataToFile(TEST_FILE1, TESTDATA);

        client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
        assertTrue(client.rename(TEST_FILE1.getName(), TEST_FILE2.getName()));

        assertFalse(TEST_FILE1.exists());
        assertTrue(TEST_FILE2.exists());
    }

    public void testDownload() throws Exception {
        TestUtil.writeDataToFile(TEST_FILE1, TESTDATA);

        client.login(ADMIN_USERNAME, ADMIN_PASSWORD);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        client.retrieveFile(TEST_FILE1.getName(), baos);

        TestUtil.assertArraysEqual(TESTDATA, baos.toByteArray());
    }

    public void testAppend() throws Exception {
        TestUtil.writeDataToFile(TEST_FILE1, TESTDATA);

        client.login(ADMIN_USERNAME, ADMIN_PASSWORD);

        client.appendFile(TEST_FILE1.getName(), new ByteArrayInputStream(
                TESTDATA));

        TestUtil.assertFileEqual(DOUBLE_TESTDATA, TEST_FILE1);
    }

    public void testUpload() throws Exception {
        client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
        client.storeFile(TEST_FILE1.getName(), new ByteArrayInputStream(
                TESTDATA));

        TestUtil.assertFileEqual(TESTDATA, TEST_FILE1);
    }

    public void testUploadUnique() throws Exception {
        client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
        client.storeUniqueFile(new ByteArrayInputStream(TESTDATA));

        TestUtil.assertFileEqual(TESTDATA, ROOT_DIR.listFiles()[0]);
    }
}
