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

package org.apache.ftpserver;

/**
 * Interface for providing the configuration for the control socket connections.
 * 
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 */
public interface ConnectionConfig {

    /**
     * The maximum number of time an user can fail to login before getting disconnected
     * @return The maximum number of failure login attempts
     */
    int getMaxLoginFailures();

    /**
     * The delay in number of milliseconds between login failures. Important to 
     * make brute force attacks harder.
     * 
     * @return The delay time in milliseconds
     */
    int getLoginFailureDelay();

    /**
     * The maximum number of time an anonymous user can fail to login before getting disconnected
     * @return The maximum number of failer login attempts
     */
    int getMaxAnonymousLogins();

    /**
     * The maximum number of concurrently logged in users
     * @return The maximum number of users
     */
    int getMaxLogins();

    /**
     * Is anonymous logins allowed at the server?
     * @return true if anonymous logins are enabled
     */
    boolean isAnonymousLoginEnabled();
    
    /**
     * Returns the maximum number of threads the server is allowed to create for
     * processing client requests.
     * 
     * @return the maximum number of threads the server is allowed to create for
     *         processing client requests.
     */
    int getMaxThreads();
}
