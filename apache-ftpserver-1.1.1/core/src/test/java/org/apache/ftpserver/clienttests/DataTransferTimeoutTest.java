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
import java.io.OutputStream;

import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.listener.ListenerFactory;

/**
*
* Test for FTPSERVER-170
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class DataTransferTimeoutTest extends ClientTestTemplate {
    private static final String TEST_FILENAME = "test.txt";
    
    private static final File TEST_FILE = new File(ROOT_DIR, TEST_FILENAME);

    @Override
    protected FtpServerFactory createServer() throws Exception {
        FtpServerFactory serverFactory = super.createServer();
        
        // set a really short timeout
        ListenerFactory listenerFactory = new ListenerFactory(serverFactory.getListener("default"));
        listenerFactory.setIdleTimeout(1);
        
        serverFactory.addListener("default", listenerFactory.createListener());
        
        return serverFactory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.ftpserver.clienttests.ClientTestTemplate#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    public void testTimeoutForStore() throws Exception {
        OutputStream os = client.storeFileStream(TEST_FILENAME);
        
        os.write(1);
        
        // make sure this take longer than the timeout time, but not timeout between writes...
        for(int i = 0; i<100; i++) {
            Thread.sleep(20);
            os.write(1);   
            os.flush();
        }
        
        os.close();

        client.completePendingCommand();
        
        // we should not have been disconnected
        client.noop();
    }

    /*
     * Disabled for now, test is not stable on Solaris
    
    public void testTimeoutForRetreive() throws Exception {
        // as used by IODataConnection
        int bufferSize = 4096 * 10;
        byte[] buffer = new byte[bufferSize];
        
        byte[] testData = new byte[200 * bufferSize];
        Arrays.fill(testData, (byte)1);
        
        TestUtil.writeDataToFile(TEST_FILE, testData);
        InputStream is = client.retrieveFileStream(TEST_FILENAME);

        // read ten buffer sizes at a time, trying to trigger IODataConnection to update
        // the session timeout for each read
        for(int i = 0; i<100; i++) {
            long startTime = System.currentTimeMillis();
            Thread.sleep(20);
            is.read(buffer);
            
            if((System.currentTimeMillis() - startTime) > 500 ) {
                fail("Read took to long, test not safe");
            }
        }
        
        is.close();

        client.completePendingCommand();
        
        // we should not have been disconnected
        client.noop();
    }
*/
}
