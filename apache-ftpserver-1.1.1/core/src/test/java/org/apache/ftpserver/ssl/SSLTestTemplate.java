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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.net.ftp.FTPSClient;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.clienttests.ClientTestTemplate;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.test.TestUtil;
import org.apache.ftpserver.util.IoUtils;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public abstract class SSLTestTemplate extends ClientTestTemplate {

    protected static final File FTPCLIENT_KEYSTORE = new File(TestUtil
            .getBaseDir(), "src/test/resources/client.jks");

    protected static final String KEYSTORE_PASSWORD = "password";

    private static final File FTPSERVER_KEYSTORE = new File(TestUtil
            .getBaseDir(), "src/test/resources/ftpserver.jks");

    protected KeyManager clientKeyManager;
    protected TrustManager clientTrustManager;

    protected SslConfigurationFactory createSslConfiguration() {
        SslConfigurationFactory sslConfigFactory = new SslConfigurationFactory();
        sslConfigFactory.setKeystoreFile(FTPSERVER_KEYSTORE);
        sslConfigFactory.setKeystorePassword(KEYSTORE_PASSWORD);
        sslConfigFactory.setSslProtocol(getAuthValue());
        sslConfigFactory.setClientAuthentication(getClientAuth());
        sslConfigFactory.setKeyPassword(KEYSTORE_PASSWORD);

        return sslConfigFactory;
    }
    
    @Override
    protected FtpServerFactory createServer() throws Exception {
        assertTrue(FTPSERVER_KEYSTORE.exists());

        FtpServerFactory server = super.createServer();
        ListenerFactory factory = new ListenerFactory(server.getListener("default"));
        
        factory.setImplicitSsl(useImplicit());

        factory.setSslConfiguration(createSslConfiguration().createSslConfiguration());
        
        server.addListener("default", factory.createListener());
        
        return server;
    }
    
    protected boolean useImplicit() {
        return false;
    }

    protected String getClientAuth() {
        return "false";
    }

    @Override
    protected FTPSClient createFTPClient() throws Exception {
        FTPSClient ftpsClient = new FTPSClient(useImplicit());

        FileInputStream fin = new FileInputStream(FTPCLIENT_KEYSTORE);
        KeyStore store = KeyStore.getInstance("jks");
        store.load(fin, KEYSTORE_PASSWORD.toCharArray());
        fin.close();

        // initialize key manager factory
        KeyManagerFactory keyManagerFactory = KeyManagerFactory
                .getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(store, KEYSTORE_PASSWORD.toCharArray());

        // initialize trust manager factory
        TrustManagerFactory trustManagerFactory = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());

        trustManagerFactory.init(store);
        
        clientKeyManager = keyManagerFactory.getKeyManagers()[0];
        clientTrustManager = trustManagerFactory.getTrustManagers()[0];
        
        ftpsClient.setKeyManager(clientKeyManager);
        ftpsClient.setTrustManager(clientTrustManager);

        
        String auth = getAuthValue();
        if (auth != null) {
            ftpsClient.setAuthValue(auth);
            
            if(auth.equals("SSL")) {
                // SSLv3 is disabled by default on the JBM JDK, therefore we need to enable it
                ftpsClient.setEnabledProtocols(new String[]{"SSLv3"});
            }
        }
        return ftpsClient;
    }

    protected abstract String getAuthValue();

    protected void writeDataToFile(File file, byte[] data) throws IOException {
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(file);

            fos.write(data);
        } finally {
            IoUtils.close(fos);
        }
    }

}
