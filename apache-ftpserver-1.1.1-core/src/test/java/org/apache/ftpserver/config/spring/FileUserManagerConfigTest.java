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

package org.apache.ftpserver.config.spring;

import java.io.File;

import org.apache.ftpserver.impl.DefaultFtpServer;
import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor;
import org.apache.ftpserver.usermanager.Md5PasswordEncryptor;
import org.apache.ftpserver.usermanager.SaltedPasswordEncryptor;
import org.apache.ftpserver.usermanager.impl.PropertiesUserManager;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class FileUserManagerConfigTest extends SpringConfigTestTemplate {

    private static final String USER_FILE_PATH = "src/test/resources/users.properties";
    
    private PropertiesUserManager createPropertiesUserManager(String config) {
        DefaultFtpServer server = (DefaultFtpServer) createServer(config);

        return (PropertiesUserManager) server.getUserManager();
    }

    public void testFile() throws Throwable {
        PropertiesUserManager um = createPropertiesUserManager("<file-user-manager file=\"" + USER_FILE_PATH + "\" />");
        assertEquals(new File("src/test/resources/users.properties"), um.getFile());
    }

    public void testMd5PasswordEncryptor() throws Throwable {
        PropertiesUserManager um = createPropertiesUserManager("<file-user-manager file=\"" + USER_FILE_PATH + "\" encrypt-passwords=\"md5\" />");

        assertTrue(um.getPasswordEncryptor() instanceof Md5PasswordEncryptor);
    }
    
    public void testTruePasswordEncryptor() throws Throwable {
        PropertiesUserManager um = createPropertiesUserManager("<file-user-manager file=\"" + USER_FILE_PATH + "\" encrypt-passwords=\"true\" />");

        assertTrue(um.getPasswordEncryptor() instanceof Md5PasswordEncryptor);
    }

    public void testNonePasswordEncryptor() throws Throwable {
        PropertiesUserManager um = createPropertiesUserManager("<file-user-manager file=\"" + USER_FILE_PATH + "\" encrypt-passwords=\"clear\" />");

        assertTrue(um.getPasswordEncryptor() instanceof ClearTextPasswordEncryptor);
    }

    public void testSaltedPasswordEncryptor() throws Throwable {
        PropertiesUserManager um = createPropertiesUserManager("<file-user-manager file=\"" + USER_FILE_PATH + "\" encrypt-passwords=\"salted\" />");

        assertTrue(um.getPasswordEncryptor() instanceof SaltedPasswordEncryptor);
    }
    
    public void testFalsePasswordEncryptor() throws Throwable {
        PropertiesUserManager um = createPropertiesUserManager("<file-user-manager file=\"" + USER_FILE_PATH + "\" encrypt-passwords=\"false\" />");

        assertTrue(um.getPasswordEncryptor() instanceof ClearTextPasswordEncryptor);
    }

}
