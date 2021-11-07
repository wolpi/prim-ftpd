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

package org.apache.ftpserver.util;

import junit.framework.TestCase;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class RegularExprTest extends TestCase {

    public void testMatchText() {
        RegularExpr expr = new RegularExpr("foo");
        assertTrue(expr.isMatch("foo"));
        assertFalse(expr.isMatch("bar"));
        assertFalse(expr.isMatch("xfoo"));
        assertFalse(expr.isMatch("foox"));
    }

    public void testMatchingTrailingWhitespace() {
        RegularExpr expr = new RegularExpr("foo");
        assertFalse(expr.isMatch("foo "));
        assertFalse(expr.isMatch("foo\n"));
        assertFalse(expr.isMatch("foo\t"));
    }

    public void testMatchingLeadingWhitespace() {
        RegularExpr expr = new RegularExpr("foo");
        assertFalse(expr.isMatch(" foo"));
        assertFalse(expr.isMatch("\nfoo"));
        assertFalse(expr.isMatch("\tfoo"));
    }

    public void testMatchStar() {
        RegularExpr expr = new RegularExpr("*");
        assertTrue(expr.isMatch("foo"));
        assertTrue(expr.isMatch("   "));
        assertTrue(expr.isMatch(""));
        assertTrue(expr.isMatch("\n"));
    }

    public void testMatchStarThenText() {
        RegularExpr expr = new RegularExpr("*foo");
        assertTrue(expr.isMatch("foo"));
        assertTrue(expr.isMatch("xfoo"));
        assertTrue(expr.isMatch("xxxfoo"));
        assertTrue(expr.isMatch("   foo"));
        assertTrue(expr.isMatch("\nfoo"));
        assertFalse(expr.isMatch("bar"));
    }

    public void testMatchTextThenStar() {
        RegularExpr expr = new RegularExpr("foo*");
        assertTrue(expr.isMatch("foo"));
        assertTrue(expr.isMatch("foox"));
        assertTrue(expr.isMatch("fooxxx"));
        assertTrue(expr.isMatch("foo   "));
        assertTrue(expr.isMatch("foo\n"));
        assertFalse(expr.isMatch("bar"));
    }

    public void testMatchQuestionMarkThenText() {
        RegularExpr expr = new RegularExpr("?foo");
        assertFalse(expr.isMatch("foo"));
        assertTrue(expr.isMatch("xfoo"));
        assertFalse(expr.isMatch("xxxfoo"));
        assertTrue(expr.isMatch(" foo"));
        assertTrue(expr.isMatch("\nfoo"));
        assertFalse(expr.isMatch("bar"));
    }

    public void testMatchStarThenQuestionMark() {
        RegularExpr expr = new RegularExpr("foo*?bar");
        assertFalse(expr.isMatch("foobar"));
        assertTrue(expr.isMatch("fooxbar"));
        assertTrue(expr.isMatch("fooxxxbar"));
        assertTrue(expr.isMatch("foo bar"));
        assertTrue(expr.isMatch("foo\nbar"));
        assertFalse(expr.isMatch("foo"));
        assertFalse(expr.isMatch("bar"));
    }

    public void testMatchQuestionMarkThenStar() {
        RegularExpr expr = new RegularExpr("foo?*bar");
        assertFalse(expr.isMatch("foobar"));
        assertTrue(expr.isMatch("fooxbar"));
        assertTrue(expr.isMatch("fooxxxbar"));
        assertTrue(expr.isMatch("foo bar"));
        assertTrue(expr.isMatch("foo\nbar"));
        assertFalse(expr.isMatch("foo"));
        assertFalse(expr.isMatch("bar"));
    }

    public void testMatchDoubleQuestionMark() {
        RegularExpr expr = new RegularExpr("foo??bar");
        assertFalse(expr.isMatch("foobar"));
        assertFalse(expr.isMatch("fooxbar"));
        assertTrue(expr.isMatch("fooxxbar"));
        assertFalse(expr.isMatch("fooxxxbar"));
    }

    public void testMatchChoice() {
        RegularExpr expr = new RegularExpr("foo[abc]bar");
        assertFalse(expr.isMatch("foobar"));
        assertFalse(expr.isMatch("fooxbar"));
        assertTrue(expr.isMatch("fooabar"));
        assertTrue(expr.isMatch("foobbar"));
        assertTrue(expr.isMatch("foocbar"));
        assertFalse(expr.isMatch("fooabbar"));
    }

    public void testMatchNonChoice() {
        RegularExpr expr = new RegularExpr("foo[^abc]bar");
        assertFalse(expr.isMatch("foobar"));
        assertTrue(expr.isMatch("fooxbar"));
        assertFalse(expr.isMatch("fooxxbar"));
        assertFalse(expr.isMatch("fooabar"));
        assertFalse(expr.isMatch("foobbar"));
        assertFalse(expr.isMatch("foocbar"));
        assertFalse(expr.isMatch("fooabbar"));
    }

    /**
     * Star is allowed as a choice character
     */
    public void testMatchChoiceWithStar() {
        RegularExpr expr = new RegularExpr("foo[*]bar");
        assertFalse(expr.isMatch("foobar"));
        assertTrue(expr.isMatch("foo*bar"));
        assertFalse(expr.isMatch("fooxxbar"));
        assertFalse(expr.isMatch("fooabar"));
    }

    /**
     * Question mark is allowed as a choice character
     */
    public void testMatchChoiceWithQuestionMark() {
        RegularExpr expr = new RegularExpr("foo[?]bar");
        assertFalse(expr.isMatch("foobar"));
        assertTrue(expr.isMatch("foo?bar"));
        assertFalse(expr.isMatch("fooxxbar"));
        assertFalse(expr.isMatch("fooabar"));
    }
}