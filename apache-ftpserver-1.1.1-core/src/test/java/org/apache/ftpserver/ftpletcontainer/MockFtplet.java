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

package org.apache.ftpserver.ftpletcontainer;

import java.io.IOException;

import org.apache.ftpserver.ftplet.DefaultFtplet;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.FtpletContext;
import org.apache.ftpserver.ftplet.FtpletResult;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class MockFtplet extends DefaultFtplet {

    protected static MockFtpletCallback callback = new MockFtpletCallback();

    public FtpletContext context;
    public boolean destroyed = false;
    
    @Override
    public void destroy() {
        destroyed = true;
        
        callback.destroy();
    }

    @Override
    public void init(FtpletContext ftpletContext) throws FtpException {
        this.context = ftpletContext;
        
        callback.init(ftpletContext);
    }

    @Override
    public FtpletResult onAppendEnd(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return callback.onAppendEnd(session, request);
    }

    @Override
    public FtpletResult onAppendStart(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return callback.onAppendStart(session, request);
    }

    @Override
    public FtpletResult onConnect(FtpSession session) throws FtpException,
            IOException {
        return callback.onConnect(session);
    }

    @Override
    public FtpletResult onDeleteEnd(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return callback.onDeleteEnd(session, request);
    }

    @Override
    public FtpletResult onDeleteStart(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return callback.onDeleteStart(session, request);
    }

    @Override
    public FtpletResult onDisconnect(FtpSession session) throws FtpException,
            IOException {
        return callback.onDisconnect(session);
    }

    @Override
    public FtpletResult onDownloadEnd(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return callback.onDownloadEnd(session, request);
    }

    @Override
    public FtpletResult onDownloadStart(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return callback.onDownloadStart(session, request);
    }

    @Override
    public FtpletResult onLogin(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return callback.onLogin(session, request);
    }

    @Override
    public FtpletResult onMkdirEnd(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return callback.onMkdirEnd(session, request);
    }

    @Override
    public FtpletResult onMkdirStart(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return callback.onMkdirStart(session, request);
    }

    @Override
    public FtpletResult onRenameEnd(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return callback.onRenameEnd(session, request);
    }

    @Override
    public FtpletResult onRenameStart(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return callback.onRenameStart(session, request);
    }

    @Override
    public FtpletResult onRmdirEnd(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return callback.onRmdirEnd(session, request);
    }

    @Override
    public FtpletResult onRmdirStart(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return callback.onRmdirStart(session, request);
    }

    @Override
    public FtpletResult onSite(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return callback.onSite(session, request);
    }

    @Override
    public FtpletResult onUploadEnd(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return callback.onUploadEnd(session, request);
    }

    @Override
    public FtpletResult onUploadStart(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return callback.onUploadStart(session, request);
    }

    @Override
    public FtpletResult onUploadUniqueEnd(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return callback.onUploadUniqueEnd(session, request);
    }

    @Override
    public FtpletResult onUploadUniqueStart(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        return callback.onUploadUniqueStart(session, request);
    }
}
