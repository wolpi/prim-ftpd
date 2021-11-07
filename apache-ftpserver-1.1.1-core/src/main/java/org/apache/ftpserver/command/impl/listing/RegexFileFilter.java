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
package org.apache.ftpserver.command.impl.listing;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.util.RegularExpr;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * Selects files which short name matches a regular expression
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class RegexFileFilter implements FileFilter {

    private final RegularExpr regex;

    private final FileFilter wrappedFilter;

    /**
     * Constructor with a regular expression
     * 
     * @param regex
     *            The regular expression to select by
     */
    public RegexFileFilter(String regex) {
        this(regex, null);
    }

    /**
     * Constructor with a wrapped filter, allows for chaining filters
     * 
     * @param regex
     *            The regular expression to select by
     * @param wrappedFilter
     *            The {@link FileFilter} to wrap
     */
    public RegexFileFilter(String regex, FileFilter wrappedFilter) {
        this.regex = new RegularExpr(regex);
        this.wrappedFilter = wrappedFilter;
    }

    /**
     * @see FileFilter#accept(FtpFile)
     */
    public boolean accept(FtpFile file) {
        if (wrappedFilter != null && !wrappedFilter.accept(file)) {
            return false;
        }

        return regex.isMatch(file.getName());
    }

}
