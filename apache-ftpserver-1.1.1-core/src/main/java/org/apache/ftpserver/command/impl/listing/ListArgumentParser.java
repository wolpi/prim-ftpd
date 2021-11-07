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

import java.util.StringTokenizer;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * Parses a list argument (e.g. for LIST or NLST) into a {@link ListArgument}
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ListArgumentParser {

    /**
     * Parse the argument
     * 
     * @param argument
     *            The argument string
     * @return The parsed argument
     * @throws IllegalArgumentException
     *             If the argument string is incorrectly formated
     */
    public static ListArgument parse(String argument) {
        String file = "./";
        String options = "";
        String pattern = "*";

        // find options and file name (may have regular expression)
        if (argument != null) {
            argument = argument.trim();
            StringBuilder optionsSb = new StringBuilder(4);
            StringBuilder fileSb = new StringBuilder(16);
            StringTokenizer st = new StringTokenizer(argument, " ", true);
            while (st.hasMoreTokens()) {
                String token = st.nextToken();

                if (fileSb.length() != 0) {
                    // file name started - append to file name buffer
                    fileSb.append(token);
                } else if (token.equals(" ")) {
                    // delimiter and file not started - ignore
                    continue;
                } else if (token.charAt(0) == '-') {
                    // token and file name not started - append to options
                    // buffer
                    if (token.length() > 1) {
                        optionsSb.append(token.substring(1));
                    }
                } else {
                    // filename - append to the filename buffer
                    fileSb.append(token);
                }
            }

            if (fileSb.length() != 0) {
                file = fileSb.toString();
            }
            options = optionsSb.toString();
        }

        int slashIndex = file.lastIndexOf('/');
        if (slashIndex == -1) {
            if (containsPattern(file)) {
                pattern = file;
                file = "./";
            }
        } else if (slashIndex != (file.length() - 1)) {
            String after = file.substring(slashIndex + 1);

            if (containsPattern(after)) {
                pattern = file.substring(slashIndex + 1);
                file = file.substring(0, slashIndex + 1);
            }

            if (containsPattern(file)) {
                throw new IllegalArgumentException(
                        "Directory path can not contain regular expression");
            }
        }

        if ("*".equals(pattern) || "".equals(pattern)) {
            pattern = null;
        }

        return new ListArgument(file, pattern, options.toCharArray());
    }

    private static boolean containsPattern(String file) {
        return file.indexOf('*') > -1 || file.indexOf('?') > -1
                || file.indexOf('[') > -1;

    }
}
