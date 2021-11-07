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

package org.apache.ftpserver.usermanager;

import javax.sql.DataSource;

import org.apache.ftpserver.FtpServerConfigurationException;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.usermanager.impl.DbUserManager;

/**
 * Factory for database backed {@link UserManager} instances.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DbUserManagerFactory implements UserManagerFactory {

    private String adminName = "admin";
    
    private String insertUserStmt;

    private String updateUserStmt;

    private String deleteUserStmt;

    private String selectUserStmt;

    private String selectAllStmt;

    private String isAdminStmt;

    private String authenticateStmt;

    private DataSource dataSource;

    private PasswordEncryptor passwordEncryptor = new Md5PasswordEncryptor();
    
    public UserManager createUserManager() {
        if (dataSource == null) {
            throw new FtpServerConfigurationException(
                    "Required data source not provided");
        }
        if (insertUserStmt == null) {
            throw new FtpServerConfigurationException(
                    "Required insert user SQL statement not provided");
        }
        if (updateUserStmt == null) {
            throw new FtpServerConfigurationException(
                    "Required update user SQL statement not provided");
        }
        if (deleteUserStmt == null) {
            throw new FtpServerConfigurationException(
                    "Required delete user SQL statement not provided");
        }
        if (selectUserStmt == null) {
            throw new FtpServerConfigurationException(
                    "Required select user SQL statement not provided");
        }
        if (selectAllStmt == null) {
            throw new FtpServerConfigurationException(
                    "Required select all users SQL statement not provided");
        }
        if (isAdminStmt == null) {
            throw new FtpServerConfigurationException(
                    "Required is admin user SQL statement not provided");
        }
        if (authenticateStmt == null) {
            throw new FtpServerConfigurationException(
                    "Required authenticate user SQL statement not provided");
        }
        
        return new DbUserManager(dataSource, selectAllStmt, selectUserStmt, 
                insertUserStmt, updateUserStmt, deleteUserStmt, authenticateStmt, 
                isAdminStmt, passwordEncryptor, adminName);
    }
    
    /**
     * Get the admin name.
     * @return The admin user name
     */
    public String getAdminName() {
        return adminName;
    }

    /**
     * Set the name to use as the administrator of the server. The default value
     * is "admin".
     * 
     * @param adminName
     *            The administrator user name
     */
    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }
    
    /**
     * Retrive the data source used by the user manager
     * 
     * @return The current data source
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Set the data source to be used by the user manager
     * 
     * @param dataSource
     *            The data source to use
     */
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Get the SQL INSERT statement used to add a new user.
     * 
     * @return The SQL statement
     */
    public String getSqlUserInsert() {
        return insertUserStmt;
    }

    /**
     * Set the SQL INSERT statement used to add a new user. All the dynamic
     * values will be replaced during runtime.
     * 
     * @param sql
     *            The SQL statement
     */
    public void setSqlUserInsert(String sql) {
        insertUserStmt = sql;
    }

    /**
     * Get the SQL DELETE statement used to delete an existing user.
     * 
     * @return The SQL statement
     */
    public String getSqlUserDelete() {
        return deleteUserStmt;
    }

    /**
     * Set the SQL DELETE statement used to delete an existing user. All the
     * dynamic values will be replaced during runtime.
     * 
     * @param sql
     *            The SQL statement
     */
    public void setSqlUserDelete(String sql) {
        deleteUserStmt = sql;
    }

    /**
     * Get the SQL UPDATE statement used to update an existing user.
     * 
     * @return The SQL statement
     */
    public String getSqlUserUpdate() {
        return updateUserStmt;
    }

    /**
     * Set the SQL UPDATE statement used to update an existing user. All the
     * dynamic values will be replaced during runtime.
     * 
     * @param sql
     *            The SQL statement
     */
    public void setSqlUserUpdate(String sql) {
        updateUserStmt = sql;
    }

    /**
     * Get the SQL SELECT statement used to select an existing user.
     * 
     * @return The SQL statement
     */
    public String getSqlUserSelect() {
        return selectUserStmt;
    }

    /**
     * Set the SQL SELECT statement used to select an existing user. All the
     * dynamic values will be replaced during runtime.
     * 
     * @param sql
     *            The SQL statement
     */
    public void setSqlUserSelect(String sql) {
        selectUserStmt = sql;
    }

    /**
     * Get the SQL SELECT statement used to select all user ids.
     * 
     * @return The SQL statement
     */
    public String getSqlUserSelectAll() {
        return selectAllStmt;
    }

    /**
     * Set the SQL SELECT statement used to select all user ids. All the dynamic
     * values will be replaced during runtime.
     * 
     * @param sql
     *            The SQL statement
     */
    public void setSqlUserSelectAll(String sql) {
        selectAllStmt = sql;
    }

    /**
     * Get the SQL SELECT statement used to authenticate user.
     * 
     * @return The SQL statement
     */
    public String getSqlUserAuthenticate() {
        return authenticateStmt;
    }

    /**
     * Set the SQL SELECT statement used to authenticate user. All the dynamic
     * values will be replaced during runtime.
     * 
     * @param sql
     *            The SQL statement
     */
    public void setSqlUserAuthenticate(String sql) {
        authenticateStmt = sql;
    }

    /**
     * Get the SQL SELECT statement used to find whether an user is admin or
     * not.
     * 
     * @return The SQL statement
     */
    public String getSqlUserAdmin() {
        return isAdminStmt;
    }

    /**
     * Set the SQL SELECT statement used to find whether an user is admin or
     * not. All the dynamic values will be replaced during runtime.
     * 
     * @param sql
     *            The SQL statement
     */
    public void setSqlUserAdmin(String sql) {
        isAdminStmt = sql;
    }

    /**
     * Retrieve the password encryptor used for this user manager
     * @return The password encryptor. Default to {@link Md5PasswordEncryptor}
     *  if no other has been provided
     */    
    public PasswordEncryptor getPasswordEncryptor() {
        return passwordEncryptor;
    }


    /**
     * Set the password encryptor to use for this user manager
     * @param passwordEncryptor The password encryptor
     */
    public void setPasswordEncryptor(PasswordEncryptor passwordEncryptor) {
        this.passwordEncryptor = passwordEncryptor;
    }
}