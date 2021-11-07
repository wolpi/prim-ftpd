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

package org.apache.ftpserver.util;

import java.io.File;
import java.io.FilenameFilter;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * This is regular expression filename filter.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class FileRegularFilter implements FilenameFilter {

    private RegularExpr regularExpr = null;

    /**
     * Constructor.
     * 
     * @param pattern
     *            regular expression
     */
    public FileRegularFilter(String pattern) {
        if ((pattern == null) || pattern.equals("") || pattern.equals("*")) {
            regularExpr = null;
        } else {
            regularExpr = new RegularExpr(pattern);
        }
    }

    /**
     * Tests if a specified file should be included in a file list.
     * 
     * @param dir
     *            - the directory in which the file was found
     * @param name
     *            - the name of the file.
     */
    public boolean accept(File dir, String name) {
        if (regularExpr == null) {
            return true;
        }
        return regularExpr.isMatch(name);
    }
}
