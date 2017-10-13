package org.primftpd.pojo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;

public class LsOutputParser {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public List<LsOutputBean> parse(InputStream stream) throws IOException {
        List<LsOutputBean> result = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line = reader.readLine();
        while (line != null) {
            LsOutputBean bean = parseLine(line);
            if (bean != null) {
                result.add(bean);
            }
            line = reader.readLine();
        }
        return result;
    }

    protected LsOutputBean parseLine(String line) {
        logger.trace("ls output: '{}'", line);

        if (line == null) {
            return null;
        }

        List<String> parts = new ArrayList<>(15);
        StringTokenizer tokenizer = new StringTokenizer(line, " ");
        while (tokenizer.hasMoreTokens()) {
            parts.add(tokenizer.nextToken());
        }

        if (parts.size() < 8) {
            return null;
        }
        String firstPart = parts.get(0);
        if (firstPart.length() < 10 || firstPart.length() > 11) {
            return null;
        }

        LsOutputBuilder builder = new LsOutputBuilder();

        // type
        if (firstPart.charAt(0) == '-') {
            builder.setFile(true);
        }
        if (firstPart.charAt(0) == 'd') {
            builder.setDir(true);
        }
        if (firstPart.charAt(0) == 'l') {
            builder.setLink(true);
        }

        // permission
        if (firstPart.charAt(1) == 'r') {
            builder.setUserReadable(true);
        }
        if (firstPart.charAt(2) == 'w') {
            builder.setUserWritable(true);
        }
        if (firstPart.charAt(3) == 'x') {
            builder.setUserExecutable(true);
        }
        if (firstPart.charAt(4) == 'r') {
            builder.setGroupReadable(true);
        }
        if (firstPart.charAt(5) == 'w') {
            builder.setGroupWritable(true);
        }
        if (firstPart.charAt(6) == 'x') {
            builder.setGroupExecutable(true);
        }
        if (firstPart.charAt(7) == 'r') {
            builder.setOtherReadable(true);
        }
        if (firstPart.charAt(8) == 'w') {
            builder.setOtherWritable(true);
        }
        if (firstPart.charAt(9) == 'x') {
            builder.setOtherExecutable(true);
        }
        if (firstPart.length() > 10 && firstPart.charAt(10) == '.') {
            builder.setHasAcl(true);
        }

        // link count
        try {
            builder.setLinkCount(Long.parseLong(parts.get(1)));
        } catch (NumberFormatException e) {
            return null;
        }

        // user & group
        builder.setUser(parts.get(2));
        builder.setGroup(parts.get(3));

        // size
        try {
            builder.setSize(Long.parseLong(parts.get(4)));
        } catch (NumberFormatException e) {
            return null;
        }

        // date
        String dateStr;
        DateFormat dateFormat;
        Date date;
        long offset = 0;
        String firstDateCol = parts.get(5);
        if (firstDateCol.length() == 10) {
            dateStr = firstDateCol + " " + parts.get(6);
            dateFormat = DATE_FORMAT_1;
        } else {
            String lastDateCol = parts.get(7);
            dateStr = firstDateCol + " " + parts.get(6) + " " + lastDateCol;
            if (lastDateCol.contains(":")) {
                dateFormat = DATE_FORMAT_3;
                offset = CURRENT_YEAR_MILLIS;
            } else {
                dateFormat = DATE_FORMAT_2;
            }
        }
        try {
            date = dateFormat.parse(dateStr);
            if (offset > 0) {
                date = new Date(date.getTime() + offset);
            }
        } catch (Exception e) {
            date = new Date(0);
        }
        builder.setDate(date);

        // name
        if (!builder.isLink()) {
            builder.setName(parts.get(parts.size() -1));
        } else {
            builder.setName(parts.get(parts.size() -3));
            builder.setLinkTarget(parts.get(parts.size() -1));
        }

        return builder.build();
    }

    private static final DateFormat DATE_FORMAT_1 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private static final DateFormat DATE_FORMAT_2 = new SimpleDateFormat("dd. MMM yyyy");
    private static final DateFormat DATE_FORMAT_3 = new SimpleDateFormat("dd. MMM HH:mm");

    private static final long CURRENT_YEAR_MILLIS;
    static {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        CURRENT_YEAR_MILLIS = cal.getTimeInMillis();
    }
}
