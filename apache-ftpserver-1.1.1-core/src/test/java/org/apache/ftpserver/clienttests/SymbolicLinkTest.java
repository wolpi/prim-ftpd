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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class SymbolicLinkTest extends ClientTestTemplate {
    private static final File TEST_REAL_DIR1 = new File(ROOT_DIR, "dir1");

    private static final File TEST_SYMBOLIC_DIR1 = new File(ROOT_DIR, "symbolic");

    // test is only enabled if the OS supports the ln -s command
    private boolean testEnabled = false;
    
    /*
     * (non-Javadoc)
     * 
     * @see org.apache.ftpserver.clienttests.ClientTestTemplate#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        assertTrue(TEST_REAL_DIR1.mkdir());
        
        // try creating the symbolic link
        String[] command = new String[]{"ln",  "-s", TEST_REAL_DIR1.getName(), TEST_SYMBOLIC_DIR1.getName()}; 
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(ROOT_DIR);
        pb.redirectErrorStream(true);
        Process process = null;
        try{
        process = pb.start();
        }catch(IOException e){
            testEnabled = false;
            return;
        }
        BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = out.readLine();
        while(line != null) {
            System.out.println(line);
            line = out.readLine();
        }
        
        if(process.waitFor() == 0) {
            testEnabled = true;
        }
        
        client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    private boolean checkEnabled() {
        if(!testEnabled) {
            System.out.println("Test disabled, \"ln -s\" not supported");
        }
        
        return testEnabled;
    }
    
    public void test() throws IOException {
        if(checkEnabled()) {
            client.cwd(TEST_SYMBOLIC_DIR1.getName());
            System.out.println(client.pwd());
            client.cwd("..");
            client.cwd(TEST_REAL_DIR1.getName());
            System.out.println(client.pwd());
            System.out.println(TEST_SYMBOLIC_DIR1.getAbsolutePath());
            System.out.println(TEST_SYMBOLIC_DIR1.getCanonicalPath());
        }
    }
}

