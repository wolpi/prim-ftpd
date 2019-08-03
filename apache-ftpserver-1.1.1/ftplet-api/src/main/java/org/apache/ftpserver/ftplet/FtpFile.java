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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * This is the file abstraction used by the server.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface FtpFile {

    /**
     * Get the full path from the base directory of the FileSystemView.
     * @return a path where the path separator is '/' (even if the operating system
     *     uses another character as path separator).
     */
    String getAbsolutePath();

    /**
     * Get the file name of the file
     * @return the last part of the file path (the part after the last '/').
     */
    String getName();

    /**
     * Is the file hidden?
     * @return true if the {@link FtpFile} is hidden
     */
    boolean isHidden();

    /**
     * Is it a directory?
     * @return true if the {@link FtpFile} is a directory
     */
    boolean isDirectory();

    /**
     * Is it a file?
     * @return true if the {@link FtpFile} is a file, false if it is a directory
     */
    boolean isFile();

    /**
     * Does this file exists?
     * @return true if the {@link FtpFile} exists
     */
    boolean doesExist();

    /**
     * Has read permission?
     * @return true if the {@link FtpFile} is readable by the user
     */
    boolean isReadable();

    /**
     * Has write permission?
     * @return true if the {@link FtpFile} is writable by the user
     */
    boolean isWritable();

    /**
     * Has delete permission?
     * @return true if the {@link FtpFile} is removable by the user
     */
    boolean isRemovable();

    /**
     * Get the owner name.
     * @return The name of the owner of the {@link FtpFile}
     */
    String getOwnerName();

    /**
     * Get owner group name.
     * @return The name of the group that owns the {@link FtpFile}
     */
    String getGroupName();

    /**
     * Get link count.
     * @return The number of links for the {@link FtpFile}
     */
    int getLinkCount();

    /**
     * Get last modified time in UTC.
     * @return The timestamp of the last modified time for the {@link FtpFile}
     */
    long getLastModified();

    /**
     * Set the last modified time stamp of a file
     * @param time The last modified time, in milliseconds since the epoch. See {@link File#setLastModified(long)}.
     */
    boolean setLastModified(long time);
    
    /**
     * Get file size.
     * @return The size of the {@link FtpFile} in bytes
     */
    long getSize();
    
    /**
     * Returns the physical location or path of the file. It is completely up to 
     * the implementation to return appropriate value based on the file system 
     * implementation.
     *  
     * @return the physical location or path of the file. 
     */
    Object getPhysicalFile();
    
    /**
     * Create directory.
     * @return true if the operation was successful
     */
    boolean mkdir();

    /**
     * Delete file.
     * @return true if the operation was successful
     */
    boolean delete();

    /**
     * Move file.
     * @param destination The target {@link FtpFile} to move the current {@link FtpFile} to
     * @return true if the operation was successful
     */
    boolean move(FtpFile destination);

    /**
     * List file objects. If not a directory or does not exist, null will be
     * returned. Files must be returned in alphabetical order.
     * List must be immutable.
     * @return The {@link List} of {@link FtpFile}s
     */
    List<? extends FtpFile> listFiles();

    /**
     * Create output stream for writing. 
     * @param offset The number of bytes at where to start writing.
     *      If the file is not random accessible,
     *      any offset other than zero will throw an exception.
     * @return An {@link OutputStream} used to write to the {@link FtpFile}
     * @throws IOException 
     */
    OutputStream createOutputStream(long offset) throws IOException;

    /**
     * Create input stream for reading. 
     * @param offset The number of bytes of where to start reading. 
     *          If the file is not random accessible,
     *          any offset other than zero will throw an exception.
     * @return An {@link InputStream} used to read the {@link FtpFile}
     * @throws IOException 
     */
    InputStream createInputStream(long offset) throws IOException;
}
