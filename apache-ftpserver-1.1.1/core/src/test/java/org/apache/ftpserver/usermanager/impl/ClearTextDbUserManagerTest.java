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

package org.apache.ftpserver.usermanager.impl;

import java.io.File;

import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.test.TestUtil;
import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor;
import org.apache.ftpserver.usermanager.DbUserManagerFactory;
import org.apache.ftpserver.usermanager.UserManagerFactory;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class ClearTextDbUserManagerTest extends DbUserManagerTest {

    @Override
    protected File getInitSqlScript() {
        return new File(TestUtil.getBaseDir(),
            "src/test/resources/dbusermanagertest-cleartext-hsql.sql");  
    }


    @Override
    protected UserManagerFactory createUserManagerFactory() throws FtpException {
        DbUserManagerFactory manager = (DbUserManagerFactory) super.createUserManagerFactory();
        manager.setPasswordEncryptor(new ClearTextPasswordEncryptor());
        return manager;

    }
}
