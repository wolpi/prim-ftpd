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

/**
 * A more specific type of FtpReply that is sent for commands that act on a
 * single file or directory such as MKD, DELE, RMD etc.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */

public interface FileActionFtpReply extends FtpReply {

	/**
	 * Returns the file (or directory) on which the action was taken 
	 * (e.g. uploaded, created, listed)
	 * 
	 * @return the file on which the action was taken. May return
	 *         <code>null</code>, if the file information is not available.
	 */
	public FtpFile getFile();
}
