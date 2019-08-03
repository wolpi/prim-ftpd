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

package org.apache.ftpserver.listener.nio;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.logging.LoggingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * Specialized @see {@link LoggingFilter} that optionally masks FTP passwords.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class FtpLoggingFilter extends LoggingFilter {

    private boolean maskPassword = true;

    private final Logger logger;
    
    /**
     * @see LoggingFilter#LoggingFilter()
     */
    public FtpLoggingFilter() {
        this(FtpLoggingFilter.class.getName());
    }

    /**
     * @see LoggingFilter#LoggingFilter(Class)
     */
    public FtpLoggingFilter(Class<?> clazz) {
        this(clazz.getName());
    }

    /**
     * @see LoggingFilter#LoggingFilter(String)
     */
    public FtpLoggingFilter(String name) {
        super(name);
        
        logger = LoggerFactory.getLogger(name);
    }

    /**
     * @see LoggingFilter#messageReceived(org.apache.mina.core.filterchain.IoFilter.NextFilter,
     *      IoSession, Object)
     */
    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session,
            Object message) throws Exception {
        String request = (String) message;

        String logMessage;
        if (maskPassword) {

            if (request.trim().toUpperCase().startsWith("PASS ")) {
                logMessage = "PASS *****";
            } else {
                logMessage = request;
            }
        } else {
            logMessage = request;
        }

        logger.info("RECEIVED: {}", logMessage);
        nextFilter.messageReceived(session, message);
    }

    /**
     * Are password masked?
     * 
     * @return true if passwords are masked
     */
    public boolean isMaskPassword() {
        return maskPassword;
    }

    /**
     * Mask password in log messages
     * 
     * @param maskPassword
     *            true if passwords should be masked
     */
    public void setMaskPassword(boolean maskPassword) {
        this.maskPassword = maskPassword;
    }

}
