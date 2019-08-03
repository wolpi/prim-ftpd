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

package org.apache.ftpserver.filesystem.nativefs.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.usermanager.impl.WriteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * This class wraps native file object.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NativeFtpFile implements FtpFile {

    private final Logger LOG = LoggerFactory.getLogger(NativeFtpFile.class);

    // the file name with respect to the user root.
    // The path separator character will be '/' and
    // it will always begin with '/'.
    private final String fileName;

    private final File file;

    private final User user;

    /**
     * Constructor, internal do not use directly.
     */
    protected NativeFtpFile(final String fileName, final File file,
            final User user) {
        if (fileName == null) {
            throw new IllegalArgumentException("fileName can not be null");
        }
        if (file == null) {
            throw new IllegalArgumentException("file can not be null");
        }

        if (fileName.length() == 0) {
            throw new IllegalArgumentException("fileName can not be empty");
        } else if (fileName.charAt(0) != '/') {
            throw new IllegalArgumentException(
                    "fileName must be an absolut path");
        }

        this.fileName = fileName;
        this.file = file;
        this.user = user;
    }

    /**
     * Get full name.
     */
    public String getAbsolutePath() {

        // strip the last '/' if necessary
        String fullName = fileName;
        int filelen = fullName.length();
        if ((filelen != 1) && (fullName.charAt(filelen - 1) == '/')) {
            fullName = fullName.substring(0, filelen - 1);
        }

        return fullName;
    }

    /**
     * Get short name.
     */
    public String getName() {

        // root - the short name will be '/'
        if (fileName.equals("/")) {
            return "/";
        }

        // strip the last '/'
        String shortName = fileName;
        int filelen = fileName.length();
        if (shortName.charAt(filelen - 1) == '/') {
            shortName = shortName.substring(0, filelen - 1);
        }

        // return from the last '/'
        int slashIndex = shortName.lastIndexOf('/');
        if (slashIndex != -1) {
            shortName = shortName.substring(slashIndex + 1);
        }
        return shortName;
    }

    /**
     * Is a hidden file?
     */
    public boolean isHidden() {
        return file.isHidden();
    }

    /**
     * Is it a directory?
     */
    public boolean isDirectory() {
        return file.isDirectory();
    }

    /**
     * Is it a file?
     */
    public boolean isFile() {
        return file.isFile();
    }

    /**
     * Does this file exists?
     */
    public boolean doesExist() {
        return file.exists();
    }

    /**
     * Get file size.
     */
    public long getSize() {
        return file.length();
    }

    /**
     * Get file owner.
     */
    public String getOwnerName() {
        return "user";
    }

    /**
     * Get group name
     */
    public String getGroupName() {
        return "group";
    }

    /**
     * Get link count
     */
    public int getLinkCount() {
        return file.isDirectory() ? 3 : 1;
    }

    /**
     * Get last modified time.
     */
    public long getLastModified() {
        return file.lastModified();
    }

    /**
     * {@inheritDoc}
     */
    public boolean setLastModified(long time) {
        return file.setLastModified(time);
    }

    /**
     * Check read permission.
     */
    public boolean isReadable() {
        return file.canRead();
    }

    /**
     * Check file write permission.
     */
    public boolean isWritable() {
        LOG.debug("Checking authorization for " + getAbsolutePath());
        if (user.authorize(new WriteRequest(getAbsolutePath())) == null) {
            LOG.debug("Not authorized");
            return false;
        }

        LOG.debug("Checking if file exists");
        if (file.exists()) {
            LOG.debug("Checking can write: " + file.canWrite());
            return file.canWrite();
        }

        LOG.debug("Authorized");
        return true;
    }

    /**
     * Has delete permission.
     */
    public boolean isRemovable() {

        // root cannot be deleted
        if ("/".equals(fileName)) {
            return false;
        }

        /* Added 12/08/2008: in the case that the permission is not explicitly denied for this file
         * we will check if the parent file has write permission as most systems consider that a file can
         * be deleted when their parent directory is writable.
        */
        String fullName = getAbsolutePath();

        // we check FTPServer's write permission for this file.
        if (user.authorize(new WriteRequest(fullName)) == null) {
            return false;
        }
        // In order to maintain consistency, when possible we delete the last '/' character in the String
        int indexOfSlash = fullName.lastIndexOf('/');
        String parentFullName;
        if (indexOfSlash == 0) {
            parentFullName = "/";
        } else {
            parentFullName = fullName.substring(0, indexOfSlash);
        }

        // we check if the parent FileObject is writable.
        NativeFtpFile parentObject = new NativeFtpFile(parentFullName, file
                .getAbsoluteFile().getParentFile(), user);
        return parentObject.isWritable();
    }

    /**
     * Delete file.
     */
    public boolean delete() {
        boolean retVal = false;
        if (isRemovable()) {
            retVal = file.delete();
        }
        return retVal;
    }

    /**
     * Move file object.
     */
    public boolean move(final FtpFile dest) {
        boolean retVal = false;
        if (dest.isWritable() && isReadable()) {
            File destFile = ((NativeFtpFile) dest).file;

            if (destFile.exists()) {
                // renameTo behaves differently on different platforms
                // this check verifies that if the destination already exists,
                // we fail
                retVal = false;
            } else {
                retVal = file.renameTo(destFile);
            }
        }
        return retVal;
    }

    /**
     * Create directory.
     */
    public boolean mkdir() {
        boolean retVal = false;
        if (isWritable()) {
            retVal = file.mkdir();
        }
        return retVal;
    }

    /**
     * Get the physical file object.
     */
    public File getPhysicalFile() {
        return file;
    }

    /**
     * List files. If not a directory or does not exist, null will be returned.
     */
    public List<FtpFile> listFiles() {

        // is a directory
        if (!file.isDirectory()) {
            return null;
        }

        // directory - return all the files
        File[] files = file.listFiles();
        if (files == null) {
            return null;
        }

        // make sure the files are returned in order
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return f1.getName().compareTo(f2.getName());
            }
        });

        // get the virtual name of the base directory
        String virtualFileStr = getAbsolutePath();
        if (virtualFileStr.charAt(virtualFileStr.length() - 1) != '/') {
            virtualFileStr += '/';
        }

        // now return all the files under the directory
        FtpFile[] virtualFiles = new FtpFile[files.length];
        for (int i = 0; i < files.length; ++i) {
            File fileObj = files[i];
            String fileName = virtualFileStr + fileObj.getName();
            virtualFiles[i] = new NativeFtpFile(fileName, fileObj, user);
        }

        return Collections.unmodifiableList(Arrays.asList(virtualFiles));
    }

    /**
     * Create output stream for writing.
     */
    public OutputStream createOutputStream(final long offset)
            throws IOException {

        // permission check
        if (!isWritable()) {
            throw new IOException("No write permission : " + file.getName());
        }

        // create output stream
        final RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.setLength(offset);
        raf.seek(offset);

        // The IBM jre needs to have both the stream and the random access file
        // objects closed to actually close the file
        return new FileOutputStream(raf.getFD()) {
            @Override
            public void close() throws IOException {
                super.close();
                raf.close();
            }
        };
    }

    /**
     * Create input stream for reading.
     */
    public InputStream createInputStream(final long offset) throws IOException {

        // permission check
        if (!isReadable()) {
            throw new IOException("No read permission : " + file.getName());
        }

        // move to the appropriate offset and create input stream
        final RandomAccessFile raf = new RandomAccessFile(file, "r");
        raf.seek(offset);

        // The IBM jre needs to have both the stream and the random access file
        // objects closed to actually close the file
        return new FileInputStream(raf.getFD()) {
            @Override
            public void close() throws IOException {
                super.close();
                raf.close();
            }
        };
    }

    /**
     * Implements equals by comparing getCanonicalPath() for the underlying file instabnce.
     * Ignores the fileName and User fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NativeFtpFile) {
            String thisCanonicalPath;
            String otherCanonicalPath;
            try {
                thisCanonicalPath = this.file.getCanonicalPath();
                otherCanonicalPath = ((NativeFtpFile) obj).file
                        .getCanonicalPath();
            } catch (IOException e) {
                throw new RuntimeException("Failed to get the canonical path",
                        e);
            }

            return thisCanonicalPath.equals(otherCanonicalPath);
        }
        return false;
    }
    
	@Override
	public int hashCode() {
		try {
			return file.getCanonicalPath().hashCode();
		} catch (IOException e) {
			return 0;
		}
	}
}
