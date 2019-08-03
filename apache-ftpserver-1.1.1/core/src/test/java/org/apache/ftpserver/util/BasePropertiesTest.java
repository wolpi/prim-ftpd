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

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import junit.framework.TestCase;

import org.apache.ftpserver.ftplet.FtpException;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class BasePropertiesTest extends TestCase {

    public void testGetBoolean() throws FtpException {
        BaseProperties props = new BaseProperties();
        props.setProperty("bool1", "true");
        props.setProperty("bool2", "TRUE");
        props.setProperty("bool3", "True");
        props.setProperty("bool4", "false");
        props.setProperty("bool5", "FALSE");
        props.setProperty("bool6", "False");
        props.setProperty("bool7", "foo");
        props.setProperty("bool8", "");

        assertEquals(true, props.getBoolean("bool1"));
        assertEquals(true, props.getBoolean("bool2"));
        assertEquals(true, props.getBoolean("bool3"));
        assertEquals(false, props.getBoolean("bool4"));
        assertEquals(false, props.getBoolean("bool5"));
        assertEquals(false, props.getBoolean("bool6"));
        assertEquals(false, props.getBoolean("bool7"));
        assertEquals(false, props.getBoolean("bool8"));

        // Unknown key
        try {
            props.getBoolean("foo");
            fail("Must throw FtpException");
        } catch (FtpException e) {
            // ok
        }

        // default values
        assertEquals(true, props.getBoolean("foo", true));
        assertEquals(false, props.getBoolean("foo", false));
        assertEquals(true, props.getBoolean("bool1", false));
        assertEquals(false, props.getBoolean("bool4", true));
    }

    public void testSetBoolean() throws FtpException {
        BaseProperties props = new BaseProperties();
        props.setProperty("b1", true);

        assertEquals(true, props.getBoolean("b1"));
        assertEquals("true", props.getProperty("b1"));
        assertEquals("true", props.getString("b1"));
    }

    public void testGetString() throws FtpException {
        BaseProperties props = new BaseProperties();
        props.setProperty("s1", "bar");

        assertEquals("bar", props.getString("s1"));

        // Unknown value
        try {
            props.getString("foo");
            fail("Must throw FtpException");
        } catch (FtpException e) {
            // ok
        }

        // default values
        assertEquals("bar", props.getString("s1", "baz"));
        assertEquals("baz", props.getString("foo", "baz"));
    }

    public void testSetString() throws FtpException {
        BaseProperties props = new BaseProperties();
        props.setProperty("s1", "bar");

        assertEquals("bar", props.getProperty("s1"));
        assertEquals("bar", props.getString("s1"));
    }

    public void testGetInteger() throws FtpException {
        BaseProperties props = new BaseProperties();
        props.setProperty("int1", "1");
        props.setProperty("int2", "123");
        props.setProperty("int3", "1.23");
        props.setProperty("int4", "foo");
        props.setProperty("int5", "");
        props.setProperty("int6", "99999999999999999");

        assertEquals(1, props.getInteger("int1"));
        assertEquals(123, props.getInteger("int2"));

        try {
            props.getInteger("int3");
            fail("Must throw FtpException");
        } catch (FtpException e) {
            // ok
        }
        try {
            props.getInteger("int4");
            fail("Must throw FtpException");
        } catch (FtpException e) {
            // ok
        }
        try {
            props.getInteger("int5");
            fail("Must throw FtpException");
        } catch (FtpException e) {
            // ok
        }
        try {
            props.getInteger("int6");
            fail("Must throw FtpException");
        } catch (FtpException e) {
            // ok
        }

        // Unknown value
        try {
            props.getInteger("foo");
            fail("Must throw FtpException");
        } catch (FtpException e) {
            // ok
        }

        // default values
        assertEquals(1, props.getInteger("int1", 7));
        assertEquals(7, props.getInteger("int3", 7));
        assertEquals(7, props.getInteger("int4", 7));
        assertEquals(7, props.getInteger("int5", 7));
        assertEquals(7, props.getInteger("int6", 7));
        assertEquals(7, props.getInteger("foo", 7));
    }

    public void testSetInteger() throws FtpException {
        BaseProperties props = new BaseProperties();
        props.setProperty("i1", 1);

        assertEquals(1, props.getInteger("i1"));
        assertEquals("1", props.getProperty("i1"));
        assertEquals("1", props.getString("i1"));
    }

    public void testGetDouble() throws FtpException {
        BaseProperties props = new BaseProperties();
        props.setProperty("d1", "1");
        props.setProperty("d2", "1.23");
        props.setProperty("d3", "1,23");
        props.setProperty("d4", "foo");
        props.setProperty("d5", "");

        assertEquals(1D, props.getDouble("d1"), 0.1);
        assertEquals(1.23D, props.getDouble("d2"), 0.1);

        try {
            props.getDouble("d3");
            fail("Must throw FtpException");
        } catch (FtpException e) {
            // ok
        }
        try {
            props.getDouble("d4");
            fail("Must throw FtpException");
        } catch (FtpException e) {
            // ok
        }
        try {
            props.getDouble("d5");
            fail("Must throw FtpException");
        } catch (FtpException e) {
            // ok
        }

        // Unknown value
        try {
            props.getDouble("foo");
            fail("Must throw FtpException");
        } catch (FtpException e) {
            // ok
        }

        // default values
        assertEquals(1, props.getDouble("d1", 7), 0.1);
        assertEquals(7, props.getDouble("d3", 7), 0.1);
        assertEquals(7, props.getDouble("d4", 7), 0.1);
        assertEquals(7, props.getDouble("d5", 7), 0.1);
        assertEquals(7, props.getDouble("foo", 7), 0.1);
    }

    public void testSetDouble() throws FtpException {
        BaseProperties props = new BaseProperties();
        props.setProperty("d1", 1.23);

        assertEquals(1.23, props.getDouble("d1"), 0.1);
        assertEquals("1.23", props.getProperty("d1"));
        assertEquals("1.23", props.getString("d1"));
    }

    public void testGetLong() throws FtpException {
        BaseProperties props = new BaseProperties();
        props.setProperty("l1", "1");
        props.setProperty("l2", "123");
        props.setProperty("l3", "1.23");
        props.setProperty("l4", "foo");
        props.setProperty("l5", "");
        props.setProperty("l6", "99999999999999999");

        assertEquals(1, props.getLong("l1"));
        assertEquals(123, props.getLong("l2"));
        assertEquals(99999999999999999L, props.getLong("l6"));

        try {
            props.getLong("l3");
            fail("Must throw FtpException");
        } catch (FtpException e) {
            // ok
        }
        try {
            props.getLong("l4");
            fail("Must throw FtpException");
        } catch (FtpException e) {
            // ok
        }
        try {
            props.getLong("l5");
            fail("Must throw FtpException");
        } catch (FtpException e) {
            // ok
        }

        // Unknown value
        try {
            props.getLong("foo");
            fail("Must throw FtpException");
        } catch (FtpException e) {
            // ok
        }

        // default values
        assertEquals(1, props.getLong("l1", 7));
        assertEquals(7, props.getLong("l3", 7));
        assertEquals(7, props.getLong("l4", 7));
        assertEquals(7, props.getLong("l5", 7));
        assertEquals(7, props.getLong("foo", 7));
    }

    public void testSetLong() throws FtpException {
        BaseProperties props = new BaseProperties();
        props.setProperty("l1", 1L);

        assertEquals(1, props.getLong("l1"));
        assertEquals("1", props.getProperty("l1"));
        assertEquals("1", props.getString("l1"));
    }

    public void testGetClass() throws FtpException {
        BaseProperties props = new BaseProperties();
        props.setProperty("c1", "java.lang.String");
        props.setProperty("c2", "foo");

        assertEquals(String.class, props.getClass("c1"));

        try {
            props.getClass("c2");
            fail("Must throw FtpException");
        } catch (FtpException e) {
            // ok
        }

        // Unknown value
        try {
            props.getClass("foo");
            fail("Must throw FtpException");
        } catch (FtpException e) {
            // ok
        }

        // default values
        assertEquals(String.class, props.getClass("c1", Integer.class));
        assertEquals(Integer.class, props.getClass("c2", Integer.class));
        assertEquals(Integer.class, props.getClass("foo", Integer.class));
    }

    public void testSetClass() throws FtpException {
        BaseProperties props = new BaseProperties();
        props.setProperty("c1", String.class);

        assertEquals(String.class, props.getClass("c1"));
        assertEquals("java.lang.String", props.getProperty("c1"));
        assertEquals("java.lang.String", props.getString("c1"));
    }

    public void testGetDate() throws FtpException {
        Date d1 = new Date();
        Date d2 = new Date(100);
        DateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSSzzz");

        BaseProperties props = new BaseProperties();
        props.setProperty("d1", format.format(d1));
        props.setProperty("d2", "foo");

        assertEquals(d1, props.getDate("d1", format));

        try {
            props.getDate("d2", format);
            fail("Must throw FtpException");
        } catch (FtpException e) {
            // ok
        }

        // Unknown value
        try {
            props.getDate("foo", format);
            fail("Must throw FtpException");
        } catch (FtpException e) {
            // ok
        }

        // default values
        assertEquals(d1, props.getDate("d1", format, d2));
        assertEquals(d2, props.getDate("d2", format, d2));
        assertEquals(d2, props.getDate("foo", format, d2));
    }

    public void testSetDate() throws FtpException {
        Date d = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSSzzz");

        BaseProperties props = new BaseProperties();
        props.setProperty("d1", d, format);

        assertEquals(d, props.getDate("d1", format));
        assertEquals(format.format(d), props.getProperty("d1"));
        assertEquals(format.format(d), props.getString("d1"));
    }

    public void testGetDateFormat() throws FtpException {
        SimpleDateFormat format1 = new SimpleDateFormat("yyyyMMddHHmmssSSSzzz");
        SimpleDateFormat format2 = new SimpleDateFormat("yyyy");

        BaseProperties props = new BaseProperties();
        props.setProperty("d1", "yyyyMMddHHmmssSSSzzz");
        props.setProperty("d2", "foo");

        assertEquals(format1, props.getDateFormat("d1"));

        try {
            props.getDateFormat("d2");
            fail("Must throw FtpException");
        } catch (FtpException e) {
            // ok
        }

        // Unknown value
        try {
            props.getDateFormat("foo");
            fail("Must throw FtpException");
        } catch (FtpException e) {
            // ok
        }

        // default values
        assertEquals(format1, props.getDateFormat("d1", format2));
        assertEquals(format2, props.getDateFormat("d2", format2));
        assertEquals(format2, props.getDateFormat("foo", format2));
    }

    public void testSetDateFormat() throws FtpException {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSSzzz");

        BaseProperties props = new BaseProperties();
        props.setProperty("f1", format);

        assertEquals(format, props.getDateFormat("f1"));
        assertEquals("yyyyMMddHHmmssSSSzzz", props.getProperty("f1"));
        assertEquals("yyyyMMddHHmmssSSSzzz", props.getString("f1"));
    }

    public void testGetFile() throws FtpException {
        File file1 = new File("test-tmp/test1.txt").getAbsoluteFile();
        File file2 = new File("test-tmp/test2.txt").getAbsoluteFile();

        BaseProperties props = new BaseProperties();
        props.setProperty("f1", file1.getAbsolutePath());

        assertEquals(file1, props.getFile("f1"));

        // Unknown value
        try {
            props.getFile("foo");
            fail("Must throw FtpException");
        } catch (FtpException e) {
            // ok
        }

        // default values
        assertEquals(file1, props.getFile("f1", file2));
        assertEquals(file2, props.getFile("foo", file2));
    }

    public void testSetFile() throws FtpException {
        File file = new File("test-tmp/test1.txt").getAbsoluteFile();

        BaseProperties props = new BaseProperties();
        props.setProperty("f1", file);

        assertEquals(file, props.getFile("f1"));
        assertEquals(file.getAbsolutePath(), props.getProperty("f1"));
        assertEquals(file.getAbsolutePath(), props.getString("f1"));
    }

    public void testGetInetAddress() throws FtpException, UnknownHostException {
        InetAddress a1 = InetAddress.getByName("1.2.3.4");
        InetAddress a2 = InetAddress.getByName("localhost");
        InetAddress a3 = InetAddress.getByName("1.2.3.5");

        BaseProperties props = new BaseProperties();
        props.setProperty("a1", "1.2.3.4");
        props.setProperty("a2", "localhost");
        props.setProperty("a4", "1.2.3.4.5.6.7.8.9");

        assertEquals(a1, props.getInetAddress("a1"));
        assertEquals(a2, props.getInetAddress("a2"));

        // Unknown value
        try {
            props.getInetAddress("foo");
            fail("Must throw FtpException");
        } catch (FtpException e) {
            // ok
        }

        // Incorrect host name
        try {
            props.getInetAddress("a4");
            fail("Must throw FtpException");
        } catch (FtpException e) {
            // ok
        }

        // default values
        assertEquals(a1, props.getInetAddress("a1", a3));
        assertEquals(a3, props.getInetAddress("foo", a3));
    }

    public void testGetTimeZone() throws FtpException {
        TimeZone tz1 = TimeZone.getTimeZone("PST");
        TimeZone tz2 = TimeZone.getTimeZone("GMT-8:00");
        TimeZone tz3 = TimeZone.getTimeZone("foo");

        BaseProperties props = new BaseProperties();
        props.setProperty("tz1", "PST");
        props.setProperty("tz2", "GMT-8:00");
        props.setProperty("tz3", "foo");

        assertEquals(tz1, props.getTimeZone("tz1"));
        assertEquals(tz2, props.getTimeZone("tz2"));
        assertEquals(tz3, props.getTimeZone("tz3"));

        // Unknown value
        try {
            props.getTimeZone("foo");
            fail("Must throw FtpException");
        } catch (FtpException e) {
            // ok
        }

        // default values
        assertEquals(tz1, props.getTimeZone("tz1", tz2));
        assertEquals(tz2, props.getTimeZone("foo", tz2));
    }

    public void testSetTimeZone() throws FtpException {
        TimeZone tz1 = TimeZone.getTimeZone("PST");

        BaseProperties props = new BaseProperties();
        props.setProperty("tz1", tz1);

        assertEquals(tz1, props.getTimeZone("tz1"));
        assertEquals("PST", props.getProperty("tz1"));
        assertEquals("PST", props.getString("tz1"));
    }
}