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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.ftp.FTPSSocketFactory;
import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.impl.ServerDataConnectionFactory;

/**
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class MinaImplicitDataChannelTest extends ImplicitSecurityTestTemplate {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected String getAuthValue() {
        return "SSL";
    }

    @Override
    protected DataConnectionConfigurationFactory createDataConnectionConfigurationFactory() {
        DataConnectionConfigurationFactory result = super
                .createDataConnectionConfigurationFactory();
        result.setImplicitSsl(true);
        return result;
    }

    @Override
    protected boolean useImplicit() {
        return true;
    }

    protected boolean expectDataConnectionSecure() {
        return true;
    }

    /**
     * Simple test that the {@link ServerDataConnectionFactory#isSecure()} 
     * works as expected
     */
    public void testThatDataChannelIsSecure() {
        assertTrue(getActiveSession().getDataConnection().isSecure());
    }

    /**
     * Test that implicit SSL data connections works with clients that
     * use implicit SSL for the data connection, without sending PROT P. 
     * In this case in active mode.
     * 
     * The inherited tests from {@link ExplicitSecurityTestTemplate} ensures that 
     * data transfers work when using PROT P
     */
    public void testStoreWithoutProtPInActiveMode() throws Exception {
        secureClientDataConnection();

        // Do not send PROT P
        
        // make sure we use a implicit SSL data connection
        assertTrue(getActiveSession().getDataConnection().isSecure());

        client.storeFile(TEST_FILE1.getName(), new ByteArrayInputStream(
                TEST_DATA));

        assertTrue(TEST_FILE1.exists());
        assertEquals(TEST_DATA.length, TEST_FILE1.length());
    }

    /**
     * Test that implicit SSL data connections works with clients that
     * use implicit SSL for the data connection, without sending PROT P. 
     * In this case in active mode.
     */
    @Override
    public void testStoreWithProtPInPassiveMode() throws Exception {
        secureClientDataConnection();
        client.enterLocalPassiveMode();

        // Do not send PROT P
        
        // make sure we use a implicit SSL data connection
        assertTrue(getActiveSession().getDataConnection().isSecure());

        client.storeFile(TEST_FILE1.getName(), new ByteArrayInputStream(
                TEST_DATA));

        assertTrue(TEST_FILE1.exists());
        assertEquals(TEST_DATA.length, TEST_FILE1.length());
    }

    
    private void secureClientDataConnection() throws NoSuchAlgorithmException,
            KeyManagementException {

        // FTPSClient does not support implicit data connections, so we hack it ourselves
        FTPSClient sclient = (FTPSClient) client;
        SSLContext context = SSLContext.getInstance("TLS");

        // these are the same key and trust managers that we initialize the client with
        context.init(new KeyManager[] { clientKeyManager },
                new TrustManager[] { clientTrustManager }, null);
        sclient.setSocketFactory(new FTPSSocketFactory(context));
        SSLServerSocketFactory ssf = context.getServerSocketFactory();
        sclient.setServerSocketFactory(ssf);

        // FTPClient should not use SSL secured sockets for the data connection 
    }
}
