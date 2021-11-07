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

import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.net.ftp.FTPSClient;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class MinaCipherSuitesTest extends SSLTestTemplate {

    @Override
    protected String getAuthValue() {
        return "TLS";
    }

    @Override
    protected boolean useImplicit() {
        return true;
    }

    @Override
    protected SslConfigurationFactory createSslConfiguration() {
        SslConfigurationFactory sslConfigFactory = super.createSslConfiguration();

        sslConfigFactory
        .setEnabledCipherSuites(new String[] { "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA" });

        return sslConfigFactory;
    }
    
    @Override
    protected FTPSClient createFTPClient() throws Exception {
        return new FTPSClient(true);
    }

    @Override
    protected boolean isConnectClient() {
        return false;
    }

    /*
     * Only certain cipher suites will work with the keys and protocol we're
     * using for this test. Two suites known to work is:
     * SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA
     */
    public void testEnabled() throws Exception {

        ((FTPSClient) client)
                .setEnabledCipherSuites(new String[] { "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA" });

        connectClient();
    }

    public void testDisabled() throws Exception {
        ((FTPSClient) client)
                .setEnabledCipherSuites(new String[] { "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA" });

        try {
            doConnect();
            fail("Must throw SSLHandshakeException");
        } catch (SSLHandshakeException e) {
            // OK
        }
    }
}
