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

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class CdTest extends ClientTestTemplate {
    protected static final File TEST_DIR1 = new File(ROOT_DIR, "dir1");

    protected static final File TEST_DIR2 = new File(ROOT_DIR, "dir2");

    protected static final File TEST_DIR_WITH_LEADING_SPACE = new File(
            ROOT_DIR, " leadingspace");

    protected static final File TEST_DIR_IN_DIR1 = new File(TEST_DIR1, "dir3");

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.ftpserver.clienttests.ClientTestTemplate#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        TEST_DIR1.mkdirs();
        TEST_DIR2.mkdirs();
        TEST_DIR_WITH_LEADING_SPACE.mkdirs();
        TEST_DIR_IN_DIR1.mkdirs();
        assertTrue(TEST_DIR1.exists());
        assertTrue(TEST_DIR2.exists());
        assertTrue(TEST_DIR_WITH_LEADING_SPACE.exists());
        assertTrue(TEST_DIR_IN_DIR1.exists());

        client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    public void testCWD() throws Exception {

        assertTrue(client.changeWorkingDirectory(TEST_DIR1.getName()));
        assertEquals("/dir1", client.printWorkingDirectory());

        assertTrue(client.changeWorkingDirectory(TEST_DIR_IN_DIR1.getName()));
        assertEquals("/dir1/dir3", client.printWorkingDirectory());

        assertTrue(client.changeWorkingDirectory('/' + TEST_DIR2.getName()));
        assertEquals("/dir2", client.printWorkingDirectory());

        assertTrue(client.changeWorkingDirectory("/ leadingspace"));
        assertEquals("/ leadingspace", client.printWorkingDirectory());

        assertTrue(client.changeWorkingDirectory("/"));
        assertEquals("/", client.printWorkingDirectory());

        assertTrue(client.changeWorkingDirectory(TEST_DIR1.getName()));
        assertEquals("/dir1", client.printWorkingDirectory());

        assertTrue(client.changeWorkingDirectory("."));
        assertEquals("/dir1", client.printWorkingDirectory());

        assertTrue(client.changeWorkingDirectory(".."));
        assertEquals("/", client.printWorkingDirectory());

        assertTrue(client.changeWorkingDirectory(TEST_DIR1.getName() + '/'
                + TEST_DIR_IN_DIR1.getName()));
        assertEquals("/dir1/dir3", client.printWorkingDirectory());
    }

    public void testCDUP() throws Exception {

        assertTrue(client.changeWorkingDirectory(TEST_DIR1.getName() + '/'
                + TEST_DIR_IN_DIR1.getName()));
        assertEquals("/dir1/dir3", client.printWorkingDirectory());

        assertTrue(client.changeToParentDirectory());
        assertEquals("/dir1", client.printWorkingDirectory());

        assertTrue(client.changeToParentDirectory());
        assertEquals("/", client.printWorkingDirectory());

        assertTrue(client.changeToParentDirectory());
        assertEquals("/", client.printWorkingDirectory());
    }

}
