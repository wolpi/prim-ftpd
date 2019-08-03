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
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class MFMTTest extends ClientTestTemplate {
    private static final File TEST_FILE1 = new File(ROOT_DIR, "test1.txt");

    private static final File TEST_DIR1 = new File(ROOT_DIR, "dir1");

    private static final File TEST_FILE_IN_DIR1 = new File(TEST_DIR1,
            "test4.txt");
    private static final File TEST_FILE_BLANK_SPACES=new File(ROOT_DIR,"my test.txt");
    

    private static final Calendar EXPECTED_TIME = new GregorianCalendar(2002, 6, 17, 21, 7, 15);        
    static {
        EXPECTED_TIME.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        assertTrue(TEST_FILE1.createNewFile());
        assertTrue(TEST_DIR1.mkdir());
        assertTrue(TEST_FILE_IN_DIR1.createNewFile());
        assertTrue(TEST_FILE_BLANK_SPACES.createNewFile());
        
        client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
    }
   
    public void testNoArgument() throws Exception {
        // must return 500 or 501 errors
        assertEquals(501, client.sendCommand("MFMT"));
    }

    public void testSingleArgument() throws Exception {
        // must return 500 or 501 errors
        assertEquals(501, client.sendCommand("MFMT", "20020717210715 "));
    }

    public void testNoManyArguments() throws Exception {
        // must return 550 errors as a filename can contain blank spaces
        assertEquals(550, client.sendCommand("MFMT", "20020717210715 test1.txt too many"));
    }
    
    public void testNonTimestampArgument() throws Exception {
        // must return 500 or 501 errors
        assertEquals(501, client.sendCommand("MFMT", "incorrect test1.txt"));
    }

    public void testNegativeTimestamp() throws Exception {
        // must return 500 or 501 errors
        assertEquals(501, client.sendCommand("MFMT", "-20020717210715 test1.txt"));
    }

    public void testIncorrectTimestamp() throws Exception {
        // must return 500 or 501 errors
        assertEquals(501, client.sendCommand("MFMT", "20021317210715 test1.txt"));
    }

    public void testNotCompleteTimestamp() throws Exception {
        // must return 500 or 501 errors
        assertEquals(501, client.sendCommand("MFMT", "20021317 test1.txt"));
    }

    
    public void testNonExistingFile() throws Exception {
        // should return 550
        assertEquals(550, client.sendCommand("MFMT", "20020717210715 dummy.txt"));
    }

    public void testDirectory() throws Exception {
        // should return 550
        assertEquals(501, client.sendCommand("MFMT", "20020717210715 " + TEST_DIR1.getName()));
    }

    
    public void testFeatures() throws Exception {
        client.sendCommand("FEAT");
        
        String result = client.getReplyString();
        
        assertTrue(result.contains(" MFMT\r\n"));
    }

    public void testSetTime() throws Exception {
        
        assertEquals(213, client.sendCommand("MFMT", "20020717210715 test1.txt"));
        
        assertEquals(EXPECTED_TIME.getTimeInMillis(),TEST_FILE1.lastModified());
    }
    
    public void testSetTimeFullPath() throws Exception {
        assertEquals(213, client.sendCommand("MFMT", "20020717210715 dir1/test4.txt"));
        
        assertEquals(EXPECTED_TIME.getTimeInMillis(), TEST_FILE_IN_DIR1.lastModified());
    }

    public void testSetTimeFileWithSpaces() throws Exception{
        assertEquals(213, client.sendCommand("MFMT", "20020717210715 my test.txt"));
        
        assertEquals(EXPECTED_TIME.getTimeInMillis(),TEST_FILE_BLANK_SPACES.lastModified());
    }
    
}
