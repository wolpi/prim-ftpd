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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.UUID;

/**
 * Defines an client session with the FTP server. The session is born when the
 * client connects and dies when the client disconnects. Ftplet methods will
 * always get the same session for one user and one connection. So the
 * attributes set by <code>setAttribute()</code> will be always available later
 * unless that attribute is removed or the client disconnects.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface FtpSession {

    /**
     * Returns the IP address of the client that sent the request.
     * @return The client {@link InetAddress}
     */
    InetSocketAddress getClientAddress();

    /**
     * Returns the IP address of the server
     * @return The server {@link InetAddress}
     */
    InetSocketAddress getServerAddress();

    /**
     * Get FTP data connection factory, used to transfer data to and from the client.
     * @return The {@link DataConnectionFactory}
     */
    DataConnectionFactory getDataConnection();

    /**
     * Retrieve the certificates for the client, if running over SSL and with client authentication
     * @return The Certificate chain, or null if the certificates are not avialble
     */
    Certificate[] getClientCertificates();

    /**
     * Get connection time.
     * @return Time when the client connected to the server
     */
    Date getConnectionTime();

    /**
     * Get the login time.
     * @return Time when the client logged into the server
     */
    Date getLoginTime();

    /**
     * Get the number of failed logins. 
     * @return The number of failed logins. When login succeeds, this will return 0.
     */
    int getFailedLogins();

    /**
     * Get last access time.
     * @return The last time the session performed any action
     */
    Date getLastAccessTime();

    /**
     * Returns maximum idle time. This time equals to
     * {@link ConnectionManagerImpl#getDefaultIdleSec()} until user login, and
     * {@link User#getMaxIdleTime()} after user login.
     * @return The number of seconds the client is allowed to be idle before disconnected.
     */
    int getMaxIdleTime();

    /**
     * Set maximum idle time in seconds. This time equals to
     * {@link ConnectionManagerImpl#getDefaultIdleSec()} until user login, and
     * {@link User#getMaxIdleTime()} after user login.
     * @param maxIdleTimeSec The number of seconds the client is allowed to be idle before disconnected.
     */
    void setMaxIdleTime(int maxIdleTimeSec);

    /**
     * Get user object.
     * @return The current {@link User}
     */
    User getUser();

    /**
     * Returns user name entered in USER command
     * 
     * @return user name entered in USER command
     */
    String getUserArgument();

    /**
     * Get the requested language.
     * @return The language requested by the client
     */
    String getLanguage();

    /**
     * Is the user logged in?
     * @return true if the user is logged in
     */
    boolean isLoggedIn();

    /**
     * Get user file system view.
     * @return The {@link FileSystemView} for this session/user
     */
    FileSystemView getFileSystemView();

    /**
     * Get file upload/download offset.
     * @return The current file transfer offset, or 0 if non is set
     */
    long getFileOffset();

    /**
     * Get rename from file object.
     * @return The current rename from, or null if non is set
     */
    FtpFile getRenameFrom();

    /**
     * Get the data type.
     * @return The current {@link DataType} for this session
     */
    DataType getDataType();

    /**
     * Get structure.
     * @return The current {@link Structure} for this session
     */
    Structure getStructure();

    /**
     * Returns the value of the named attribute as an Object.
     * @param name The attribute name
     * @return The attribute value, or null if no
     * attribute of the given name exists.
     */
    Object getAttribute(String name);

    /**
     * Stores an attribute in this request. It will be available until it was
     * removed or when the connection ends.
     * @param name The attribute name
     * @param value The attribute value
     */
    void setAttribute(String name, Object value);

    /**
     * Removes an attribute from this request.
     * @param name The attribute name
     */
    void removeAttribute(String name);

    /**
     * Write a reply to the client
     * 
     * @param reply
     *            The reply that will be sent to the client
     * @throws FtpException
     */
    void write(FtpReply reply) throws FtpException;

    /**
     * Indicates whether the control socket for this session is secure, that is,
     * running over SSL/TLS
     * 
     * @return true if the control socket is secured
     */
    boolean isSecure();

    /**
     * Get the unique ID for this session. This ID will be maintained for 
     * the entire session and is also available to MDC logging using the "session"
     * identifier. 
     * @return The unique ID for this session
     */
    public UUID getSessionId();

}
