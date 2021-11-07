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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * Standard date related utility methods.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DateUtils {

    private static final TimeZone TIME_ZONE_UTC = TimeZone.getTimeZone("UTC");

    private final static String[] MONTHS = { "Jan", "Feb", "Mar", "Apr", "May",
            "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

    /*
     * Creates the DateFormat object used to parse/format
     * dates in FTP format.
     */
    private static final ThreadLocal<DateFormat> FTP_DATE_FORMAT = new ThreadLocal<DateFormat>() {

        @Override
        protected DateFormat initialValue() {
            DateFormat df=new SimpleDateFormat("yyyyMMddHHmmss");
            df.setLenient(false);
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
            return df;
        }
        
    };
    
    /**
     * Get unix style date string.
     */
    public final static String getUnixDate(long millis) {
        if (millis < 0) {
            return "------------";
        }

        StringBuilder sb = new StringBuilder(16);
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(millis);

        // month
        sb.append(MONTHS[cal.get(Calendar.MONTH)]);
        sb.append(' ');

        // day
        int day = cal.get(Calendar.DATE);
        if (day < 10) {
            sb.append(' ');
        }
        sb.append(day);
        sb.append(' ');

        long sixMonth = 15811200000L; // 183L * 24L * 60L * 60L * 1000L;
        long nowTime = System.currentTimeMillis();
        if (Math.abs(nowTime - millis) > sixMonth) {

            // year
            int year = cal.get(Calendar.YEAR);
            sb.append(' ');
            sb.append(year);
        } else {

            // hour
            int hh = cal.get(Calendar.HOUR_OF_DAY);
            if (hh < 10) {
                sb.append('0');
            }
            sb.append(hh);
            sb.append(':');

            // minute
            int mm = cal.get(Calendar.MINUTE);
            if (mm < 10) {
                sb.append('0');
            }
            sb.append(mm);
        }
        return sb.toString();
    }

    /**
     * Get ISO 8601 timestamp.
     */
    public final static String getISO8601Date(long millis) {
        StringBuilder sb = new StringBuilder(19);
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(millis);

        // year
        sb.append(cal.get(Calendar.YEAR));

        // month
        sb.append('-');
        int month = cal.get(Calendar.MONTH) + 1;
        if (month < 10) {
            sb.append('0');
        }
        sb.append(month);

        // date
        sb.append('-');
        int date = cal.get(Calendar.DATE);
        if (date < 10) {
            sb.append('0');
        }
        sb.append(date);

        // hour
        sb.append('T');
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if (hour < 10) {
            sb.append('0');
        }
        sb.append(hour);

        // minute
        sb.append(':');
        int min = cal.get(Calendar.MINUTE);
        if (min < 10) {
            sb.append('0');
        }
        sb.append(min);

        // second
        sb.append(':');
        int sec = cal.get(Calendar.SECOND);
        if (sec < 10) {
            sb.append('0');
        }
        sb.append(sec);

        return sb.toString();
    }

    /**
     * Get FTP date.
     */
    public final static String getFtpDate(long millis) {
        StringBuilder sb = new StringBuilder(20);
        
        // MLST should use UTC
        Calendar cal = new GregorianCalendar(TIME_ZONE_UTC);
        cal.setTimeInMillis(millis);
        

        // year
        sb.append(cal.get(Calendar.YEAR));

        // month
        int month = cal.get(Calendar.MONTH) + 1;
        if (month < 10) {
            sb.append('0');
        }
        sb.append(month);

        // date
        int date = cal.get(Calendar.DATE);
        if (date < 10) {
            sb.append('0');
        }
        sb.append(date);

        // hour
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if (hour < 10) {
            sb.append('0');
        }
        sb.append(hour);

        // minute
        int min = cal.get(Calendar.MINUTE);
        if (min < 10) {
            sb.append('0');
        }
        sb.append(min);

        // second
        int sec = cal.get(Calendar.SECOND);
        if (sec < 10) {
            sb.append('0');
        }
        sb.append(sec);

        // millisecond
        sb.append('.');
        int milli = cal.get(Calendar.MILLISECOND);
        if (milli < 100) {
            sb.append('0');
        }
        if (milli < 10) {
            sb.append('0');
        }
        sb.append(milli);
        return sb.toString();
    }
    /*
     *  Parses a date in the format used by the FTP commands 
     *  involving dates(MFMT, MDTM)
     */
    public final static Date parseFTPDate(String dateStr) throws ParseException{
        return FTP_DATE_FORMAT.get().parse(dateStr);
        
    }
    
}
