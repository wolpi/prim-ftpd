package org.primftpd.util;

import android.content.Context;
import android.net.Uri;

import org.primftpd.share.TargetDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.documentfile.provider.DocumentFile;

public class FilenameUnique {

    protected static Logger logger = LoggerFactory.getLogger(FilenameUnique.class);

    protected static DateFormat FILENAME_DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");

    private static final int MAX_FILENAME_LENGTH = 120;

    static String sanitize(String filename) {
        StringBuilder sb = new StringBuilder(filename.length());
        for (int i = 0; i < filename.length() && sb.length() < MAX_FILENAME_LENGTH; i++) {
            char c = filename.charAt(i);
            if (c == '/' || c == '\\' || c == ':' || c < ' ') {
                sb.append('_');
            } else {
                sb.append(c);
            }
        }
        if (filename.length() > MAX_FILENAME_LENGTH) {
            // keep file extension if name had to be shortened
            int dotIndex = filename.lastIndexOf('.');
            int extLength = filename.length() - dotIndex;
            if (dotIndex > 0 && extLength > 1 && extLength <= 10) {
                String ext = sanitizeExtension(filename.substring(dotIndex));
                sb.setLength(MAX_FILENAME_LENGTH - ext.length());
                sb.append(ext);
            }
        }
        String result = sb.toString().trim();
        if (result.isEmpty() || ".".equals(result) || "..".equals(result)) {
            synchronized (FILENAME_DATEFORMAT) {
                result = FILENAME_DATEFORMAT.format(new Date());
            }
        }
        return result;
    }

    private static String sanitizeExtension(String ext) {
        return ext.replace('/', '_').replace('\\', '_').replace(':', '_');
    }

    public static String filename(Uri uri, String content, String type, TargetDir targetDir, Context context) {
        String filename = null;
        boolean addExtension = false;
        if (content != null) {
            // if we got content use that as filename
            filename = content;
            addExtension = true;
        }
        if (filename == null && uri != null) {
            // if we don't got content derive filename from url

            // 1st: try to resolve content url
            try {
                DocumentFile documentFile = DocumentFile.fromSingleUri(context, uri);
                filename = documentFile.getName();
            } catch (Exception e) {
                logger.error("could not resolve content url: {}", uri, e);

                // 2nd: use last segment of url
                filename = uri.getLastPathSegment();
            }

            if (filename != null) {
                addExtension = !filename.contains(".");
            }
        }
        if (filename == null || (content == null && addExtension)) {
            // if we still don't have a filename or last part of url does not contain a extension
            // use timestamp as filename
            // DateFormat is not thread-safe
            synchronized (FILENAME_DATEFORMAT) {
                filename = FILENAME_DATEFORMAT.format(new Date());
            }
            addExtension = true;
        }
        // name is provided by other apps, it must not be able to point to another directory
        filename = sanitize(filename);
        String fileExt;
        String basename;
        if (addExtension) {
            // try to add extension base on given mime type
            if (type != null) {
                fileExt = type.contains("/")
                        ? type.substring(type.lastIndexOf('/') + 1)
                        : type;
                // for http downloads fileType may contain something like: text/html; charset=UTF-8
                fileExt = fileExt.contains(";")
                        ? fileExt.substring(0, fileExt.lastIndexOf(';'))
                        : fileExt;
                if ("plain".equals(fileExt)) {
                    fileExt = "txt";
                } else if ("jpeg".equals(fileExt)) {
                    fileExt = "jpg";
                }
            } else {
                fileExt = "";
            }
            basename = filename;
            filename = basename + "." + fileExt;
        } else {
            fileExt = filename.substring(filename.lastIndexOf('.') +1);
            basename = filename.substring(0, filename.lastIndexOf('.'));
        }

        // check if file exists
        boolean exists;
        int counter = 0;
        do {
            String uniquePart = counter == 0 ? "" : "_" + counter;
            String tmpName = basename + uniquePart + "." + fileExt;
            exists = targetDir.doesFilenameExist(tmpName);
            if (exists) {
                logger.debug("file already exists in target-dir: {}", tmpName);
                counter++;
            }
        } while (exists);

        // make unique if was existing
        if (counter > 0) {
            filename = basename + "_" + counter + "." + fileExt;
        }

        return filename;
    }
}
