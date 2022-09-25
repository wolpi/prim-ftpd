package org.primftpd.util;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

public class FileSizeUtils {
    public static String humanReadableByteCountSI(long bytes) {
        return  humanReadableByteCountSI(bytes, "");
    }
    public static String humanReadableByteCountSI(long bytes, String suffix) {
        if (bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format("%.1f %2$cB%3$s", bytes / 1000.0, ci.current(), suffix);
    }
}
