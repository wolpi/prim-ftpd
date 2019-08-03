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

package org.apache.ftpserver.command.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.ftpserver.command.Command;
import org.apache.ftpserver.command.CommandFactory;
import org.apache.ftpserver.command.CommandFactoryFactory;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * Command factory to return appropriate command implementation depending on the
 * FTP request command string.
 *
 * <strong><strong>Internal class, do not use directly.</strong></strong>
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultCommandFactory implements CommandFactory {

    public DefaultCommandFactory() {
        this(new HashMap<String, Command>());
    }

    /**
     * Internal constructor, use {@link CommandFactoryFactory} instead
     */
    public DefaultCommandFactory(Map<String, Command> commandMap) {
        this.commandMap = commandMap;
    }

    private final Map<String, Command> commandMap;

    /**
     * Get command. Returns null if not found.
     */
    public Command getCommand(final String cmdName) {
        if (cmdName == null || cmdName.equals("")) {
            return null;
        }
        String upperCaseCmdName = cmdName.toUpperCase();
        return commandMap.get(upperCaseCmdName);
    }
}
