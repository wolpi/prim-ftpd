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

import java.util.Map;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * String utility methods.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class StringUtils {

    /**
     * This is a string replacement method.
     */
    public final static String replaceString(String source, String oldStr,
            String newStr) {
        StringBuilder sb = new StringBuilder(source.length());
        int sind = 0;
        int cind = 0;
        while ((cind = source.indexOf(oldStr, sind)) != -1) {
            sb.append(source.substring(sind, cind));
            sb.append(newStr);
            sind = cind + oldStr.length();
        }
        sb.append(source.substring(sind));
        return sb.toString();
    }

    /**
     * Replace string
     */
    public final static String replaceString(String source, Object[] args) {
        int startIndex = 0;
        int openIndex = source.indexOf('{', startIndex);
        if (openIndex == -1) {
            return source;
        }

        int closeIndex = source.indexOf('}', startIndex);
        if ((closeIndex == -1) || (openIndex > closeIndex)) {
            return source;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(source.substring(startIndex, openIndex));
        while (true) {
            String intStr = source.substring(openIndex + 1, closeIndex);
            int index = Integer.parseInt(intStr);
            sb.append(args[index]);

            startIndex = closeIndex + 1;
            openIndex = source.indexOf('{', startIndex);
            if (openIndex == -1) {
                sb.append(source.substring(startIndex));
                break;
            }

            closeIndex = source.indexOf('}', startIndex);
            if ((closeIndex == -1) || (openIndex > closeIndex)) {
                sb.append(source.substring(startIndex));
                break;
            }
            sb.append(source.substring(startIndex, openIndex));
        }
        return sb.toString();
    }

    /**
     * Replace string.
     */
    public final static String replaceString(String source,
            Map<String, Object> args) {
        int startIndex = 0;
        int openIndex = source.indexOf('{', startIndex);
        if (openIndex == -1) {
            return source;
        }

        int closeIndex = source.indexOf('}', startIndex);
        if ((closeIndex == -1) || (openIndex > closeIndex)) {
            return source;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(source.substring(startIndex, openIndex));
        while (true) {
            String key = source.substring(openIndex + 1, closeIndex);
            Object val = args.get(key);
            if (val != null) {
                sb.append(val);
            }

            startIndex = closeIndex + 1;
            openIndex = source.indexOf('{', startIndex);
            if (openIndex == -1) {
                sb.append(source.substring(startIndex));
                break;
            }

            closeIndex = source.indexOf('}', startIndex);
            if ((closeIndex == -1) || (openIndex > closeIndex)) {
                sb.append(source.substring(startIndex));
                break;
            }
            sb.append(source.substring(startIndex, openIndex));
        }
        return sb.toString();
    }

    /**
         * This method is used to insert HTML block dynamically
         *
         * @param source the HTML code to be processes
         * @param bReplaceNl if true '\n' will be replaced by <br>
         * @param bReplaceTag if true '<' will be replaced by &lt; and 
         *                          '>' will be replaced by &gt;
         * @param bReplaceQuote if true '\"' will be replaced by &quot; 
         */
    public final static String formatHtml(String source, boolean bReplaceNl,
            boolean bReplaceTag, boolean bReplaceQuote) {

        StringBuilder sb = new StringBuilder();
        int len = source.length();
        for (int i = 0; i < len; i++) {
            char c = source.charAt(i);
            switch (c) {
            case '\"':
                if (bReplaceQuote)
                    sb.append("&quot;");
                else
                    sb.append(c);
                break;

            case '<':
                if (bReplaceTag)
                    sb.append("&lt;");
                else
                    sb.append(c);
                break;

            case '>':
                if (bReplaceTag)
                    sb.append("&gt;");
                else
                    sb.append(c);
                break;

            case '\n':
                if (bReplaceNl) {
                    if (bReplaceTag)
                        sb.append("&lt;br&gt;");
                    else
                        sb.append("<br>");
                } else {
                    sb.append(c);
                }
                break;

            case '\r':
                break;

            case '&':
                sb.append("&amp;");
                break;

            default:
                sb.append(c);
                break;
            }
        }
        return sb.toString();
    }

    /**
     * Pad string object
     */
    public final static String pad(String src, char padChar, boolean rightPad,
            int totalLength) {

        int srcLength = src.length();
        if (srcLength >= totalLength) {
            return src;
        }

        int padLength = totalLength - srcLength;
        StringBuilder sb = new StringBuilder(padLength);
        for (int i = 0; i < padLength; ++i) {
            sb.append(padChar);
        }

        if (rightPad) {
            return src + sb.toString();
        } else {
            return sb.toString() + src;
        }
    }

    /**
     * Get hex string from byte array
     */
    public final static String toHexString(byte[] res) {
        StringBuilder sb = new StringBuilder(res.length << 1);
        for (int i = 0; i < res.length; i++) {
            String digit = Integer.toHexString(0xFF & res[i]);
            if (digit.length() == 1) {
                sb.append('0');
            }
            sb.append(digit);
        }
        return sb.toString().toUpperCase();
    }

    /**
     * Get byte array from hex string
     */
    public final static byte[] toByteArray(String hexString) {
        int arrLength = hexString.length() >> 1;
        byte buff[] = new byte[arrLength];
        for (int i = 0; i < arrLength; i++) {
            int index = i << 1;
            String digit = hexString.substring(index, index + 2);
            buff[i] = (byte) Integer.parseInt(digit, 16);
        }
        return buff;
    }

}
