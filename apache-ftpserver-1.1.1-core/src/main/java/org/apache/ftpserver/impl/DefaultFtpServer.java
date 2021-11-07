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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ftpserver.ConnectionConfig;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.command.CommandFactory;
import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.message.MessageResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * This is the starting point of all the servers. It invokes a new listener
 * thread. <code>Server</code> implementation is used to create the server
 * socket and handle client connection.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultFtpServer implements FtpServer {

    private final Logger LOG = LoggerFactory.getLogger(DefaultFtpServer.class);

    private FtpServerContext serverContext;

    private boolean suspended = false;

    private boolean started = false;

    /**
     * Internal constructor, do not use directly. Use {@link FtpServerFactory} instead
     */
    public DefaultFtpServer(final FtpServerContext serverContext) {
        this.serverContext = serverContext;
    }

    /**
     * Start the server. Open a new listener thread.
     * @throws FtpException 
     */
    public void start() throws FtpException {
        if (serverContext == null) {
            // we have already been stopped, can not be restarted
            throw new IllegalStateException("FtpServer has been stopped. Restart is not supported");
        }

        List<Listener> startedListeners = new ArrayList<Listener>();
        
        try {
            Map<String, Listener> listeners = serverContext.getListeners();
            for (Listener listener : listeners.values()) {
                listener.start(serverContext);
                startedListeners.add(listener);
            }
    
            // init the Ftplet container
            serverContext.getFtpletContainer().init(serverContext);
        
            started = true;

            LOG.info("FTP server started");
        } catch(Exception e) {
            // must close listeners that we were able to start
            for(Listener listener : startedListeners) {
                listener.stop();
            }
            
            if(e instanceof FtpException) {
                throw (FtpException)e;
            } else {
                throw (RuntimeException)e;
            }
            
        }
    }

    /**
     * Stop the server. Stopping the server will close completely and 
     * it not supported to restart using {@link #start()}.
     */
    public void stop() {
        if (serverContext == null) {
            // we have already been stopped, ignore
            return;
        }

        // stop all listeners
        Map<String, Listener> listeners = serverContext.getListeners();
        for (Listener listener : listeners.values()) {
            listener.stop();
        }

        // destroy the Ftplet container
        serverContext.getFtpletContainer().destroy();

        // release server resources
        if (serverContext != null) {
            serverContext.dispose();
            serverContext = null;
        }

        started = false;
    }

    /**
     * Get the server status.
     */
    public boolean isStopped() {
        return !started;
    }

    /**
     * Suspend further requests
     */
    public void suspend() {
        if (!started) {
            return;
        }

        LOG.debug("Suspending server");
        // stop all listeners
        Map<String, Listener> listeners = serverContext.getListeners();
        for (Listener listener : listeners.values()) {
            listener.suspend();
        }

        suspended = true;
        LOG.debug("Server suspended");
    }

    /**
     * Resume the server handler
     */
    public void resume() {
        if (!suspended) {
            return;
        }

        LOG.debug("Resuming server");
        Map<String, Listener> listeners = serverContext.getListeners();
        for (Listener listener : listeners.values()) {
            listener.resume();
        }

        suspended = false;
        LOG.debug("Server resumed");
    }

    /**
     * Is the server suspended
     */
    public boolean isSuspended() {
        return suspended;
    }

    /**
     * Get the root server context.
     */
    public FtpServerContext getServerContext() {
        return serverContext;
    }

    /**
     * Get all listeners available one this server
     * 
     * @return The current listeners
     */
    public Map<String, Listener> getListeners() {
        return getServerContext().getListeners();
    }

    /**
     * Get a specific listener identified by its name
     * 
     * @param name
     *            The name of the listener
     * @return The {@link Listener} matching the provided name
     */
    public Listener getListener(final String name) {
        return getServerContext().getListener(name);
    }

    /**
     * Get all {@link Ftplet}s registered at this server
     * 
     * @return All {@link Ftplet}s
     */
    public Map<String, Ftplet> getFtplets() {
        return getServerContext().getFtpletContainer().getFtplets();
    }

    /**
     * Retrieve the user manager used with this server
     * 
     * @return The user manager
     */
    public UserManager getUserManager() {
        return getServerContext().getUserManager();
    }

    /**
     * Retrieve the file system used with this server
     * 
     * @return The {@link FileSystemFactory}
     */
    public FileSystemFactory getFileSystem() {
        return getServerContext().getFileSystemManager();
    }

    /**
     * Retrieve the command factory used with this server
     * 
     * @return The {@link CommandFactory}
     */
    public CommandFactory getCommandFactory() {
        return getServerContext().getCommandFactory();
    }

    /**
     * Retrieve the message resource used with this server
     * 
     * @return The {@link MessageResource}
     */
    public MessageResource getMessageResource() {
        return getServerContext().getMessageResource();
    }

    /**
     * Retrieve the connection configuration this server
     * 
     * @return The {@link MessageResource}
     */
    public ConnectionConfig getConnectionConfig() {
        return getServerContext().getConnectionConfig();
    }
}
