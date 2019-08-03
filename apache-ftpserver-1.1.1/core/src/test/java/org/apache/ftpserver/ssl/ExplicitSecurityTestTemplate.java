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

package org.apache.ftpserver.ssl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.security.Security;

import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.ftpserver.util.IoUtils;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public abstract class ExplicitSecurityTestTemplate extends SSLTestTemplate {

    protected static final File TEST_FILE1 = new File(ROOT_DIR, "test1.txt");

    protected static final File TEST_FILE2 = new File(ROOT_DIR, "test2.txt");

    protected static final byte[] TEST_DATA = "TESTDATA".getBytes();
    
    // Enabling SSLv3 because Java 8 disable it by default...
    static
    {
        Security.setProperty( "jdk.tls.disabledAlgorithms", "RC4, MD5withRSA, DH keySize < 768" );
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    protected boolean expectDataConnectionSecure() {
        return getAuthValue().equals("SSL") && !useImplicit();
    }

    /**
     * Tests that we can send command over the command channel. This is, in fact
     * already tested by login in setup but an explicit test is good anyways.
     */
    public void testCommandChannel() throws Exception {
        assertTrue(getActiveSession().isSecure());

        assertEquals(expectDataConnectionSecure(), getActiveSession().getDataConnection().isSecure());

        assertTrue(FTPReply.isPositiveCompletion(client.noop()));
    }

    public void testReissueAuth() throws Exception {
        assertTrue(getActiveSession().isSecure());
        assertTrue(FTPReply.isPositiveCompletion(client.noop()));

        // we do not accept reissued AUTH or AUTH on implicitly secured socket
        assertEquals(534, client.sendCommand("AUTH SSL"));
    }

    public void testIsSecure() {
        assertTrue(getActiveSession().isSecure());
    }

    public void testStoreWithProtPInPassiveMode() throws Exception {
        client.setRemoteVerificationEnabled(false);
        client.enterLocalPassiveMode();

        ((FTPSClient) client).execPROT("P");

        assertTrue(getActiveSession().getDataConnection().isSecure());

        client.storeFile(TEST_FILE1.getName(), new ByteArrayInputStream(
                TEST_DATA));

        assertTrue(TEST_FILE1.exists());
        assertEquals(TEST_DATA.length, TEST_FILE1.length());
    }

    public void testStoreWithProtPAndReturnToProtCInPassiveMode()
            throws Exception {
        client.setRemoteVerificationEnabled(false);
        client.enterLocalPassiveMode();

        ((FTPSClient) client).execPROT("P");

        assertTrue(getActiveSession().getDataConnection().isSecure());

        client.storeFile(TEST_FILE1.getName(), new ByteArrayInputStream(
                TEST_DATA));

        assertTrue(TEST_FILE1.exists());
        assertEquals(TEST_DATA.length, TEST_FILE1.length());

        ((FTPSClient) client).execPROT("C");

        assertFalse(getActiveSession().getDataConnection().isSecure());

        client.storeFile(TEST_FILE2.getName(), new ByteArrayInputStream(
                TEST_DATA));

        assertTrue(TEST_FILE2.exists());
        assertEquals(TEST_DATA.length, TEST_FILE2.length());
    }

    public void testStoreWithProtPInActiveMode() throws Exception {
        client.setRemoteVerificationEnabled(false);
        
        ((FTPSClient) client).execPROT("P");
        assertTrue(getActiveSession().getDataConnection().isSecure());

        client.storeFile(TEST_FILE1.getName(), new ByteArrayInputStream(
                TEST_DATA));

        assertTrue(TEST_FILE1.exists());
        assertEquals(TEST_DATA.length, TEST_FILE1.length());
    }

    public void testStoreWithProtPAndReturnToProtCInActiveMode()
            throws Exception {
        ((FTPSClient) client).execPROT("P");
        assertTrue(getActiveSession().getDataConnection().isSecure());

        client.storeFile(TEST_FILE1.getName(), new ByteArrayInputStream(
                TEST_DATA));

        assertTrue(TEST_FILE1.exists());
        assertEquals(TEST_DATA.length, TEST_FILE1.length());

        // needed due to bug in commons-net
        client.setServerSocketFactory(null);

        ((FTPSClient) client).execPROT("C");

        client.storeFile(TEST_FILE2.getName(), new ByteArrayInputStream(
                TEST_DATA));

        assertTrue(TEST_FILE2.exists());
        assertEquals(TEST_DATA.length, TEST_FILE2.length());
    }

    public void testListEmptyDir() throws Exception {
        client.enterLocalPassiveMode();

        ((FTPSClient) client).execPROT("P");
        assertTrue(getActiveSession().getDataConnection().isSecure());

        File dir = new File(ROOT_DIR, "dir");
        dir.mkdir();

        client.listFiles(dir.getName());
    }

    public void testReceiveEmptyFile() throws Exception {
        client.enterLocalPassiveMode();

        ((FTPSClient) client).execPROT("P");
        assertTrue(getActiveSession().getDataConnection().isSecure());

        File file = new File(ROOT_DIR, "foo");
        file.createNewFile();

        InputStream is = null;
        try {
            is = client.retrieveFileStream(file.getName());
            assertEquals(-1, is.read(new byte[1024]));
        } finally {
            IoUtils.close(is);
        }
    }
}
