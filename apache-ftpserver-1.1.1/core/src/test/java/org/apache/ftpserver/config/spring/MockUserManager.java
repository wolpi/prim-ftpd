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

import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;

/*
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class MockUserManager implements UserManager {

    public User authenticate(Authentication authentication)
            throws AuthenticationFailedException {
        return null;
    }

    public void delete(String username) throws FtpException {
    }

    public boolean doesExist(String username) throws FtpException {
        return false;
    }

    public String getAdminName() throws FtpException {
        return null;
    }

    public String[] getAllUserNames() throws FtpException {
        return null;
    }

    public User getUserByName(String username) throws FtpException {
        return null;
    }

    public boolean isAdmin(String username) throws FtpException {
        return false;
    }

    public void save(User user) throws FtpException {
    }

}
