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

import org.apache.ftpserver.ftplet.FtpException;


/**
 * This is the starting point of all the servers. It invokes a new listener
 * thread. <code>Server</code> implementation is used to create the server
 * socket and handle client connection.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface FtpServer {

    /**
     * Start the server. Open a new listener thread.
     * @throws FtpException 
     */
    void start() throws FtpException;
    
    /**
     * Stop the server. Stop the listener thread.
     */
    void stop();
    
    /**
     * Get the server status.
     * @return true if the server is stopped
     */
    boolean isStopped();
    
    /**
     * Suspend further requests
     */
    void suspend();
    
    /**
     * Resume the server handler
     */
    void resume();
    
    /**
     * Is the server suspended
     * @return true if the server is suspended
     */
    boolean isSuspended();
    
}
