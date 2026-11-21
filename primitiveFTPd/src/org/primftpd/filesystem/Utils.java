package org.primftpd.filesystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.SimpleTimeZone;

public class Utils {

    protected static Logger logger = LoggerFactory.getLogger(Utils.class);

    static String absolute(String rel, String workingDir) {
        if (rel == null || rel.isEmpty()) {
            return workingDir;
        }
        if (rel.charAt(0) == '/') {
            return rel;
        }
        if ("./".equals(rel) || ".".equals(rel)) {
            return workingDir;
        }
        return workingDir + "/" + rel;
    }

    static String absoluteOrHome(String path, String homeDir) {
        if (path == null || path.isEmpty() || ".".equals(path) || "/.".equals(path)) {
            return homeDir;
        }
        // if homeDir == / -> stay relative (needed for virtual folders)
        if (path.charAt(0) != '/' && !"/".equals(homeDir)) {
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
        // strip trailing slashes, but keep root
        int end = path.length();
        while (end > 1 && path.charAt(end - 1) == '/') {
            end--;
        }
        int idx = path.lastIndexOf('/', end - 1);
        if (idx <= 0) {
            // top level entry (or no slash at all) -> parent is root
            return "/";
        }
        return path.substring(0, idx);
    }

    /**
     * Skips exactly the given number of bytes or until end of stream is reached.
     * InputStream.skip() is allowed to skip less bytes than requested.
     * Closes the stream if skipping fails.
     */
    static void skipFully(InputStream is, long offset) throws IOException {
        try {
            long remaining = offset;
            while (remaining > 0) {
                long skipped = is.skip(remaining);
                if (skipped <= 0) {
                    // skip() may return 0 without being at end of stream
                    if (is.read() < 0) {
                        break;
                    }
                    skipped = 1;
                }
                remaining -= skipped;
            }
        } catch (IOException | RuntimeException e) {
            try {
                is.close();
            } catch (IOException e2) {
                // ignore
            }
            throw e;
        }
    }

    /**
     * Positions the stream at offset and wraps it into a buffered stream.
     * Skipping is done on the raw stream as file based streams can seek instead of reading.
     */
    static InputStream openAtOffset(InputStream is, long offset) throws IOException {
        skipFully(is, offset);
        return new BufferedInputStream(is, TracingBufferedOutputStream.BUFFER_SIZE);
    }

    private static final DateFormat TOUCH_DATE_FORMAT = new SimpleDateFormat("yyMMddHHmm.ss");
    static {
        TOUCH_DATE_FORMAT.setTimeZone(new SimpleTimeZone(0, "No-TZ"));
    }
    static String touchDate(long time) {
        // SimpleDateFormat is not thread-safe
        synchronized (TOUCH_DATE_FORMAT) {
            return TOUCH_DATE_FORMAT.format(time);
        }
    }

    public static final boolean RUN_TESTS = false;
}
