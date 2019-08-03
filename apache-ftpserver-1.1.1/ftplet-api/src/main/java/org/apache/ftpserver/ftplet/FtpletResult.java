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
 * This class encapsulates the return values of the ftplet methods.
 * 
 * DEFAULT < NO_FTPLET < SKIP < DISCONNECT
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public enum FtpletResult {

    /**
     * This return value indicates that the next ftplet method will be called.
     * If no other ftplet is available, the ftpserver will process the request.
     */
    DEFAULT,

    /**
     * This return value indicates that the other ftplet methods will not be
     * called but the ftpserver will continue processing this request.
     */
    NO_FTPLET,

    /**
     * It indicates that the ftpserver will skip everything. No further
     * processing (both ftplet and server) will be done for this request.
     */
    SKIP,

    /**
     * It indicates that the server will skip and disconnect the client. No
     * other request from the same client will be served.
     */
    DISCONNECT;
}
