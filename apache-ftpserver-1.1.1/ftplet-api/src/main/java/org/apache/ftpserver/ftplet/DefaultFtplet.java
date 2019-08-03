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

import java.io.IOException;

/**
 * Default ftplet implementation. All the callback method returns null. It is
 * just an empty implementation. You can derive your ftplet implementation from
 * this class.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultFtplet implements Ftplet {

    public void init(FtpletContext ftpletContext) throws FtpException {
    }

    public void destroy() {
    }

    public FtpletResult onConnect(FtpSession session) throws FtpException,
            IOException {
        return null;
    }

    public FtpletResult onDisconnect(FtpSession session) throws FtpException,
            IOException {
        return null;
    }

    public FtpletResult beforeCommand(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        String command = request.getCommand().toUpperCase();

        if ("DELE".equals(command)) {
            return onDeleteStart(session, request);
        } else if ("STOR".equals(command)) {
            return onUploadStart(session, request);
        } else if ("RETR".equals(command)) {
            return onDownloadStart(session, request);
        } else if ("RMD".equals(command)) {
            return onRmdirStart(session, request);
        } else if ("MKD".equals(command)) {
            return onMkdirStart(session, request);
        } else if ("APPE".equals(command)) {
            return onAppendStart(session, request);
        } else if ("STOU".equals(command)) {
            return onUploadUniqueStart(session, request);
        } else if ("RNTO".equals(command)) {
            return onRenameStart(session, request);
        } else if ("SITE".equals(command)) {
            return onSite(session, request);
        } else {
            // TODO should we call a catch all?
            return null;
        }
    }

    public FtpletResult afterCommand(FtpSession session, FtpRequest request, FtpReply reply)
            throws FtpException, IOException {

        // the reply is ignored for these callbacks
        
        String command = request.getCommand().toUpperCase();

        if ("PASS".equals(command)) {
            return onLogin(session, request);
        } else if ("DELE".equals(command)) {
            return onDeleteEnd(session, request);
        } else if ("STOR".equals(command)) {
            return onUploadEnd(session, request);
        } else if ("RETR".equals(command)) {
            return onDownloadEnd(session, request);
        } else if ("RMD".equals(command)) {
            return onRmdirEnd(session, request);
        } else if ("MKD".equals(command)) {
            return onMkdirEnd(session, request);
        } else if ("APPE".equals(command)) {
            return onAppendEnd(session, request);
        } else if ("STOU".equals(command)) {
            return onUploadUniqueEnd(session, request);
        } else if ("RNTO".equals(command)) {
            return onRenameEnd(session, request);
        } else {
            // TODO should we call a catch all?
            return null;
        }
    }

    /**
     * Override this method to intercept user logins
     * @param session The current {@link FtpSession}
     * @param request The current {@link FtpRequest}
     * @return The action for the container to take
     * @throws FtpException
     * @throws IOException
     */
    public FtpletResult onLogin(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return null;
    }

    /**
     * Override this method to intercept deletions
     * @param session The current {@link FtpSession}
     * @param request The current {@link FtpRequest}
     * @return The action for the container to take
     * @throws FtpException
     * @throws IOException
     */
    public FtpletResult onDeleteStart(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return null;
    }

    /**
     * Override this method to handle deletions after completion
     * @param session The current {@link FtpSession}
     * @param request The current {@link FtpRequest}
     * @return The action for the container to take
     * @throws FtpException
     * @throws IOException
     */
    public FtpletResult onDeleteEnd(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return null;
    }

    /**
     * Override this method to intercept uploads
     * @param session The current {@link FtpSession}
     * @param request The current {@link FtpRequest}
     * @return The action for the container to take
     * @throws FtpException
     * @throws IOException
     */
    public FtpletResult onUploadStart(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return null;
    }

    /**
     * Override this method to handle uploads after completion
     * @param session The current {@link FtpSession}
     * @param request The current {@link FtpRequest}
     * @return The action for the container to take
     * @throws FtpException
     * @throws IOException
     */
    public FtpletResult onUploadEnd(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return null;
    }

    /**
     * Override this method to intercept downloads
     * @param session The current {@link FtpSession}
     * @param request The current {@link FtpRequest}
     * @return The action for the container to take
     * @throws FtpException
     * @throws IOException
     */
    public FtpletResult onDownloadStart(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return null;
    }

    /**
     * Override this method to handle downloads after completion
     * @param session The current {@link FtpSession}
     * @param request The current {@link FtpRequest}
     * @return The action for the container to take
     * @throws FtpException
     * @throws IOException
     */
    public FtpletResult onDownloadEnd(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return null;
    }

    /**
     * Override this method to intercept deletion of directories
     * @param session The current {@link FtpSession}
     * @param request The current {@link FtpRequest}
     * @return The action for the container to take
     * @throws FtpException
     * @throws IOException
     */
    public FtpletResult onRmdirStart(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return null;
    }

    /**
     * Override this method to handle deletion of directories after completion
     * @param session The current {@link FtpSession}
     * @param request The current {@link FtpRequest}
     * @return The action for the container to take
     * @throws FtpException
     * @throws IOException
     */
    public FtpletResult onRmdirEnd(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return null;
    }

    /**
     * Override this method to intercept creation of directories
     * @param session The current {@link FtpSession}
     * @param request The current {@link FtpRequest}
     * @return The action for the container to take
     * @throws FtpException
     * @throws IOException
     */
    public FtpletResult onMkdirStart(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return null;
    }

    /**
     * Override this method to handle creation of directories after completion
     * @param session The current {@link FtpSession}
     * @param request The current {@link FtpRequest}
     * @return The action for the container to take
     * @throws FtpException
     * @throws IOException
     */
    public FtpletResult onMkdirEnd(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return null;
    }

    /**
     * Override this method to intercept file appends
     * @param session The current {@link FtpSession}
     * @param request The current {@link FtpRequest}
     * @return The action for the container to take
     * @throws FtpException
     * @throws IOException
     */
    public FtpletResult onAppendStart(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return null;
    }

    /**
     * Override this method to intercept file appends after completion
     * @param session The current {@link FtpSession}
     * @param request The current {@link FtpRequest}
     * @return The action for the container to take
     * @throws FtpException
     * @throws IOException
     */
    public FtpletResult onAppendEnd(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return null;
    }

    /**
     * Override this method to intercept unique uploads
     * @param session The current {@link FtpSession}
     * @param request The current {@link FtpRequest}
     * @return The action for the container to take
     * @throws FtpException
     * @throws IOException
     */
    public FtpletResult onUploadUniqueStart(FtpSession session,
            FtpRequest request) throws FtpException, IOException {
        return null;
    }

    /**
     * Override this method to handle unique uploads after completion
     * @param session The current {@link FtpSession}
     * @param request The current {@link FtpRequest}
     * @return The action for the container to take
     * @throws FtpException
     * @throws IOException
     */
    public FtpletResult onUploadUniqueEnd(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return null;
    }

    /**
     * Override this method to intercept renames
     * @param session The current {@link FtpSession}
     * @param request The current {@link FtpRequest}
     * @return The action for the container to take
     * @throws FtpException
     * @throws IOException
     */
    public FtpletResult onRenameStart(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return null;
    }

    /**
     * Override this method to handle renames after completion
     * @param session The current {@link FtpSession}
     * @param request The current {@link FtpRequest}
     * @return The action for the container to take
     * @throws FtpException
     * @throws IOException
     */
    public FtpletResult onRenameEnd(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return null;
    }

    /**
     * Override this method to intercept SITE commands
     * @param session The current {@link FtpSession}
     * @param request The current {@link FtpRequest}
     * @return The action for the container to take
     * @throws FtpException
     * @throws IOException
     */
    public FtpletResult onSite(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return null;
    }
}