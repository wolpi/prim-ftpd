package org.primftpd.share;

import android.net.Uri;

import org.primftpd.util.FilenameUnique;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

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
            String filename = FilenameUnique.filename(uri, content, type, targetPath, this);
            return new File(targetPath, filename);
        } else {
            return targetPath;
        }
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
