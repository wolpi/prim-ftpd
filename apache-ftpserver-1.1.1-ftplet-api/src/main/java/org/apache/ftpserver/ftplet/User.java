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

package org.apache.ftpserver.ftplet;

import java.util.List;

/**
 * Basic user interface.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface User {

    /**
     * Get the user name.
     * @return The user name, the same used for login
     */
    String getName();

    /**
     * Get password.
     * @return The users password or null if the user manager can not provide the password 
     */
    String getPassword();

    /** 
     * Get all authorities granted to this user
     * 
     * @return All authorities
     */
    List<? extends Authority> getAuthorities();

    /**
     * Get authorities of the specified type granted to this user
     * @param clazz The type of {@link Authority}
     * @return Authorities of the specified class
     */
    List<? extends Authority> getAuthorities(Class<? extends Authority> clazz);

    /**
     * Authorize a {@link AuthorizationRequest} for this user
     *
     * @param request
     *            The {@link AuthorizationRequest} to authorize
     * @return A populated AuthorizationRequest if the user was authorized, null
     *         otherwise.
     */
    AuthorizationRequest authorize(AuthorizationRequest request);

    /**
     * Get the maximum idle time in seconds. Zero or less idle time means no
     * limit.
     * @return The idle time in seconds
     */
    int getMaxIdleTime();

    /**
     * Get the user enable status.
     * @return true if the user is enabled
     */
    boolean getEnabled();

    /**
     * Get the user home directory
     * @return The path to the home directory for the user
     */
    String getHomeDirectory();
}
