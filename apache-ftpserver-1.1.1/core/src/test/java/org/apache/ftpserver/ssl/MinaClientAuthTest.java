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

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;

import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.ftpserver.impl.FtpIoSession;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class MinaClientAuthTest extends SSLTestTemplate {

    @Override
    protected FTPSClient createFTPClient() throws Exception {
        FTPSClient client = new FTPSClient(useImplicit());
        client.setNeedClientAuth(true);

        KeyStore ks = KeyStore.getInstance("JKS");
        FileInputStream fis = new FileInputStream(FTPCLIENT_KEYSTORE);
        ks.load(fis, KEYSTORE_PASSWORD.toCharArray());
        fis.close();

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory
                .getDefaultAlgorithm());
        kmf.init(ks, KEYSTORE_PASSWORD.toCharArray());

        client.setKeyManager(kmf.getKeyManagers()[0]);

        return client;
    }

    @Override
    protected String getAuthValue() {
        return "TLS";
    }

    @Override
    protected String getClientAuth() {
        return "true";
    }

    public void testCommandChannel() throws Exception {
        assertTrue(client.login(ADMIN_USERNAME, ADMIN_PASSWORD));
        assertTrue(FTPReply.isPositiveCompletion(client.noop()));
    }

    public void testClientCertificates() throws Exception {
        FtpIoSession session = server.getListener("default")
                .getActiveSessions().iterator().next();
        assertEquals(1, session.getClientCertificates().length);

        X509Certificate cert = (X509Certificate) session
                .getClientCertificates()[0];

        assertTrue(cert.getSubjectDN().toString().contains("FtpClient"));
    }

}
