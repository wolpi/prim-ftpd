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
import java.util.regex.Pattern;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class SiteTest extends ClientTestTemplate {

    private static final String TEST_FILENAME = "test.txt";
    private static final byte[] TESTDATA = "TESTDATA".getBytes();
    
    private static final String TIMESTAMP_PATTERN = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}";

    public void testSiteDescUser() throws Exception {
        client.login(ADMIN_USERNAME, ADMIN_PASSWORD);

        client.sendCommand("SITE DESCUSER admin");
        String[] siteReplies = client.getReplyString().split("\r\n");

        assertEquals("200-", siteReplies[0]);
        assertEquals("userid          : admin", siteReplies[1]);
        assertEquals("userpassword    : ********", siteReplies[2]);
        assertEquals("homedirectory   : ./test-tmp/ftproot", siteReplies[3]);
        assertEquals("writepermission : true", siteReplies[4]);
        assertEquals("enableflag      : true", siteReplies[5]);
        assertEquals("idletime        : 0", siteReplies[6]);
        assertEquals("uploadrate      : 0", siteReplies[7]);
        assertEquals("200 downloadrate    : 0", siteReplies[8]);
    }

    public void testAnonNotAllowed() throws Exception {
        client.login(ANONYMOUS_USERNAME, ANONYMOUS_PASSWORD);

        assertTrue(FTPReply.isNegativePermanent(client.sendCommand("SITE DESCUSER admin")));
    }

    public void testSiteWho() throws Exception {
        client.login(ADMIN_USERNAME, ADMIN_PASSWORD);

        client.sendCommand("SITE WHO");
        String[] siteReplies = client.getReplyString().split("\r\n");

        assertEquals("200-", siteReplies[0]);
        String pattern = "200 admin           127.0.0.1       " + TIMESTAMP_PATTERN + " " + TIMESTAMP_PATTERN + " "; 
        
        assertTrue(Pattern.matches(pattern, siteReplies[1]));
    }

    public void testSiteStat() throws Exception {
        // reboot server to clear stats
        server.stop();
        initServer();
        
        // let's generate some stats
        FTPClient client1 = new FTPClient();
        client1.connect("localhost", getListenerPort());
        
        assertTrue(client1.login(ADMIN_USERNAME, ADMIN_PASSWORD));
        assertTrue(client1.makeDirectory("foo"));
        assertTrue(client1.makeDirectory("foo2"));
        assertTrue(client1.removeDirectory("foo2"));
        assertTrue(client1.storeFile(TEST_FILENAME, new ByteArrayInputStream(TESTDATA)));
        assertTrue(client1.storeFile(TEST_FILENAME, new ByteArrayInputStream(TESTDATA)));
        assertTrue(client1.retrieveFile(TEST_FILENAME, new ByteArrayOutputStream()));
        assertTrue(client1.deleteFile(TEST_FILENAME));
        
        assertTrue(client1.logout());
        client1.disconnect();

        FTPClient client2 = new FTPClient();
        client2.connect("localhost", getListenerPort());

        assertTrue(client2.login(ANONYMOUS_USERNAME, ANONYMOUS_PASSWORD));
        // done setting up stats
        
        // send a command to verify that we are correctly logged in
        assertTrue(FTPReply.isPositiveCompletion(client2.noop()));
        
        client.connect("localhost", getListenerPort());
        assertTrue(client.login(ADMIN_USERNAME, ADMIN_PASSWORD));

        if(server.getServerContext().getFtpStatistics().getCurrentLoginNumber() != 2) {
            // wait until both clients have been logged in
            Thread.sleep(2000);
            if(server.getServerContext().getFtpStatistics().getCurrentLoginNumber() != 2) {
                Thread.sleep(5000);
            }
        }
        
        client.sendCommand("SITE STAT");
        String[] siteReplies = client.getReplyString().split("\r\n");

        assertEquals("200-", siteReplies[0]);
        
        String pattern = "Start Time               : " + TIMESTAMP_PATTERN; 
        assertTrue(siteReplies[1], Pattern.matches(pattern, siteReplies[1]));
        assertTrue(siteReplies[2], Pattern.matches("File Upload Number       : 2", siteReplies[2]));
        assertTrue(siteReplies[3], Pattern.matches("File Download Number     : 1", siteReplies[3]));
        assertTrue(siteReplies[4], Pattern.matches("File Delete Number       : 1", siteReplies[4]));
        assertTrue(siteReplies[5], Pattern.matches("File Upload Bytes        : 16", siteReplies[5]));
        assertTrue(siteReplies[6], Pattern.matches("File Download Bytes      : 8", siteReplies[6]));
        assertTrue(siteReplies[7], Pattern.matches("Directory Create Number  : 2", siteReplies[7]));
        assertTrue(siteReplies[8], Pattern.matches("Directory Remove Number  : 1", siteReplies[8]));
        //assertTrue(siteReplies[9], Pattern.matches("Current Logins           : 2", siteReplies[9]));
        assertTrue(siteReplies[10], Pattern.matches("Total Logins             : 3", siteReplies[10]));
        assertTrue(siteReplies[11], Pattern.matches("Current Anonymous Logins : 1", siteReplies[11]));
        assertTrue(siteReplies[12], Pattern.matches("Total Anonymous Logins   : 1", siteReplies[12]));
        assertTrue(siteReplies[13], Pattern.matches("Current Connections      : 2", siteReplies[13]));
        assertTrue(siteReplies[14], Pattern.matches("200 Total Connections        : 3", siteReplies[14]));
    }

    
}
