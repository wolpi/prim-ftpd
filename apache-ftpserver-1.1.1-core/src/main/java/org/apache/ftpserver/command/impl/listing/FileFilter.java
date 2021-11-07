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

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * Interface for selecting files based on some critera.
 * 
 * @see java.io.FileFilter
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface FileFilter {

    /**
     * Decide if the {@link FtpFile} should be selected
     * 
     * @param file
     *            The {@link FtpFile}
     * @return true if the {@link FtpFile} was selected
     */
    boolean accept(FtpFile file);

}
