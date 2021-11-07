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

package org.apache.ftpserver.config.spring;

import java.net.InetAddress;
import java.util.List;
import java.util.Set;

import org.apache.ftpserver.DataConnectionConfiguration;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;
import org.apache.ftpserver.ipfilter.SessionFilter;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.ssl.SslConfiguration;
import org.apache.mina.filter.firewall.Subnet;

/**
 * Used for testing creation of custom listeners from Spring config
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 */
public class MyCustomListener implements Listener {

    private int port;
    
    public void setPort(int port) {
        this.port = port;
    }

    public Set<FtpIoSession> getActiveSessions() {
        return null;
    }

    public DataConnectionConfiguration getDataConnectionConfiguration() {
        return null;
    }

    public int getIdleTimeout() {
        return 0;
    }

    public int getPort() {
        return port;
    }

    public String getServerAddress() {
        return null;
    }

    public SslConfiguration getSslConfiguration() {
        return null;
    }

    public boolean isImplicitSsl() {
        return false;
    }

    public boolean isStopped() {
        return false;
    }

    public boolean isSuspended() {
        return false;
    }

    public void resume() {
        
    }

    public void start(FtpServerContext serverContext) {
        
    }

    public void stop() {
        
    }

    public void suspend() {
        
    }

    public List<InetAddress> getBlockedAddresses() {
        return null;
    }

    public List<Subnet> getBlockedSubnets() {
        return null;
    }

    public SessionFilter getSessionFilter() {
        return null;
    }

}
