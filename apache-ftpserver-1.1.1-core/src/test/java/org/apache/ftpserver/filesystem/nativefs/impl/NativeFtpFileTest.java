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

package org.apache.ftpserver.filesystem.nativefs.impl;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.util.IoUtils;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class NativeFtpFileTest extends FtpFileTestTemplate {

    private static final File TEST_TMP_DIR = new File("test-tmp");

    private static final File ROOT_DIR = new File(TEST_TMP_DIR, "ftproot");

    private static final File TEST_DIR1 = new File(ROOT_DIR, "dir1");

    private static final File TEST_FILE1 = new File(ROOT_DIR, "file1");

    private static final File TEST_FILE2_IN_DIR1 = new File(TEST_DIR1, "file2");

    private static final File TEST_FILE3 = new File(ROOT_DIR, "file3");

    private static final Map<String, File> FILE_MAPPINGS = new HashMap<String, File>();

    static {
        FILE_MAPPINGS.put("/", ROOT_DIR);
        FILE_MAPPINGS.put(FILE2_PATH, TEST_FILE2_IN_DIR1);
        FILE_MAPPINGS.put(DIR1_PATH, TEST_DIR1);
        FILE_MAPPINGS.put(FILE1_PATH, TEST_FILE1);
        FILE_MAPPINGS.put(FILE2_PATH, TEST_FILE3);
        FILE_MAPPINGS.put(DIR1_WITH_SLASH_PATH, TEST_DIR1);
        FILE_MAPPINGS.put(" \t", TEST_FILE2_IN_DIR1);
    }




    @Override
    protected void setUp() throws Exception {
        initDirs();

        TEST_DIR1.mkdirs();
        TEST_FILE1.createNewFile();
        TEST_FILE2_IN_DIR1.createNewFile();
        TEST_FILE3.createNewFile();
    }

    @Override
    protected FtpFile createFileObject(String fileName, User user) {
        return new NativeFtpFile(fileName, FILE_MAPPINGS.get(fileName), user);
    }

    public void testConstructorWithNullFile() {
        try {
            new NativeFtpFile("foo", null, USER);
            fail("Must throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }
    public void testDeleteReadOnlyFiles(){
    	
    	NativeFtpFile fileObj=(NativeFtpFile)createFileObject(FILE2_PATH, USER);
    	File physicalFile=fileObj.getPhysicalFile();
    	// First check
    	assertTrue(fileObj.isRemovable());
    	
    	// Now file is read only.
    	if (!physicalFile.setReadOnly() ){
    		fail("Test cannot be setup properly");   		
    	}
    
    	assertTrue(fileObj.isRemovable());
    	//can we actually delete this file?
    	assertTrue(physicalFile.delete());
    }

    @Override
    protected void tearDown() throws Exception {
        cleanTmpDirs();
    }

    /**
     * @throws IOException
     */
    protected void initDirs() throws IOException {
        cleanTmpDirs();

        TEST_TMP_DIR.mkdirs();
        ROOT_DIR.mkdirs();
    }

    protected void cleanTmpDirs() throws IOException {
        if (TEST_TMP_DIR.exists()) {
            IoUtils.delete(TEST_TMP_DIR);
        }
    }

}
