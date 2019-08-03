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
import java.io.FileReader;
import java.sql.Connection;
import java.sql.Statement;

import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.test.TestUtil;
import org.apache.ftpserver.usermanager.DbUserManagerFactory;
import org.apache.ftpserver.usermanager.UserManagerFactory;
import org.apache.ftpserver.util.IoUtils;
import org.hsqldb.jdbc.jdbcDataSource;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class DbUserManagerTest extends UserManagerTestTemplate {

    private jdbcDataSource ds;

    private Connection conn;

    protected File getInitSqlScript() {
        return new File(TestUtil.getBaseDir(),
            "src/test/resources/dbusermanagertest-hsql.sql");  
    }
    
    private void createDatabase() throws Exception {
        conn = ds.getConnection();
        conn.setAutoCommit(true);

        String ddl = IoUtils.readFully(new FileReader(getInitSqlScript()));

        Statement stm = conn.createStatement();
        stm.execute(ddl);
    }

    @Override
    protected UserManagerFactory createUserManagerFactory() throws FtpException {
        DbUserManagerFactory manager = new DbUserManagerFactory();

        manager.setDataSource(ds);
        manager
                .setSqlUserInsert("INSERT INTO FTP_USER (userid, userpassword, homedirectory, enableflag, writepermission, idletime, uploadrate, downloadrate, maxloginnumber, maxloginperip) VALUES ('{userid}', '{userpassword}', '{homedirectory}', {enableflag}, {writepermission}, {idletime}, {uploadrate}, {downloadrate}, {maxloginnumber}, {maxloginperip})");
        manager
                .setSqlUserUpdate("UPDATE FTP_USER SET userpassword='{userpassword}',homedirectory='{homedirectory}',enableflag={enableflag},writepermission={writepermission},idletime={idletime},uploadrate={uploadrate},downloadrate={downloadrate},maxloginnumber={maxloginnumber}, maxloginperip={maxloginperip} WHERE userid='{userid}'");
        manager
                .setSqlUserDelete("DELETE FROM FTP_USER WHERE userid = '{userid}'");
        manager
                .setSqlUserSelect("SELECT * FROM FTP_USER WHERE userid = '{userid}'");
        manager
                .setSqlUserSelectAll("SELECT userid FROM FTP_USER ORDER BY userid");
        manager
                .setSqlUserAuthenticate("SELECT userid, userpassword FROM FTP_USER WHERE userid='{userid}'");
        manager
                .setSqlUserAdmin("SELECT userid FROM FTP_USER WHERE userid='{userid}' AND userid='admin'");

        return manager;

    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        ds = new jdbcDataSource();
        ds.setDatabase("jdbc:hsqldb:mem:ftpd");
        ds.setUser("sa");
        ds.setPassword("");

        createDatabase();

        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        Statement stm = conn.createStatement();
        stm.execute("SHUTDOWN");

        super.tearDown();
    }

}
