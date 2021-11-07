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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.ftpserver.test.TestUtil;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class I18NTest extends ClientTestTemplate {

    private static final String TESTDATA = "TESTDATA";

    private static final String ENCODING = "UTF-8";

    private static byte[] testData = null;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.ftpserver.clienttests.ClientTestTemplate#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        testData = TESTDATA.getBytes(ENCODING);

        client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    @Override
    protected FTPClient createFTPClient() throws Exception {
        FTPClient client = new FTPClient();
        client.setControlEncoding("UTF-8");
        return client;
    }

    public void testStoreWithUTF8FileName() throws Exception {

        String oddFileName = "����";
        File testFile = new File(ROOT_DIR, oddFileName);

        assertTrue(client.storeFile(oddFileName, new ByteArrayInputStream(
                testData)));

        assertTrue(testFile.exists());

        TestUtil.assertFileEqual(testData, testFile);
    }

    public void testRetrieveWithUTF8FileName() throws Exception {

        String oddFileName = "����";
        File testFile = new File(ROOT_DIR, oddFileName);
        TestUtil.writeDataToFile(testFile, testData);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        assertTrue(client.retrieveFile(testFile.getName(), baos));

        TestUtil.assertArraysEqual(testData, baos.toByteArray());
    }
}
