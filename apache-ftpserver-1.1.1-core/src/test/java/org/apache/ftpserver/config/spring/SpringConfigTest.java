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
import java.util.Map;

import junit.framework.TestCase;

import org.apache.ftpserver.command.CommandFactory;
import org.apache.ftpserver.command.impl.HELP;
import org.apache.ftpserver.command.impl.STAT;
import org.apache.ftpserver.filesystem.nativefs.NativeFileSystemFactory;
import org.apache.ftpserver.impl.DefaultFtpServer;
import org.apache.ftpserver.ipfilter.RemoteIpFilter;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.nio.NioListener;
import org.apache.mina.filter.firewall.Subnet;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.FileSystemResource;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class SpringConfigTest extends TestCase {

    public void test() throws Throwable {
        XmlBeanFactory factory = new XmlBeanFactory(new FileSystemResource(
                "src/test/resources/spring-config/config-spring-1.xml"));

        DefaultFtpServer server = (DefaultFtpServer) factory.getBean("server");

        assertEquals(500, server.getConnectionConfig().getMaxLogins());
        assertEquals(false, server.getConnectionConfig()
                .isAnonymousLoginEnabled());
        assertEquals(123, server.getConnectionConfig().getMaxAnonymousLogins());
        assertEquals(124, server.getConnectionConfig().getMaxLoginFailures());
        assertEquals(125, server.getConnectionConfig().getLoginFailureDelay());

        Map<String, Listener> listeners = server.getServerContext()
                .getListeners();
        assertEquals(3, listeners.size());

        Listener listener = listeners.get("listener0");
        assertNotNull(listener);
        assertTrue(listener instanceof NioListener);
        assertEquals(2222, ((NioListener) listener).getPort());
        assertEquals(InetAddress.getByName("1.2.3.4"), InetAddress.getByName(((NioListener) listener)
                .getServerAddress()));
        assertEquals(100, ((NioListener) listener)
                .getDataConnectionConfiguration().getIdleTime());
        assertTrue(((NioListener) listener)
                .getDataConnectionConfiguration().isActiveEnabled());
        assertTrue(((NioListener) listener)
                .getDataConnectionConfiguration().isImplicitSsl());
        
        assertEquals(InetAddress.getByName("1.2.3.4"), InetAddress.getByName(((NioListener) listener)
        		.getDataConnectionConfiguration().getActiveLocalAddress()) );
        assertEquals("123-125", ((NioListener) listener)
                .getDataConnectionConfiguration().getPassivePorts());
        assertEquals(false, ((NioListener) listener)
                .getDataConnectionConfiguration().isPassiveIpCheck());
        
        RemoteIpFilter filter = (RemoteIpFilter) listener.getSessionFilter();
        assertEquals(3, filter.size());
        assertTrue(filter.contains(new Subnet(InetAddress.getByName("1.2.3.0"), 16)));
        assertTrue(filter.contains(new Subnet(InetAddress.getByName("1.2.4.0"), 16)));
        assertTrue(filter.contains(new Subnet(InetAddress.getByName("1.2.3.4"), 32)));
        listener = listeners.get("listener1");
        assertNotNull(listener);
        assertTrue(listener instanceof MyCustomListener);
        assertEquals(2223, listener.getPort());

        listener = listeners.get("listener2");
        assertNotNull(listener);
        assertTrue(listener instanceof MyCustomListener);
        assertEquals(2224, listener.getPort());

        CommandFactory cf = server.getCommandFactory();
        assertTrue(cf.getCommand("FOO") instanceof HELP);
        assertTrue(cf.getCommand("FOO2") instanceof STAT);

        List<String> languages = server.getServerContext().getMessageResource()
                .getAvailableLanguages();

        assertEquals(2, languages.size());
        assertEquals("en", languages.get(0));
        assertEquals("zh-tw", languages.get(1));
        
        NativeFileSystemFactory fs = (NativeFileSystemFactory) server.getFileSystem();
        assertTrue(fs.isCreateHome());
        assertTrue(fs.isCaseInsensitive());
        
        assertEquals(2, server.getFtplets().size());
        assertEquals(123, ((TestFtplet)server.getFtplets().get("ftplet1")).getFoo());
        assertEquals(223, ((TestFtplet)server.getFtplets().get("ftplet2")).getFoo());
    }
}
