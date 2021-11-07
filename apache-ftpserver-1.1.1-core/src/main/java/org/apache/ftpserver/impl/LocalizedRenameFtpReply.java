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

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.RenameFtpReply;

/**
 * An implementation of <code>RenameFtpReply</code> that is sent when a file
 * or directory is renamed.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */

public class LocalizedRenameFtpReply extends LocalizedFtpReply implements
	RenameFtpReply {

	/**
	 * The from file
	 */
	private final FtpFile from;

	/**
	 * The to file
	 */
	private final FtpFile to;

	/**
	 * Creates a new instance of <code>LocalizedRenameFtpReply</code>.
	 * 
	 * @param code
	 *            the reply code
	 * @param message
	 *            the detailed message
	 * @param from
	 *            the old file
	 * @param to
	 *            the new file
	 */
	public LocalizedRenameFtpReply(int code, String message, FtpFile from,
		FtpFile to) {
		super(code, message);
		this.from = from;
		this.to = to;
	}

	public FtpFile getFrom() {
		return from;
	}

	public FtpFile getTo() {
		return to;
	}

	/**
	 * Returns the localized reply that contains all details about the rename
	 * operation.
	 * 
	 * @param session
	 *            the FTP session
	 * @param request
	 *            the FTP request
	 * @param context
	 *            the FTP server context
	 * @param code
	 *            the reply code
	 * @param subId
	 *            the sub message ID
	 * @param basicMsg
	 *            the basic message
	 * @param from
	 *            the file or directory as it was before the rename
	 * @param to
	 *            the file or directory after the rename
	 * @return the localized reply
	 */
	public static LocalizedRenameFtpReply translate(FtpIoSession session,
		FtpRequest request, FtpServerContext context, int code, String subId,
		String basicMsg, FtpFile from, FtpFile to) {
		String msg = FtpReplyTranslator.translateMessage(session, request,
			context, code, subId, basicMsg);
		return new LocalizedRenameFtpReply(code, msg, from, to);
	}
}
