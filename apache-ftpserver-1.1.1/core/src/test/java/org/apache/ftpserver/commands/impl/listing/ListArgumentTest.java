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

package org.apache.ftpserver.commands.impl.listing;

import java.util.Arrays;

import junit.framework.TestCase;

import org.apache.ftpserver.command.impl.listing.ListArgument;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class ListArgumentTest extends TestCase {

    private static final char[] OPTIONS = new char[] { 'a', 'b', 'c' };

    private static final char[] OPTIONS_EMPTY = new char[] {};

    public void testFilePatternAndOptions() {
        ListArgument arg = new ListArgument("bar", "foo", OPTIONS);

        assertEquals("bar", arg.getFile());
        assertEquals("foo", arg.getPattern());

        assertTrue(Arrays.equals(OPTIONS, arg.getOptions()));
    }

    public void testArgumentAndEmptyOptions() {
        ListArgument arg = new ListArgument("bar", "foo", OPTIONS_EMPTY);

        assertEquals(0, arg.getOptions().length);
    }

    public void testArgumentAndNullOptions() {
        ListArgument arg = new ListArgument("bar", "foo", null);

        assertNotNull(arg.getOptions());
        assertEquals(0, arg.getOptions().length);
    }

    public void testNullFile() {
        ListArgument arg = new ListArgument(null, "foo", null);

        assertNull(arg.getFile());
        assertEquals("foo", arg.getPattern());
    }

    public void testNullPattern() {
        ListArgument arg = new ListArgument("bar", null, null);

        assertEquals("bar", arg.getFile());
        assertNull(arg.getPattern());
    }

}