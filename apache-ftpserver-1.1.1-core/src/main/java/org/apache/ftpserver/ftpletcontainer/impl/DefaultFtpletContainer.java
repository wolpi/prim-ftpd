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

package org.apache.ftpserver.ftpletcontainer.impl;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.FtpletContext;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.apache.ftpserver.ftpletcontainer.FtpletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * This ftplet calls other ftplet methods and returns appropriate return value.
 *
 * <strong><strong>Internal class, do not use directly.</strong></strong>
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultFtpletContainer implements FtpletContainer {

    private final Logger LOG = LoggerFactory
            .getLogger(DefaultFtpletContainer.class);

    private final Map<String, Ftplet> ftplets ;

    public DefaultFtpletContainer() {
        this(new ConcurrentHashMap<String, Ftplet>());
    }
    
    public DefaultFtpletContainer(Map<String, Ftplet> ftplets) {
        this.ftplets = ftplets;
    }

    /**
     * Get Ftplet for the given name.
     */
    public synchronized Ftplet getFtplet(String name) {
        if (name == null) {
            return null;
        }

        return ftplets.get(name);
    }

    public synchronized void init(FtpletContext ftpletContext) throws FtpException {
        for (Entry<String, Ftplet> entry : ftplets.entrySet()) {
            entry.getValue().init(ftpletContext);
        }
    }

    /**
     * @see FtpletContainer#getFtplets()
     */
    public synchronized Map<String, Ftplet> getFtplets() {
        return ftplets;
    }

    /**
     * Destroy all ftplets.
     */
    public void destroy() {
        for (Entry<String, Ftplet> entry : ftplets.entrySet()) {
            try {
                entry.getValue().destroy();
            } catch (Exception ex) {
                LOG.error(entry.getKey() + " :: FtpletHandler.destroy()", ex);
            }
        }
    }

    /**
     * Call ftplet onConnect.
     */
    public FtpletResult onConnect(FtpSession session) throws FtpException,
            IOException {
        FtpletResult retVal = FtpletResult.DEFAULT;
        for (Entry<String, Ftplet> entry : ftplets.entrySet()) {
            retVal = entry.getValue().onConnect(session);
            if (retVal == null) {
                retVal = FtpletResult.DEFAULT;
            }

            // proceed only if the return value is FtpletResult.DEFAULT
            if (retVal != FtpletResult.DEFAULT) {
                break;
            }
        }
        return retVal;
    }

    /**
     * Call ftplet onDisconnect.
     */
    public FtpletResult onDisconnect(FtpSession session) throws FtpException,
            IOException {
        FtpletResult retVal = FtpletResult.DEFAULT;
        for (Entry<String, Ftplet> entry : ftplets.entrySet()) {

            retVal = entry.getValue().onDisconnect(session);
            if (retVal == null) {
                retVal = FtpletResult.DEFAULT;
            }

            // proceed only if the return value is FtpletResult.DEFAULT
            if (retVal != FtpletResult.DEFAULT) {
                break;
            }
        }
        return retVal;
    }

    public FtpletResult afterCommand(FtpSession session, FtpRequest request, FtpReply reply)
            throws FtpException, IOException {
        FtpletResult retVal = FtpletResult.DEFAULT;
        for (Entry<String, Ftplet> entry : ftplets.entrySet()) {

            retVal = entry.getValue().afterCommand(session, request, reply);
            if (retVal == null) {
                retVal = FtpletResult.DEFAULT;
            }

            // proceed only if the return value is FtpletResult.DEFAULT
            if (retVal != FtpletResult.DEFAULT) {
                break;
            }
        }
        return retVal;
    }

    public FtpletResult beforeCommand(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        FtpletResult retVal = FtpletResult.DEFAULT;
        for (Entry<String, Ftplet> entry : ftplets.entrySet()) {

            retVal = entry.getValue().beforeCommand(session, request);
            if (retVal == null) {
                retVal = FtpletResult.DEFAULT;
            }

            // proceed only if the return value is FtpletResult.DEFAULT
            if (retVal != FtpletResult.DEFAULT) {
                break;
            }
        }
        return retVal;
    }

}
