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
import org.apache.ftpserver.util.DateUtils;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * Formats files according to the MLST specification
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class MLSTFileFormater implements FileFormater {

    private static final String[] DEFAULT_TYPES = new String[] { "Size",
            "Modify", "Type" };

    private final static char[] NEWLINE = { '\r', '\n' };

    private String[] selectedTypes = DEFAULT_TYPES;

    /**
     * @param selectedTypes
     *            The types to show in the formated file
     */
    public MLSTFileFormater(String[] selectedTypes) {
        if (selectedTypes != null) {
            this.selectedTypes = selectedTypes.clone();
        }
    }

    /**
     * @see FileFormater#format(FtpFile)
     */
    public String format(FtpFile file) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < selectedTypes.length; ++i) {
            String type = selectedTypes[i];
            if (type.equalsIgnoreCase("size")) {
                sb.append("Size=");
                sb.append(String.valueOf(file.getSize()));
                sb.append(';');
            } else if (type.equalsIgnoreCase("modify")) {
                String timeStr = DateUtils.getFtpDate(file.getLastModified());
                sb.append("Modify=");
                sb.append(timeStr);
                sb.append(';');
            } else if (type.equalsIgnoreCase("type")) {
                if (file.isFile()) {
                    sb.append("Type=file;");
                } else if (file.isDirectory()) {
                    sb.append("Type=dir;");
                }
            } else if (type.equalsIgnoreCase("perm")) {
                sb.append("Perm=");
                if (file.isReadable()) {
                    if (file.isFile()) {
                        sb.append('r');
                    } else if (file.isDirectory()) {
                        sb.append('e');
                        sb.append('l');
                    }
                }
                if (file.isWritable()) {
                    if (file.isFile()) {
                        sb.append('a');
                        sb.append('d');
                        sb.append('f');
                        sb.append('w');
                    } else if (file.isDirectory()) {
                        sb.append('f');
                        sb.append('p');
                        sb.append('c');
                        sb.append('m');
                    }
                }
                sb.append(';');
            }
        }
        sb.append(' ');
        sb.append(file.getName());

        sb.append(NEWLINE);

        return sb.toString();
    }
}
