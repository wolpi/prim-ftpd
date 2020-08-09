package org.primftpd.share;

import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentActivity;

public abstract class AbstractReceiveShareActivity extends FragmentActivity {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    protected void saveUris(File targetPath, List<Uri> uris, List<String> contents, String type) {
        if (uris == null) {
            return;
        }
        for (int i=0; i<uris.size(); i++) {
            Uri uri = uris.get(i);

            String content = null;
            if (contents != null && i < contents.size()) {
                content = contents.get(i);
            }

            saveUri(targetPath, uri, content, type);
        }
    }

    protected String saveUri(File targetPath, Uri uri, String content, String type) {
        if (uri == null) {
            return "";
        }
        FileOutputStream fos = null;
        InputStream is = null;
        try {
            File targetFile = targetFile(uri, content, type, targetPath);
            logger.debug("saving under: {}", targetFile);
            fos = new FileOutputStream(targetFile);
            is = getContentResolver().openInputStream(uri);
            copyStream(is, fos);
            return targetFile.getAbsolutePath();
        } catch (Exception e) {
            logger.warn("could not copy shared data", e);
        } finally {
            try {
                if (fos != null) fos.close();
                if (is != null) is.close();
            } catch (IOException e) {
                logger.warn("could not copy shared data", e);
            }
        }
        return "";
    }

    protected File targetFile(Uri uri, String content, String type, File targetPath) {
        if (!targetPath.isFile()) {
            String filename = filename(uri, content, type, targetPath);
            return new File(targetPath, filename);
        } else {
            return targetPath;
        }
    }

    protected static DateFormat FILENAME_DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
    protected String filename(Uri uri, String content, String type, File targetDir) {
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
                DocumentFile documentFile = DocumentFile.fromSingleUri(this, uri);
                filename = documentFile.getName();
            } catch (Exception e) {
                logger.error("could not resolve content url: " + uri, e);

                // 2nd: use last segment of url
                filename = uri.getLastPathSegment();
            }

            addExtension = !filename.contains(".");
        }
        if (filename == null || (content == null && addExtension)) {
            // if we still don't have a filename or last part of url does not contain a extension
            // use timestamp as filename
            filename = FILENAME_DATEFORMAT.format(new Date());
            addExtension = true;
        }
        String fileExt;
        String basename;
        if (addExtension) {
            // try to add extension base on given mime type
            if (type != null) {
                fileExt = type.contains("/")
                        ? type.substring(type.lastIndexOf('/') + 1, type.length())
                        : type;
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
            fileExt = filename.substring(filename.lastIndexOf('.') +1, filename.length());
            basename = filename.substring(0, filename.lastIndexOf('.'));
        }

        // check if file exists
        boolean exists;
        int counter = 0;
        do {
            String uniquePart = counter == 0 ? "" : "_" + counter;
            String tmpName = basename + uniquePart + "." + fileExt;
            File targetFile = new File(targetDir, tmpName);
            exists = targetFile.exists();
            if (exists) {
                logger.debug("file already exists: {}", targetFile.getAbsolutePath());
                counter++;
            }
        } while (exists);

        // make unique if was existing
        if (counter > 0) {
            filename = basename + "_" + counter + "." + fileExt;
        }

        return filename;
    }

    private static final int BUFFER_SIZE = 4096;

    private void copyStream(InputStream is, OutputStream os) {
        try {
            byte[] bytes = new byte[BUFFER_SIZE];
            for (;;) {
                int count = is.read(bytes, 0, BUFFER_SIZE);
                if (count == -1) {
                    break;
                }
                os.write(bytes, 0, count);
            }
        } catch (Exception e) {
            logger.warn("could not copy stream", e);
        }
    }
}
