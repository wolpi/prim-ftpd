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
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface DataConnectionFactory {

    /**
     * Open an active data connection
     * 
     * @return The open data connection
     * @throws Exception
     */
    DataConnection openConnection() throws Exception;

    /**
     * Indicates whether the data socket created by this factory will be secure
     * that is, running over SSL/TLS.
     * 
     * @return true if the data socket will be secured
     */

    boolean isSecure();

    /**
     * Close data socket. If no open data connection exists,
     * this will silently ignore the call.
     */
    void closeDataConnection();

}