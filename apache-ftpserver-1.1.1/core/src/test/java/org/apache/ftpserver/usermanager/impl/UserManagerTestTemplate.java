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

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.usermanager.UserManagerFactory;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public abstract class UserManagerTestTemplate extends TestCase {

    protected UserManager userManager;

    protected abstract UserManagerFactory createUserManagerFactory() throws Exception;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        userManager = createUserManagerFactory().createUserManager();
    }

    public void testAuthenticate() throws Exception {
        assertNotNull(userManager
                .authenticate(new UsernamePasswordAuthentication("user1", "pw1")));
    }

    public void testAuthenticateWrongPassword() throws Exception {
        try {
            userManager.authenticate(new UsernamePasswordAuthentication(
                    "user1", "foo"));
            fail("Must throw AuthenticationFailedException");
        } catch (AuthenticationFailedException e) {
            // ok
        }
    }

    public void testAuthenticateUnknownUser() throws Exception {
        try {
            userManager.authenticate(new UsernamePasswordAuthentication("foo",
                    "foo"));
            fail("Must throw AuthenticationFailedException");
        } catch (AuthenticationFailedException e) {
            // ok
        }
    }

    public void testAuthenticateEmptyPassword() throws Exception {
        assertNotNull(userManager
                .authenticate(new UsernamePasswordAuthentication("user3", "")));
    }

    public void testAuthenticateNullPassword() throws Exception {
        assertNotNull(userManager
                .authenticate(new UsernamePasswordAuthentication("user3", null)));
    }

    public static class FooAuthentication implements Authentication {
    }

    public void testAuthenticateNullUser() throws Exception {
        try {
            userManager.authenticate(new UsernamePasswordAuthentication(null,
                    "foo"));
            fail("Must throw AuthenticationFailedException");
        } catch (AuthenticationFailedException e) {
            // ok
        }
    }

    public void testAuthenticateUnknownAuthentication() throws Exception {
        try {
            userManager.authenticate(new FooAuthentication());
            fail("Must throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testDoesExist() throws Exception {
        assertTrue(userManager.doesExist("user1"));
        assertTrue(userManager.doesExist("user2"));
        assertFalse(userManager.doesExist("foo"));
    }

    public void testGetAdminName() throws Exception {
        assertEquals("admin", userManager.getAdminName());
    }

    public void testIsAdmin() throws Exception {
        assertTrue(userManager.isAdmin("admin"));
        assertFalse(userManager.isAdmin("user1"));
        assertFalse(userManager.isAdmin("foo"));
    }

    public void testDelete() throws Exception {
        assertTrue(userManager.doesExist("user1"));
        assertTrue(userManager.doesExist("user2"));
        userManager.delete("user1");
        assertFalse(userManager.doesExist("user1"));
        assertTrue(userManager.doesExist("user2"));
        userManager.delete("user2");
        assertFalse(userManager.doesExist("user1"));
        assertFalse(userManager.doesExist("user2"));
    }

    public void testDeleteNonExistingUser() throws Exception {
        // silent failure
        userManager.delete("foo");
    }

    public void testGetUserByNameWithDefaultValues() throws Exception {
        User user = userManager.getUserByName("user1");

        assertEquals("user1", user.getName());
        assertNull("Password must not be set", user.getPassword());
        assertEquals("home", user.getHomeDirectory());
        assertEquals(0, getMaxDownloadRate(user));
        assertEquals(0, user.getMaxIdleTime());
        assertEquals(0, getMaxLoginNumber(user));
        assertEquals(0, getMaxLoginPerIP(user));
        assertEquals(0, getMaxUploadRate(user));
        assertNull(user.authorize(new WriteRequest()));
        assertTrue(user.getEnabled());
    }

    public void testGetUserByName() throws Exception {
        User user = userManager.getUserByName("user2");

        assertEquals("user2", user.getName());
        assertNull("Password must not be set", user.getPassword());
        assertEquals("home", user.getHomeDirectory());
        assertEquals(1, getMaxDownloadRate(user));
        assertEquals(2, user.getMaxIdleTime());
        assertEquals(3, getMaxLoginNumber(user));
        assertEquals(4, getMaxLoginPerIP(user));
        assertEquals(5, getMaxUploadRate(user));
        assertNotNull(user.authorize(new WriteRequest()));
        assertFalse(user.getEnabled());
    }

    public void testGetUserByNameWithUnknownUser() throws Exception {
        assertNull(userManager.getUserByName("foo"));
    }

    private int getMaxDownloadRate(User user) {
        TransferRateRequest transferRateRequest = new TransferRateRequest();
        transferRateRequest = (TransferRateRequest) user
                .authorize(transferRateRequest);

        if (transferRateRequest != null) {
            return transferRateRequest.getMaxDownloadRate();
        } else {
            return 0;
        }
    }

    private int getMaxUploadRate(User user) {
        TransferRateRequest transferRateRequest = new TransferRateRequest();
        transferRateRequest = (TransferRateRequest) user
                .authorize(transferRateRequest);

        if (transferRateRequest != null) {
            return transferRateRequest.getMaxUploadRate();
        } else {
            return 0;
        }
    }

    private int getMaxLoginNumber(User user) {
        ConcurrentLoginRequest concurrentLoginRequest = new ConcurrentLoginRequest(
                0, 0);
        concurrentLoginRequest = (ConcurrentLoginRequest) user
                .authorize(concurrentLoginRequest);

        if (concurrentLoginRequest != null) {
            return concurrentLoginRequest.getMaxConcurrentLogins();
        } else {
            return 0;
        }
    }

    private int getMaxLoginPerIP(User user) {
        ConcurrentLoginRequest concurrentLoginRequest = new ConcurrentLoginRequest(
                0, 0);
        concurrentLoginRequest = (ConcurrentLoginRequest) user
                .authorize(concurrentLoginRequest);

        if (concurrentLoginRequest != null) {
            return concurrentLoginRequest.getMaxConcurrentLoginsPerIP();
        } else {
            return 0;
        }
    }

    public void testSave() throws Exception {
        BaseUser user = new BaseUser();
        user.setName("newuser");
        user.setPassword("newpw");
        user.setHomeDirectory("newhome");
        user.setEnabled(false);
        user.setMaxIdleTime(2);

        List<Authority> authorities = new ArrayList<Authority>();
        authorities.add(new WritePermission());
        authorities.add(new ConcurrentLoginPermission(3, 4));
        authorities.add(new TransferRatePermission(1, 5));
        user.setAuthorities(authorities);

        userManager.save(user);

        User actualUser = userManager.getUserByName("newuser");

        assertEquals(user.getName(), actualUser.getName());
        assertNull(actualUser.getPassword());
        assertEquals(user.getHomeDirectory(), actualUser.getHomeDirectory());
        assertEquals(user.getEnabled(), actualUser.getEnabled());
        assertNotNull(user.authorize(new WriteRequest()));
        assertEquals(getMaxDownloadRate(user), getMaxDownloadRate(actualUser));
        assertEquals(user.getMaxIdleTime(), actualUser.getMaxIdleTime());
        assertEquals(getMaxLoginNumber(user), getMaxLoginNumber(actualUser));
        assertEquals(getMaxLoginPerIP(user), getMaxLoginPerIP(actualUser));
        assertEquals(getMaxUploadRate(user), getMaxUploadRate(actualUser));
        
        // verify the password
        assertNotNull(userManager.authenticate(new UsernamePasswordAuthentication("newuser", "newpw")));

        try {
            userManager.authenticate(new UsernamePasswordAuthentication("newuser", "dummy"));
            fail("Must throw AuthenticationFailedException");
        } catch(AuthenticationFailedException e) {
            // ok
        }

        // save without updating the users password (password==null)
        userManager.save(user);

        assertNotNull(userManager.authenticate(new UsernamePasswordAuthentication("newuser", "newpw")));
        try {
            userManager.authenticate(new UsernamePasswordAuthentication("newuser", "dummy"));
            fail("Must throw AuthenticationFailedException");
        } catch(AuthenticationFailedException e) {
            // ok
        }

               
        // save and update the users password
        user.setPassword("newerpw");
        userManager.save(user);
        
        assertNotNull(userManager.authenticate(new UsernamePasswordAuthentication("newuser", "newerpw")));

        try {
            userManager.authenticate(new UsernamePasswordAuthentication("newuser", "newpw"));
            fail("Must throw AuthenticationFailedException");
        } catch(AuthenticationFailedException e) {
            // ok
        }

    }

    public void testSavePersistent() throws Exception {
        BaseUser user = new BaseUser();
        user.setName("newuser");
        user.setPassword("newpw");
        user.setHomeDirectory("newhome");
        user.setEnabled(false);
        user.setMaxIdleTime(2);

        List<Authority> authorities = new ArrayList<Authority>();
        authorities.add(new WritePermission());
        authorities.add(new ConcurrentLoginPermission(3, 4));
        authorities.add(new TransferRatePermission(1, 5));
        user.setAuthorities(authorities);

        userManager.save(user);

        UserManager newUserManager = createUserManagerFactory().createUserManager();

        User actualUser = newUserManager.getUserByName("newuser");

        assertEquals(user.getName(), actualUser.getName());
        assertNull(actualUser.getPassword());
        assertEquals(user.getHomeDirectory(), actualUser.getHomeDirectory());
        assertEquals(user.getEnabled(), actualUser.getEnabled());
        assertNotNull(user.authorize(new WriteRequest()));
        assertEquals(getMaxDownloadRate(user), getMaxDownloadRate(actualUser));
        assertEquals(user.getMaxIdleTime(), actualUser.getMaxIdleTime());
        assertEquals(getMaxLoginNumber(user), getMaxLoginNumber(actualUser));
        assertEquals(getMaxLoginPerIP(user), getMaxLoginPerIP(actualUser));
        assertEquals(getMaxUploadRate(user), getMaxUploadRate(actualUser));
        
        // verify the password
        assertNotNull(newUserManager.authenticate(new UsernamePasswordAuthentication("newuser", "newpw")));

        try {
            newUserManager.authenticate(new UsernamePasswordAuthentication("newuser", "dummy"));
            fail("Must throw AuthenticationFailedException");
        } catch(AuthenticationFailedException e) {
            // ok
        }

        // save without updating the users password (password==null)
        userManager.save(user);

        newUserManager = createUserManagerFactory().createUserManager();
        assertNotNull(newUserManager.authenticate(new UsernamePasswordAuthentication("newuser", "newpw")));
        try {
            newUserManager.authenticate(new UsernamePasswordAuthentication("newuser", "dummy"));
            fail("Must throw AuthenticationFailedException");
        } catch(AuthenticationFailedException e) {
            // ok
        }

               
        // save and update the users password
        user.setPassword("newerpw");
        userManager.save(user);
        
        newUserManager = createUserManagerFactory().createUserManager();
        assertNotNull(newUserManager.authenticate(new UsernamePasswordAuthentication("newuser", "newerpw")));

        try {
            newUserManager.authenticate(new UsernamePasswordAuthentication("newuser", "newpw"));
            fail("Must throw AuthenticationFailedException");
        } catch(AuthenticationFailedException e) {
            // ok
        }

    }

    
    public void testSaveWithExistingUser() throws Exception {
        BaseUser user = new BaseUser();
        user.setName("user2");
        user.setHomeDirectory("newhome");
        userManager.save(user);

        User actualUser = userManager.getUserByName("user2");

        assertEquals("user2", actualUser.getName());
        assertNull(actualUser.getPassword());
        assertEquals("newhome", actualUser.getHomeDirectory());
        assertEquals(0, getMaxDownloadRate(actualUser));
        assertEquals(0, actualUser.getMaxIdleTime());
        assertEquals(0, getMaxLoginNumber(actualUser));
        assertEquals(0, getMaxLoginPerIP(actualUser));
        assertEquals(0, getMaxUploadRate(actualUser));
        assertNull(user.authorize(new WriteRequest()));
        assertTrue(actualUser.getEnabled());
    }

    public void testSaveWithDefaultValues() throws Exception {
        BaseUser user = new BaseUser();
        user.setName("newuser");
        user.setPassword("newpw");
        userManager.save(user);

        User actualUser = userManager.getUserByName("newuser");

        assertEquals(user.getName(), actualUser.getName());
        assertNull(actualUser.getPassword());
        assertEquals("/", actualUser.getHomeDirectory());
        assertEquals(true, actualUser.getEnabled());
        assertNull(user.authorize(new WriteRequest()));
        assertEquals(0, getMaxDownloadRate(actualUser));
        assertEquals(0, actualUser.getMaxIdleTime());
        assertEquals(0, getMaxLoginNumber(actualUser));
        assertEquals(0, getMaxLoginPerIP(actualUser));
        assertEquals(0, getMaxUploadRate(actualUser));
    }
}
