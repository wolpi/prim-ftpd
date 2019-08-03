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

package org.apache.ftpserver.commands.impl;

import junit.framework.TestCase;

import org.apache.ftpserver.command.Command;
import org.apache.ftpserver.command.CommandFactory;
import org.apache.ftpserver.command.CommandFactoryFactory;
import org.apache.ftpserver.command.impl.NOOP;
import org.apache.ftpserver.command.impl.STOR;

/**
 * 
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 */
public class DefaultCommandFactoryTest extends TestCase {

    public void testReturnFromDefaultUpper() {
        CommandFactory factory = new CommandFactoryFactory().createCommandFactory();
        Command command = factory.getCommand("STOR");

        assertNotNull(command);
        assertTrue(command instanceof STOR);
    }

    public void testReturnFromDefaultLower() {
        CommandFactory factory = new CommandFactoryFactory().createCommandFactory();
        Command command = factory.getCommand("stor");

        assertNotNull(command);
        assertTrue(command instanceof STOR);
    }

    public void testReturnFromDefaultUnknown() {
        CommandFactory factory = new CommandFactoryFactory().createCommandFactory();
        Command command = factory.getCommand("dummy");

        assertNull(command);
    }

    public void testOverride() {
        CommandFactoryFactory factoryFactory = new CommandFactoryFactory();
        factoryFactory.addCommand("stor", new NOOP());

        CommandFactory factory = factoryFactory.createCommandFactory();

        Command command = factory.getCommand("Stor");

        assertTrue(command instanceof NOOP);
    }

    public void testAppend() {
        CommandFactoryFactory factoryFactory = new CommandFactoryFactory();
        factoryFactory.addCommand("foo", new NOOP());

        CommandFactory factory = factoryFactory.createCommandFactory();

        assertTrue(factory.getCommand("FOO") instanceof NOOP);
        assertTrue(factory.getCommand("stor") instanceof STOR);
    }

    public void testAppendWithoutDefault() {
        CommandFactoryFactory factoryFactory = new CommandFactoryFactory();
        factoryFactory.addCommand("foo", new NOOP());
        factoryFactory.setUseDefaultCommands(false);

        CommandFactory factory = factoryFactory.createCommandFactory();
        
        assertTrue(factory.getCommand("FOO") instanceof NOOP);
        assertNull(factory.getCommand("stor"));
    }
}
