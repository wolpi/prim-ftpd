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
import java.io.IOException;

import junit.framework.TestCase;

import org.apache.commons.net.ProtocolCommandEvent;
import org.apache.commons.net.ProtocolCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.impl.DefaultFtpServer;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.test.TestUtil;
import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.util.IoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public abstract class ClientTestTemplate extends TestCase {

    private final Logger LOG = LoggerFactory
            .getLogger(ClientTestTemplate.class);

    protected static final String ADMIN_PASSWORD = "admin";

    protected static final String ADMIN_USERNAME = "admin";

    protected static final String ANONYMOUS_PASSWORD = "foo@bar.com";

    protected static final String ANONYMOUS_USERNAME = "anonymous";

    protected static final String TESTUSER2_USERNAME = "testuser2";

    protected static final String TESTUSER1_USERNAME = "testuser1";

    protected static final String TESTUSER_PASSWORD = "password";

    protected DefaultFtpServer server;

    protected FTPClient client;

    private static final File USERS_FILE = new File(TestUtil.getBaseDir(),
            "src/test/resources/users.properties");

    private static final File TEST_TMP_DIR = new File("test-tmp");

    protected static final File ROOT_DIR = new File(TEST_TMP_DIR, "ftproot");

    protected FtpServerFactory createServer() throws Exception {
        assertTrue(USERS_FILE.getAbsolutePath() + " must exist", USERS_FILE
                .exists());

        FtpServerFactory serverFactory = new FtpServerFactory();

        serverFactory.setConnectionConfig(createConnectionConfigFactory()
                .createConnectionConfig());

        ListenerFactory listenerFactory = new ListenerFactory();

        listenerFactory.setPort(0);

        listenerFactory
                .setDataConnectionConfiguration(createDataConnectionConfigurationFactory()
                        .createDataConnectionConfiguration());

        serverFactory.addListener("default", listenerFactory.createListener());

        PropertiesUserManagerFactory umFactory = new PropertiesUserManagerFactory();
        umFactory.setAdminName("admin");
        umFactory.setPasswordEncryptor(new ClearTextPasswordEncryptor());
        umFactory.setFile(USERS_FILE);

        serverFactory.setUserManager(umFactory.createUserManager());

        return serverFactory;
    }

    protected ConnectionConfigFactory createConnectionConfigFactory() {
        return new ConnectionConfigFactory();
    }

    protected DataConnectionConfigurationFactory createDataConnectionConfigurationFactory() {
        return new DataConnectionConfigurationFactory();
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        initDirs();

        initServer();

        connectClient();
    }

    /**
     * @throws IOException
     */
    protected void initDirs() throws IOException {
        cleanTmpDirs();

        TEST_TMP_DIR.mkdirs();
        ROOT_DIR.mkdirs();
    }

    /**
     * @throws IOException
     * @throws Exception
     */
    protected void initServer() throws IOException, Exception {
        // cast to internal class to get access to getters
        server = (DefaultFtpServer) createServer().createServer();

        if (isStartServer()) {
            server.start();
        }
    }

    protected int getListenerPort() {
        return server.getListener("default").getPort();
    }
    
    protected boolean isStartServer() {
        return true;
    }

    protected FTPClient createFTPClient() throws Exception {
        FTPClient client = new FTPClient();
        client.setDefaultTimeout(10000);
        return client;
    }

    /**
     * @throws Exception
     */
    protected void connectClient() throws Exception {
        client = createFTPClient();
        client.addProtocolCommandListener(new ProtocolCommandListener() {

            public void protocolCommandSent(ProtocolCommandEvent event) {
                LOG.debug("> " + event.getMessage().trim());

            }

            public void protocolReplyReceived(ProtocolCommandEvent event) {
                LOG.debug("< " + event.getMessage().trim());
            }
        });

        if (isConnectClient()) {
            doConnect();
        }
    }

    protected void doConnect() throws Exception {
        try {
            client.connect("localhost", getListenerPort());
        } catch (FTPConnectionClosedException e) {
            // try again
            Thread.sleep(200);
            client.connect("localhost", getListenerPort());
        }
    }

    protected boolean isConnectClient() {
        return true;
    }

    protected void cleanTmpDirs() throws IOException {
        if (TEST_TMP_DIR.exists()) {
            IoUtils.delete(TEST_TMP_DIR);
        }
    }

    protected FtpIoSession getActiveSession() {
        return server.getListener("default").getActiveSessions().iterator()
                .next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        if (isConnectClient()) {
            try {
                client.quit();
            } catch (Exception e) {
                // ignore
            }
        }

        if (server != null) {
        	try {
        		server.stop();
        	} catch(NullPointerException e) {
        		// a bug in the IBM JVM might cause Thread.interrupt() to throw an NPE
        		// see http://www-01.ibm.com/support/docview.wss?uid=swg1IZ52037&wv=1
        	}
        }

        cleanTmpDirs();
    }

}
