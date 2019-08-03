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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.ftpserver.ConnectionConfig;
import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.command.CommandFactory;
import org.apache.ftpserver.command.CommandFactoryFactory;
import org.apache.ftpserver.filesystem.nativefs.NativeFileSystemFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.apache.ftpserver.ftplet.FtpStatistics;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.ftpletcontainer.FtpletContainer;
import org.apache.ftpserver.ftpletcontainer.impl.DefaultFtpletContainer;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.message.MessageResource;
import org.apache.ftpserver.message.MessageResourceFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.TransferRatePermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.apache.mina.filter.executor.OrderedThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * FTP server configuration implementation. It holds all the components used.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultFtpServerContext implements FtpServerContext {

    private final Logger LOG = LoggerFactory
            .getLogger(DefaultFtpServerContext.class);

    private MessageResource messageResource = new MessageResourceFactory().createMessageResource();

    private UserManager userManager = new PropertiesUserManagerFactory().createUserManager();

    private FileSystemFactory fileSystemManager = new NativeFileSystemFactory();

    private FtpletContainer ftpletContainer = new DefaultFtpletContainer();

    private FtpStatistics statistics = new DefaultFtpStatistics();

    private CommandFactory commandFactory = new CommandFactoryFactory().createCommandFactory();

    private ConnectionConfig connectionConfig = new ConnectionConfigFactory().createConnectionConfig();

    private Map<String, Listener> listeners = new HashMap<String, Listener>();

    private static final List<Authority> ADMIN_AUTHORITIES = new ArrayList<Authority>();
    private static final List<Authority> ANON_AUTHORITIES = new ArrayList<Authority>();
    
    /**
     * The thread pool executor to be used by the server using this context
     */
    private ThreadPoolExecutor threadPoolExecutor = null;
    
    static {
        ADMIN_AUTHORITIES.add(new WritePermission());
        
        ANON_AUTHORITIES.add(new ConcurrentLoginPermission(20, 2));
        ANON_AUTHORITIES.add(new TransferRatePermission(4800, 4800));
    }
    

    public DefaultFtpServerContext() {
        // create the default listener
        listeners.put("default", new ListenerFactory().createListener());
    }

    /**
     * Create default users.
     */
    public void createDefaultUsers() throws Exception {
        UserManager userManager = getUserManager();

        // create admin user
        String adminName = userManager.getAdminName();
        if (!userManager.doesExist(adminName)) {
            LOG.info("Creating user : " + adminName);
            BaseUser adminUser = new BaseUser();
            adminUser.setName(adminName);
            adminUser.setPassword(adminName);
            adminUser.setEnabled(true);

            adminUser.setAuthorities(ADMIN_AUTHORITIES);

            adminUser.setHomeDirectory("./res/home");
            adminUser.setMaxIdleTime(0);
            userManager.save(adminUser);
        }

        // create anonymous user
        if (!userManager.doesExist("anonymous")) {
            LOG.info("Creating user : anonymous");
            BaseUser anonUser = new BaseUser();
            anonUser.setName("anonymous");
            anonUser.setPassword("");

            anonUser.setAuthorities(ANON_AUTHORITIES);

            anonUser.setEnabled(true);

            anonUser.setHomeDirectory("./res/home");
            anonUser.setMaxIdleTime(300);
            userManager.save(anonUser);
        }
    }

    /**
     * Get user manager.
     */
    public UserManager getUserManager() {
        return userManager;
    }

    /**
     * Get file system manager.
     */
    public FileSystemFactory getFileSystemManager() {
        return fileSystemManager;
    }

    /**
     * Get message resource.
     */
    public MessageResource getMessageResource() {
        return messageResource;
    }

    /**
     * Get ftp statistics.
     */
    public FtpStatistics getFtpStatistics() {
        return statistics;
    }

    public void setFtpStatistics(FtpStatistics statistics) {
        this.statistics = statistics;
    }

    /**
     * Get ftplet handler.
     */
    public FtpletContainer getFtpletContainer() {
        return ftpletContainer;
    }

    /**
     * Get the command factory.
     */
    public CommandFactory getCommandFactory() {
        return commandFactory;
    }

    /**
     * Get Ftplet.
     */
    public Ftplet getFtplet(String name) {
        return ftpletContainer.getFtplet(name);
    }

    /**
     * Close all the components.
     */
    public void dispose() {
        listeners.clear();
        ftpletContainer.getFtplets().clear();
        if (threadPoolExecutor != null) {
            LOG.debug("Shutting down the thread pool executor");
            threadPoolExecutor.shutdown();
            try {
                threadPoolExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
            } finally {
                // TODO: how to handle?
            }
        }
    }

    public Listener getListener(String name) {
        return listeners.get(name);
    }

    public void setListener(String name, Listener listener) {
        listeners.put(name, listener);
    }

    public Map<String, Listener> getListeners() {
        return listeners;
    }

    public void setListeners(Map<String, Listener> listeners) {
        this.listeners = listeners;
    }

    public void addListener(String name, Listener listener) {
        listeners.put(name, listener);
    }

    public Listener removeListener(String name) {
        return listeners.remove(name);
    }

    public void setCommandFactory(CommandFactory commandFactory) {
        this.commandFactory = commandFactory;
    }

    public void setFileSystemManager(FileSystemFactory fileSystemManager) {
        this.fileSystemManager = fileSystemManager;
    }

    public void setFtpletContainer(FtpletContainer ftpletContainer) {
        this.ftpletContainer = ftpletContainer;
    }

    public void setMessageResource(MessageResource messageResource) {
        this.messageResource = messageResource;
    }

    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }

    public ConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    public void setConnectionConfig(ConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
    }
    
    public synchronized ThreadPoolExecutor getThreadPoolExecutor() {
        if(threadPoolExecutor == null) {
            int maxThreads = connectionConfig.getMaxThreads();
            if(maxThreads < 1) {
                int maxLogins = connectionConfig.getMaxLogins();
                if(maxLogins > 0) {
                    maxThreads = maxLogins;
                }
                else {
                    maxThreads = 16;
                }
            }
            LOG.debug("Intializing shared thread pool executor with max threads of {}", maxThreads);
            threadPoolExecutor = new OrderedThreadPoolExecutor(maxThreads);
        }
        return threadPoolExecutor;
    }
}
