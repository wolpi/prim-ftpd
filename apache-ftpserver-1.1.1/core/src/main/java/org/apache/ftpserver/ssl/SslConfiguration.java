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

package org.apache.ftpserver.ssl;

import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

/**
 * SSL configuration
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface SslConfiguration {
	
	/**
	 * Returns the socket factory that can be used to create sockets using 
	 * 		   this <code>SslConfiguration</code>. 
	 * @return the socket factory that can be used to create sockets using this 
	 * 		   <code>SslConfiguration</code>.
	 * @throws GeneralSecurityException if any error occurs while creating the 
	 *         socket factory.  
	 * 		    
	 */
	SSLSocketFactory getSocketFactory() throws GeneralSecurityException;

    /**
     * Return the SSL context for this configuration
     * 
     * @return The {@link SSLContext}
     * @throws GeneralSecurityException
     */
    SSLContext getSSLContext() throws GeneralSecurityException;

    /**
     * Return the SSL context for this configuration given the specified
     * protocol
     * 
     * @param protocol
     *            The protocol, SSL or TLS must be supported
     * @return The {@link SSLContext}
     * @throws GeneralSecurityException
     */
    SSLContext getSSLContext(String protocol) throws GeneralSecurityException;

    /**
     * Returns the cipher suites that should be enabled for this connection.
     * Must return null if the default (as decided by the JVM) cipher suites
     * should be used.
     * 
     * @return An array of cipher suites, or null.
     */
    String[] getEnabledCipherSuites();

    /**
     * Return the required client authentication setting
     * 
     * @return {@link ClientAuth#NEED} if client authentication is required,
     *         {@link ClientAuth#WANT} is client authentication is wanted or
     *         {@link ClientAuth#NONE} if no client authentication is the be
     *         performed
     */
    ClientAuth getClientAuth();
}
