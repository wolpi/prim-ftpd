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

package org.apache.ftpserver.ftplet;

import junit.framework.TestCase;

import org.apache.ftpserver.ftplet.DefaultFtpReply;

/**
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 */
public class DefaultFtpReplyTest extends TestCase {

    public void testSingleLineToString() {
        DefaultFtpReply response = new DefaultFtpReply(123, "foo bar");

        assertEquals("123 foo bar\r\n", response.toString());
    }

    public void testSingleLineWithTrailingLineFeedToString() {
        DefaultFtpReply response = new DefaultFtpReply(123, "foo bar\n");

        assertEquals("123 foo bar\r\n", response.toString());
    }

    public void testCarriageReturnToString() {
        DefaultFtpReply response = new DefaultFtpReply(123, "foo \rbar\r\n");

        assertEquals("123 foo bar\r\n", response.toString());
    }
    
    public void testNullToString() {
        DefaultFtpReply response = new DefaultFtpReply(123, (String) null);

        assertEquals("123 \r\n", response.toString());
    }

    public void testMultipleLinesToString() {
        DefaultFtpReply response = new DefaultFtpReply(123, "foo\nbar\nbaz");

        assertEquals("123-foo\r\nbar\r\n123 baz\r\n", response.toString());
    }

    public void testMultipleLinesEndWithNewlineToString() {
        DefaultFtpReply response = new DefaultFtpReply(123, "foo\nbar\nbaz\n");

        assertEquals("123-foo\r\nbar\r\n123 baz\r\n", response.toString());
    }

    public void testArrayLinesToString() {
        DefaultFtpReply response = new DefaultFtpReply(123, new String[] {
                "foo", "bar", "baz" });

        assertEquals("123-foo\r\nbar\r\n123 baz\r\n", response.toString());
    }

    public void testMultipleLinesToString1() {
        DefaultFtpReply response = new DefaultFtpReply(123, "\nfoo\nbar\nbaz");

        assertEquals("123-\r\nfoo\r\nbar\r\n123 baz\r\n", response.toString());
    }

    public void testMultipleLinesToStringSpaceFirst() {
        DefaultFtpReply response = new DefaultFtpReply(123, "foo\n bar\nbaz");

        assertEquals("123-foo\r\n bar\r\n123 baz\r\n", response.toString());
    }

    public void testMultipleLinesToStringThreeNumbers() {
        DefaultFtpReply response = new DefaultFtpReply(123, "foo\n234bar\nbaz");

        assertEquals("123-foo\r\n  234bar\r\n123 baz\r\n", response.toString());
    }

    public void testMultipleLinesToStringThreeNumbersOnFirstLine() {
        DefaultFtpReply response = new DefaultFtpReply(123, "234foo\nbar\nbaz");

        assertEquals("123-234foo\r\nbar\r\n123 baz\r\n", response.toString());
    }

    public void testMultipleLinesToStringThreeNumbersOnLastLine() {
        DefaultFtpReply response = new DefaultFtpReply(123, "foo\nbar\n234baz");

        assertEquals("123-foo\r\nbar\r\n123 234baz\r\n", response.toString());
    }

    public void testMultipleLinesToStringSingleNumberOnLine() {
        DefaultFtpReply response = new DefaultFtpReply(123, "foo\n2bar\nbaz");

        assertEquals("123-foo\r\n2bar\r\n123 baz\r\n", response.toString());
    }

}
