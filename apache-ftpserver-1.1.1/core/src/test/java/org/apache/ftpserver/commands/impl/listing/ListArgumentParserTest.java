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

import junit.framework.TestCase;

import org.apache.ftpserver.command.impl.listing.ListArgument;
import org.apache.ftpserver.command.impl.listing.ListArgumentParser;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class ListArgumentParserTest extends TestCase {

    public void testParseOnlyFile() {
        ListArgument arg = ListArgumentParser.parse("foo");

        assertEquals("foo", arg.getFile());
        assertNull(arg.getPattern());
        assertEquals(0, arg.getOptions().length);
    }

    public void testParseOnlyFileWithDir() {
        ListArgument arg = ListArgumentParser.parse("bar/foo");

        assertEquals("bar/foo", arg.getFile());
        assertNull(arg.getPattern());
        assertEquals(0, arg.getOptions().length);
    }

    public void testParseOnlyPatternWithDir() {
        ListArgument arg = ListArgumentParser.parse("bar/foo*");

        assertEquals("bar/", arg.getFile());
        assertEquals("foo*", arg.getPattern());
        assertEquals(0, arg.getOptions().length);
    }

    public void testParseFileWithSpace() {
        ListArgument arg = ListArgumentParser.parse("foo bar");

        assertEquals("foo bar", arg.getFile());
        assertNull(arg.getPattern());
        assertEquals(0, arg.getOptions().length);
    }

    public void testParseWithTrailingOptions() {
        ListArgument arg = ListArgumentParser.parse("foo -la");

        assertEquals("foo -la", arg.getFile());
        assertNull(arg.getPattern());
        assertEquals(0, arg.getOptions().length);
    }

    public void testParseNullArgument() {
        ListArgument arg = ListArgumentParser.parse(null);

        assertEquals("./", arg.getFile());
        assertNull(arg.getPattern());
        assertEquals(0, arg.getOptions().length);
    }

    public void testParseFileAndOptions() {
        ListArgument arg = ListArgumentParser.parse("-la foo");

        assertEquals("foo", arg.getFile());
        assertNull(arg.getPattern());
        assertEquals(2, arg.getOptions().length);
        assertTrue(arg.hasOption('l'));
        assertTrue(arg.hasOption('a'));
    }

    public void testParseOnlyOptions() {
        ListArgument arg = ListArgumentParser.parse("-la");

        assertEquals("./", arg.getFile());
        assertNull(arg.getPattern());
        assertEquals(2, arg.getOptions().length);
        assertTrue(arg.hasOption('l'));
        assertTrue(arg.hasOption('a'));
    }

    public void testPatternDetection() {
        assertNull(ListArgumentParser.parse("foo").getPattern());
        assertNotNull(ListArgumentParser.parse("foo*").getPattern());
        assertNotNull(ListArgumentParser.parse("f*oo").getPattern());
        assertNotNull(ListArgumentParser.parse("*foo").getPattern());
        assertNotNull(ListArgumentParser.parse("?foo").getPattern());
        assertNotNull(ListArgumentParser.parse("f?oo").getPattern());
        assertNotNull(ListArgumentParser.parse("foo?").getPattern());
        assertNotNull(ListArgumentParser.parse("foo[").getPattern());
        assertNotNull(ListArgumentParser.parse("[foo").getPattern());
    }

    public void testParseSimplePattern() {
        ListArgument arg = ListArgumentParser.parse("foo*");

        assertEquals("./", arg.getFile());
        assertEquals("foo*", arg.getPattern());
        assertEquals(0, arg.getOptions().length);
    }

    public void testParseDirAndPattern() {
        ListArgument arg = ListArgumentParser.parse("bar/foo*");

        assertEquals("bar/", arg.getFile());
        assertEquals("foo*", arg.getPattern());
        assertEquals(0, arg.getOptions().length);
    }

    public void testParsePatternInDir() {
        try {
            ListArgumentParser.parse("bar*/foo");
            fail("IllegalArgumentException must be thrown");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }
}