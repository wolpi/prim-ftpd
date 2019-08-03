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

import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.util.IoUtils;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class NativeFileSystemViewTest extends FileSystemViewTemplate {

    private static final File TEST_TMP_DIR = new File("test-tmp");

    private static final File ROOT_DIR = new File(TEST_TMP_DIR, "ftproot");

    private static final File TEST_DIR1 = new File(ROOT_DIR, DIR1_NAME);

    private static final File TEST_FILE2_IN_DIR1 = new File(TEST_DIR1, "file2");
    
    private static final String ROOT_DIR_PATH = ROOT_DIR.getAbsolutePath()
            .replace(File.separatorChar, '/');

    private static final String FULL_PATH = ROOT_DIR_PATH + "/"
            + TEST_DIR1.getName() + "/" + TEST_FILE2_IN_DIR1.getName();

    private static final String FULL_PATH_NO_CURRDIR = ROOT_DIR_PATH + "/"
            + TEST_FILE2_IN_DIR1.getName();

    @Override
    protected void setUp() throws Exception {
        initDirs();

        TEST_DIR1.mkdirs();
        TEST_FILE2_IN_DIR1.createNewFile();

        user.setHomeDirectory(ROOT_DIR.getAbsolutePath());
    }

    public void testConstructor() throws FtpException {
        NativeFileSystemView view = new NativeFileSystemView(user);
        assertEquals("/", view.getWorkingDirectory().getAbsolutePath());
    }

    public void testConstructorWithNullUser() throws FtpException {
        try {
            new NativeFileSystemView(null);
            fail("Must throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    public void testConstructorWithNullHomeDir() throws FtpException {
        user.setHomeDirectory(null);
        try {
            new NativeFileSystemView(user);
            fail("Must throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    public void testGetPhysicalName() throws FtpException {
        NativeFileSystemView view = new NativeFileSystemView(user);

        assertEquals(FULL_PATH, view.getPhysicalName(ROOT_DIR_PATH + "/", "/"
                + TEST_DIR1.getName() + "/", TEST_FILE2_IN_DIR1.getName(), false));
        
        assertEquals("No trailing slash on rootDir", FULL_PATH, view
                .getPhysicalName(ROOT_DIR_PATH,
                        "/" + TEST_DIR1.getName() + "/", TEST_FILE2_IN_DIR1
                                .getName(), false));
        
        assertEquals("No leading slash on currDir", FULL_PATH,
                view.getPhysicalName(ROOT_DIR_PATH + "/", TEST_DIR1
                        .getName()
                        + "/", TEST_FILE2_IN_DIR1.getName(), false));
        assertEquals("No trailing slash on currDir", FULL_PATH,
                view.getPhysicalName(ROOT_DIR_PATH + "/", "/"
                        + TEST_DIR1.getName(), TEST_FILE2_IN_DIR1.getName(), false));
        assertEquals("No slashes on currDir", FULL_PATH, view
                .getPhysicalName(ROOT_DIR_PATH + "/", TEST_DIR1.getName(),
                        TEST_FILE2_IN_DIR1.getName(), false));
        assertEquals("Backslashes in rootDir", FULL_PATH, view
                .getPhysicalName(ROOT_DIR.getAbsolutePath() + "/", "/"
                        + TEST_DIR1.getName() + "/", TEST_FILE2_IN_DIR1
                        .getName(), false));
        assertEquals("Null currDir", FULL_PATH_NO_CURRDIR, view
                .getPhysicalName(ROOT_DIR.getAbsolutePath() + "/", null,
                        TEST_FILE2_IN_DIR1.getName(), false));
        assertEquals("Empty currDir", FULL_PATH_NO_CURRDIR, view
                .getPhysicalName(ROOT_DIR.getAbsolutePath() + "/", "",
                        TEST_FILE2_IN_DIR1.getName(), false));
        assertEquals("Absolute fileName in root", FULL_PATH_NO_CURRDIR,
                view
                        .getPhysicalName(ROOT_DIR.getAbsolutePath() + "/",
                                TEST_DIR1.getName(), "/"
                                        + TEST_FILE2_IN_DIR1.getName(), false));
        assertEquals("Absolute fileName in dir1", FULL_PATH, view
                .getPhysicalName(ROOT_DIR.getAbsolutePath() + "/", null, "/"
                        + TEST_DIR1.getName() + "/"
                        + TEST_FILE2_IN_DIR1.getName(), false));

        assertEquals(". in currDir", FULL_PATH, view.getPhysicalName(
                ROOT_DIR.getAbsolutePath(), TEST_DIR1.getName() + "/./", "/"
                        + TEST_DIR1.getName() + "/"
                        + TEST_FILE2_IN_DIR1.getName(), false));

    }

    public void testGetPhysicalNameWithRelative() throws FtpException {
        NativeFileSystemView view = new NativeFileSystemView(user);
        
        assertEquals(".. in fileName", FULL_PATH_NO_CURRDIR, view
                .getPhysicalName(ROOT_DIR.getAbsolutePath(), TEST_DIR1
                        .getName(), "/../" + TEST_FILE2_IN_DIR1.getName(), false));
        assertEquals(".. beyond rootDir", FULL_PATH_NO_CURRDIR, view
                .getPhysicalName(ROOT_DIR.getAbsolutePath(), TEST_DIR1
                        .getName(), "/../../" + TEST_FILE2_IN_DIR1.getName(), false));
    }

    public void testGetPhysicalNameWithTilde() throws FtpException {
        NativeFileSystemView view = new NativeFileSystemView(user);
        
        assertEquals(FULL_PATH_NO_CURRDIR, view.getPhysicalName(
                ROOT_DIR.getAbsolutePath(), TEST_DIR1.getName(), "/~/"
                        + TEST_FILE2_IN_DIR1.getName(), false));
    }

    public void testGetPhysicalNameCaseInsensitive() throws FtpException {
        NativeFileSystemView view = new NativeFileSystemView(user);
        
        assertEquals(FULL_PATH, view.getPhysicalName(ROOT_DIR
                .getAbsolutePath(), TEST_DIR1.getName(), TEST_FILE2_IN_DIR1
                .getName().toUpperCase(), true));

    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#tearDown()
     */
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
