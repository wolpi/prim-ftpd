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

package org.apache.ftpserver.impl;

import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.UUID;

import org.apache.ftpserver.ftplet.DataConnectionFactory;
import org.apache.ftpserver.ftplet.DataType;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.Structure;
import org.apache.ftpserver.ftplet.User;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * FTP session
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultFtpSession implements FtpSession {

    private final FtpIoSession ioSession;

    /**
     * Default constructor.
     */
    public DefaultFtpSession(final FtpIoSession ioSession) {
        this.ioSession = ioSession;
    }

    /**
     * Is logged-in
     */
    public boolean isLoggedIn() {
        return ioSession.isLoggedIn();
    }

    /**
     * Get FTP data connection.
     */
    public DataConnectionFactory getDataConnection() {
        return ioSession.getDataConnection();
    }

    /**
     * Get file system view.
     */
    public FileSystemView getFileSystemView() {
        return ioSession.getFileSystemView();
    }

    /**
     * Get connection time.
     */
    public Date getConnectionTime() {
        return new Date(ioSession.getCreationTime());
    }

    /**
     * Get the login time.
     */
    public Date getLoginTime() {
        return ioSession.getLoginTime();
    }

    /**
     * Get last access time.
     */
    public Date getLastAccessTime() {
        return ioSession.getLastAccessTime();
    }

    /**
     * Get file offset.
     */
    public long getFileOffset() {
        return ioSession.getFileOffset();
    }

    /**
     * Get rename from file object.
     */
    public FtpFile getRenameFrom() {
        return ioSession.getRenameFrom();
    }

    /**
     * Returns user name entered in USER command
     * 
     * @return user name entered in USER command
     */
    public String getUserArgument() {
        return ioSession.getUserArgument();
    }

    /**
     * Get language.
     */
    public String getLanguage() {
        return ioSession.getLanguage();
    }

    /**
     * Get user.
     */
    public User getUser() {
        return ioSession.getUser();
    }

    /**
     * Get remote address
     */
    public InetSocketAddress getClientAddress() {
        if (ioSession.getRemoteAddress() instanceof InetSocketAddress) {
            return ((InetSocketAddress) ioSession.getRemoteAddress());
        } else {
            return null;
        }
    }

    /**
     * Get attribute
     */
    public Object getAttribute(final String name) {
        if (name.startsWith(FtpIoSession.ATTRIBUTE_PREFIX)) {
            throw new IllegalArgumentException(
                    "Illegal lookup of internal attribute");
        }

        return ioSession.getAttribute(name);
    }

    /**
     * Set attribute.
     */
    public void setAttribute(final String name, final Object value) {
        if (name.startsWith(FtpIoSession.ATTRIBUTE_PREFIX)) {
            throw new IllegalArgumentException(
                    "Illegal setting of internal attribute");
        }

        ioSession.setAttribute(name, value);
    }

    public int getMaxIdleTime() {
        return ioSession.getMaxIdleTime();
    }

    public void setMaxIdleTime(final int maxIdleTime) {
        ioSession.setMaxIdleTime(maxIdleTime);
    }

    /**
     * Get the data type.
     */
    public DataType getDataType() {
        return ioSession.getDataType();
    }

    /**
     * Get structure.
     */
    public Structure getStructure() {
        return ioSession.getStructure();
    }

    public Certificate[] getClientCertificates() {
        return ioSession.getClientCertificates();
    }

    public InetSocketAddress getServerAddress() {
        if (ioSession.getLocalAddress() instanceof InetSocketAddress) {
            return ((InetSocketAddress) ioSession.getLocalAddress());
        } else {
            return null;
        }
    }

    public int getFailedLogins() {
        return ioSession.getFailedLogins();
    }

    public void removeAttribute(final String name) {
        if (name.startsWith(FtpIoSession.ATTRIBUTE_PREFIX)) {
            throw new IllegalArgumentException(
                    "Illegal removal of internal attribute");
        }

        ioSession.removeAttribute(name);
    }

    public void write(FtpReply reply) throws FtpException {
        ioSession.write(reply);
    }

    public boolean isSecure() {
        // TODO Auto-generated method stub
        return ioSession.isSecure();
    }

    /**
     * Increase the number of bytes written on the data connection
     * @param increment The number of bytes written
     */
    public void increaseWrittenDataBytes(int increment) {
        ioSession.increaseWrittenDataBytes(increment);
    }

    /**
     * Increase the number of bytes read on the data connection
     * @param increment The number of bytes written
     */
    public void increaseReadDataBytes(int increment) {
        ioSession.increaseReadDataBytes(increment);
    }

    /**
     * {@inheritDoc}
     */
    public UUID getSessionId() {
        return ioSession.getSessionId();
    }

    
}
