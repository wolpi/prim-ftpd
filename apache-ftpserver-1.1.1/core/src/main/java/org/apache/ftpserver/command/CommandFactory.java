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

package org.apache.ftpserver.command;

/**
 * Command factory interface.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface CommandFactory {

    /**
     * Get the command instance.
     * @param commandName The name of the command to create
     * @return The {@link Command} matching the provided name, or
     *   null if no such command exists.
     */
    Command getCommand(String commandName);

    /**
     * Get the registered SITE commands
     * 
     * @return Active site commands, the key is the site command name, used in
     *         FTP sessions as SITE <command name>
     */
    // Map<String, Command> getSiteCommands();
    /**
     * Register SITE commands. The map can replace or append to the default SITE
     * commands provided by FtpServer depending on the value of {@see
     * CommandFactory#isUseDefaultSiteCommands()}
     * 
     * @param siteCommands
     *            Active site commands, the key is the site command name, used
     *            in FTP sessions as SITE <command name>. The value is the
     *            command
     */
    // void setSiteCommands(Map<String, Command> siteCommands);
    /**
     * Should custom site commands append to or replace the default commands
     * provided by FtpServer?. The default is to append
     * 
     * @return true if custom commands should append to the default, false if
     *         they should replace
     */
    // boolean isUseDefaultSiteCommands();
    /**
     * Should custom site commands append to or replace the default commands
     * provided by FtpServer?.
     * 
     * @param useDefaultSiteCommands
     *            true if custom commands should append to the default, false if
     *            they should replace
     */
    // void setUseDefaultSiteCommands(boolean useDefaultSiteCommands);
}
