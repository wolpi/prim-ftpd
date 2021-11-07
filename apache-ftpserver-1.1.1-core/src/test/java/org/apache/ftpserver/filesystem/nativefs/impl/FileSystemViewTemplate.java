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

import junit.framework.TestCase;

import org.apache.ftpserver.usermanager.impl.BaseUser;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public abstract class FileSystemViewTemplate extends TestCase {

    protected static final String DIR1_NAME = "dir1";

    protected BaseUser user = new BaseUser();

    public void testChangeDirectory() throws Exception {
        NativeFileSystemView view = new NativeFileSystemView(user);
        assertEquals("/", view.getWorkingDirectory().getAbsolutePath());

        assertTrue(view.changeWorkingDirectory(DIR1_NAME));
        assertEquals("/" + DIR1_NAME, view.getWorkingDirectory().getAbsolutePath());

        assertTrue(view.changeWorkingDirectory("."));
        assertEquals("/" + DIR1_NAME, view.getWorkingDirectory().getAbsolutePath());

        assertTrue(view.changeWorkingDirectory(".."));
        assertEquals("/", view.getWorkingDirectory().getAbsolutePath());

        assertTrue(view.changeWorkingDirectory("./" + DIR1_NAME));
        assertEquals("/" + DIR1_NAME, view.getWorkingDirectory().getAbsolutePath());

        assertTrue(view.changeWorkingDirectory("~"));
        assertEquals("/", view.getWorkingDirectory().getAbsolutePath());
    }

    public void testChangeDirectoryCaseInsensitive() throws Exception {
        NativeFileSystemView view = new NativeFileSystemView(user, true);
        assertEquals("/", view.getWorkingDirectory().getAbsolutePath());

        assertTrue(view.changeWorkingDirectory("/DIR1"));
        assertEquals("/dir1", view.getWorkingDirectory().getAbsolutePath());
        assertTrue(view.getWorkingDirectory().doesExist());

        assertTrue(view.changeWorkingDirectory("/dir1"));
        assertEquals("/dir1", view.getWorkingDirectory().getAbsolutePath());
        assertTrue(view.getWorkingDirectory().doesExist());

        assertTrue(view.changeWorkingDirectory("/DiR1"));
        assertEquals("/dir1", view.getWorkingDirectory().getAbsolutePath());
        assertTrue(view.getWorkingDirectory().doesExist());
    }

}