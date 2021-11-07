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

import java.util.Map;

import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.impl.DefaultFtpServer;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class FtpletsConfigTest extends SpringConfigTestTemplate {

    private static final String USER_FILE_PATH = "src/test/resources/users.properties";
    
    private Map<String, Ftplet> createFtplets(String config) {
        DefaultFtpServer server = (DefaultFtpServer) createServer("<ftplets>" + config + "</ftplets>");

        return server.getFtplets();
    }

    public void testFtplet() throws Throwable {
        Map<String, Ftplet> ftplets = createFtplets("<ftplet name=\"foo\">" +
        		"<beans:bean class=\"" + TestFtplet.class.getName() +  "\">" +
                        "<beans:property name=\"foo\" value=\"123\" />" +
                        "</beans:bean></ftplet>");

        assertEquals(1, ftplets.size());
        assertEquals(123, ((TestFtplet)ftplets.get("foo")).getFoo());
    }

    public void testFtpletMap() throws Throwable {
        Map<String, Ftplet> ftplets = createFtplets("<beans:map>" + 
                "<beans:entry key=\"foo\">" +
                        "<beans:bean class=\"" + TestFtplet.class.getName() +  "\">" +
                        "<beans:property name=\"foo\" value=\"123\" />" +
                        "</beans:bean>" +
                        "</beans:entry></beans:map>");

        assertEquals(1, ftplets.size());
        assertEquals(123, ((TestFtplet)ftplets.get("foo")).getFoo());
    }

}
