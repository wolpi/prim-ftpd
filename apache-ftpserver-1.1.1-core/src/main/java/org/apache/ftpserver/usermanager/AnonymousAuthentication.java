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

import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.usermanager.impl.UserMetadata;

/**
 * Class representing an anonymous authentication attempt
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class AnonymousAuthentication implements Authentication {

    private UserMetadata userMetadata;

    /**
     * Default constructor
     */
    public AnonymousAuthentication() {
        // empty
    }

    /**
     * Constructor with an additional user metadata parameter
     * 
     * @param userMetadata
     *            User metadata
     */
    public AnonymousAuthentication(UserMetadata userMetadata) {
        this.userMetadata = userMetadata;
    }

    /**
     * Retrive the user metadata
     * 
     * @return The user metadata
     */
    public UserMetadata getUserMetadata() {
        return userMetadata;
    }

}
