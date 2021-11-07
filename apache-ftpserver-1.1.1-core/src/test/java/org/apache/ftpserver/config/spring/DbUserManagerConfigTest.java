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

import org.apache.ftpserver.impl.DefaultFtpServer;
import org.apache.ftpserver.usermanager.SaltedPasswordEncryptor;
import org.apache.ftpserver.usermanager.impl.DbUserManager;
import org.hsqldb.jdbc.jdbcDataSource;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class DbUserManagerConfigTest extends SpringConfigTestTemplate {

    public void test() throws Throwable {
        DefaultFtpServer server = (DefaultFtpServer) createServer("<db-user-manager  encrypt-passwords=\"salted\">"
                + "<data-source>"
                + "    <beans:bean class=\"org.hsqldb.jdbc.jdbcDataSource\">"
                + "        <beans:property name=\"database\" value=\"jdbc:hsqldb:mem:foo\" />"
                + "        <beans:property name=\"user\" value=\"sa\" />"
                + "        <beans:property name=\"password\" value=\"\" />"
                + "    </beans:bean>" + "</data-source>"
                + "<insert-user>INSERT USER</insert-user>"
                + "<update-user>UPDATE USER</update-user>"
                + "<delete-user>DELETE USER</delete-user>"
                + "<select-user>SELECT USER</select-user>"
                + "<select-all-users>SELECT ALL USERS</select-all-users>"
                + "<is-admin>IS ADMIN</is-admin>"
                + "<authenticate>AUTHENTICATE</authenticate>"
                + "</db-user-manager>");

        DbUserManager um = (DbUserManager) server.getUserManager();
        assertTrue(um.getDataSource() instanceof jdbcDataSource);
        assertTrue(um.getPasswordEncryptor() instanceof SaltedPasswordEncryptor);

        assertEquals("INSERT USER", um.getSqlUserInsert());
        assertEquals("UPDATE USER", um.getSqlUserUpdate());
        assertEquals("DELETE USER", um.getSqlUserDelete());
        assertEquals("SELECT USER", um.getSqlUserSelect());
        assertEquals("SELECT ALL USERS", um.getSqlUserSelectAll());
        assertEquals("IS ADMIN", um.getSqlUserAdmin());
        assertEquals("AUTHENTICATE", um.getSqlUserAuthenticate());

    }
}
