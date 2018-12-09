package org.primftpd.filesystem;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

class Utils {

    static String absolute(String rel, String workingDir) {
        if (rel.charAt(0) == '/') {
            return rel;
        }
        if ("./".equals(rel) || ".".equals(rel)) {
            return workingDir;
        }
        return workingDir + "/" + rel;
    }

    static String absoluteOrHome(String path, String homeDir) {
        if (".".equals(path) || "/.".equals(path)) {
            return homeDir;
        }
        if (path.charAt(0) != '/') {
            // assume it is relative to home dir, see GH issue #111
            return homeDir + "/" + path;
        }
        return path;
    }

    static List<String> normalizePath(String path) {
        String[] parts = path.split("/");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            if (".".equals(part) || "".equals(part)) {
                continue;
            } else if ("..".equals(part)) {
                if (!result.isEmpty()) {
                    result.remove(result.size() - 1);
                }
                continue;
            } else {
                result.add(part);
            }
        }
        return result;
    }

    static String toPath(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        sb.append("/");
        int i = 0;
        for (String part : parts) {
            sb.append(part);
            if (i < parts.size() - 1) {
                sb.append("/");
            }
            i++;
        }
        return sb.toString();
    }

    static String parent(String path) {
        return path.substring(0, path.lastIndexOf('/'));
    }

    private static final DateFormat TOUCH_DATE_FORMAT = new SimpleDateFormat("yyMMddHHmm.ss");
    static {
        TOUCH_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    static String touchDate(long time) {
        return TOUCH_DATE_FORMAT.format(sshTimeToFileTime(time));
    }

    static long sshTimeToFileTime(long time) {
        return time * 1000;
    }
}
