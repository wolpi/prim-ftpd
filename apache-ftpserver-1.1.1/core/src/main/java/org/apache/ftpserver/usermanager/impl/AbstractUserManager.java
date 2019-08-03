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

import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.usermanager.Md5PasswordEncryptor;
import org.apache.ftpserver.usermanager.PasswordEncryptor;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * Abstract common base type for {@link UserManager} implementations
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractUserManager implements UserManager {

    public static final String ATTR_LOGIN = "userid";

    public static final String ATTR_PASSWORD = "userpassword";

    public static final String ATTR_HOME = "homedirectory";

    public static final String ATTR_WRITE_PERM = "writepermission";

    public static final String ATTR_ENABLE = "enableflag";

    public static final String ATTR_MAX_IDLE_TIME = "idletime";

    public static final String ATTR_MAX_UPLOAD_RATE = "uploadrate";

    public static final String ATTR_MAX_DOWNLOAD_RATE = "downloadrate";

    public static final String ATTR_MAX_LOGIN_NUMBER = "maxloginnumber";

    public static final String ATTR_MAX_LOGIN_PER_IP = "maxloginperip";

    private final String adminName;
    
    private final PasswordEncryptor passwordEncryptor;

    public AbstractUserManager() {
        this(null,  new Md5PasswordEncryptor());
    }

    /**
     * Internal constructor, do not use directly
     */
    public AbstractUserManager(String adminName, PasswordEncryptor passwordEncryptor) {
        this.adminName = adminName;
        this.passwordEncryptor = passwordEncryptor;
    }

    /**
     * Get the admin name.
     */
    public String getAdminName() {
        return adminName;
    }

    /**
     * @return true if user with this login is administrator
     */
    public boolean isAdmin(String login) throws FtpException {
        return adminName.equals(login);
    }
    
    
    /**
     * Retrieve the password encryptor used for this user manager
     * @return The password encryptor. Default to {@link Md5PasswordEncryptor}
     *  if no other has been provided
     */
    public PasswordEncryptor getPasswordEncryptor() {
        return passwordEncryptor;
    }
}
