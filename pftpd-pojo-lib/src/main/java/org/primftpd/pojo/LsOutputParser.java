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

    public LsOutputBean parseLine(String line) {
        logger.trace("ls output: '{}'", line);

        if (line == null) {
            return null;
        }

        List<String> parts = new ArrayList<>(15);
        StringTokenizer tokenizer = new StringTokenizer(line, " ");
        while (tokenizer.hasMoreTokens()) {
            parts.add(tokenizer.nextToken());
        }

        // link count is optional, makes min size 7
        if (parts.size() < 7) {
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
        int linkCountOffset = 1;
        try {
            builder.setLinkCount(Long.parseLong(parts.get(1)));
        } catch (NumberFormatException e) {
            linkCountOffset = 0;
        }

        // user & group
        builder.setUser(parts.get(1 + linkCountOffset));
        builder.setGroup(parts.get(2 + linkCountOffset));

        // size
        try {
            builder.setSize(Long.parseLong(parts.get(3 + linkCountOffset)));
        } catch (NumberFormatException e) {
            return null;
        }

        // date
        String dateStr;
        DateFormat dateFormat;
        Date date;
        long offset = 0;
        String firstDateCol = parts.get(4 + linkCountOffset);
        int dateEndIndex;
        if (firstDateCol.length() == 10) {
            dateStr = firstDateCol + " " + parts.get(5 + linkCountOffset);
            dateFormat = DATE_FORMAT_1;
            dateEndIndex = 5 + linkCountOffset;
        } else {
            String lastDateCol = parts.get(6 + linkCountOffset);
            dateStr = firstDateCol + " " + parts.get(5 + linkCountOffset) + " " + lastDateCol;
            if (lastDateCol.contains(":")) {
                dateFormat = DATE_FORMAT_3;
                offset = CURRENT_YEAR_MILLIS;
            } else {
                dateFormat = DATE_FORMAT_2;
            }
            dateEndIndex = 6 + linkCountOffset;
        }
        try {
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            date = dateFormat.parse(dateStr);
            if (offset > 0) {
                date = new Date(date.getTime() + offset);
            }
        } catch (Exception e) {
            date = new Date(0);
        }
        builder.setDate(date);

        // name
        int nameEndIndex = 0;
        if (!builder.isLink()) {
            nameEndIndex = parts.size();
        } else {
            boolean found = false;
            int targetStartIndex = parts.size();
            for (int i = dateEndIndex + 1; i<parts.size(); i++) {
                String part = parts.get(i);
                if ("->".equals(part)) {
                    if (!found) {
                        found = true;
                        nameEndIndex = i;
                        targetStartIndex = i;
                    } else {
                        return null;
                    }
                }
            }
            StringBuilder sb = new StringBuilder();
            String prefix = "";
            for (int i = targetStartIndex + 1; i<parts.size(); i++) {
                sb.append(prefix);
                sb.append(parts.get(i));
                prefix = " ";
            }
            builder.setLinkTarget(sb.toString());
        }
        StringBuilder sb = new StringBuilder();
        String prefix = "";
        for (int i = dateEndIndex + 1; i<nameEndIndex; i++) {
            sb.append(prefix);
            sb.append(parts.get(i));
            prefix = " ";
        }
        builder.setName(sb.toString());

        return builder.build();
    }

    private static final DateFormat DATE_FORMAT_1 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private static final DateFormat DATE_FORMAT_2 = new SimpleDateFormat("dd. MMM yyyy");
    private static final DateFormat DATE_FORMAT_3 = new SimpleDateFormat("dd. MMM HH:mm");

    static final long CURRENT_YEAR_MILLIS;
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
