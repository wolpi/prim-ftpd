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

package org.apache.ftpserver.commands.impl.listing;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import org.apache.ftpserver.command.impl.listing.LISTFileFormater;
import org.apache.ftpserver.ftplet.FtpFile;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
@SuppressWarnings("deprecation")
public class LISTFileFormaterTest extends TestCase {

    private static final Date LAST_MODIFIED_IN_2005 = new Date(105, 1, 2, 3, 4);

    private static final FtpFile TEST_FILE = new MockFileObject();

    private static final String TEST_FILE_FORMAT = "-r--------   1 owner group           13 Feb  2  2005 short\r\n";

    private static final String TEST_DIR_FORMAT = "dr-x------   3 owner group            0 Feb  2  2005 short\r\n";

    public LISTFileFormater formater = new LISTFileFormater();

    public static class MockFileObject implements FtpFile {
        public InputStream createInputStream(long offset) throws IOException {
            return null;
        }

        public OutputStream createOutputStream(long offset) throws IOException {
            return null;
        }

        public boolean delete() {
            return false;
        }

        public boolean doesExist() {
            return false;
        }

        public String getAbsolutePath() {
            return "fullname";
        }

        public String getGroupName() {
            return "group";
        }

        public long getLastModified() {
            return LAST_MODIFIED_IN_2005.getTime();
        }

        public int getLinkCount() {
            return 1;
        }

        public String getOwnerName() {
            return "owner";
        }

        public String getName() {
            return "short";
        }

        public long getSize() {
            return 13;
        }

        public boolean isRemovable() {
            return false;
        }

        public boolean isReadable() {
            return true;
        }

        public boolean isWritable() {
            return false;
        }

        public boolean isDirectory() {
            return false;
        }

        public boolean isFile() {
            return true;
        }

        public boolean isHidden() {
            return false;
        }

        public List<FtpFile> listFiles() {
            return null;
        }

        public boolean mkdir() {
            return false;
        }

        public boolean move(FtpFile destination) {
            return false;
        }

        public boolean setLastModified(long time) {
            return false;
        }
        
        public Object getPhysicalFile() {
        	return "/short";
        }
    }

    public void testSingleFile() {
        assertEquals(TEST_FILE_FORMAT, formater.format(TEST_FILE));
    }

    public void testSingleDir() {
        FtpFile dir = new MockFileObject() {
            @Override
            public int getLinkCount() {
                return 3;
            }

            @Override
            public boolean isDirectory() {
                return true;
            }

            @Override
            public boolean isFile() {
                return false;
            }

        };

        assertEquals(TEST_DIR_FORMAT, formater.format(dir));
    }

}