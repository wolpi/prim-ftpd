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

/**
 * Interface for an authority granted to the user, typical example is write
 * access or the number of concurrent logins
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface Authority {

    /**
     * Indicates weather this Authority can authorize a certain request
     * 
     * @param request
     *            The request to authorize
     * @return True if the request can be authorized by this Authority
     */
    boolean canAuthorize(AuthorizationRequest request);

    /**
     * Authorize an {@link AuthorizationRequest}.
     * 
     * @param request
     *            The {@link AuthorizationRequest}
     * @return Returns a populated AuthorizationRequest as long as If
     *         {@link #canAuthorize(AuthorizationRequest)} returns true for the 
     *         AuthorizationRequest, otherwise returns null. 
     *         {@link #canAuthorize(AuthorizationRequest)} should always be checked before 
     *         calling this method.
     */
    AuthorizationRequest authorize(AuthorizationRequest request);
}
