package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.support.v4.provider.DocumentFile;

import org.apache.ftpserver.ftplet.FtpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SafFileSystemView<T extends SafFile<X>, X> {

    protected final static String ROOT_PATH = "/";

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final Context context;
    protected final ContentResolver contentResolver;
    protected final Uri startUrl;

    public SafFileSystemView(Context context, ContentResolver contentResolver, Uri startUrl) {
        this.context = context;
        this.contentResolver = contentResolver;
        this.startUrl = startUrl;
    }

    protected abstract T createFile();
    protected abstract T createFile(DocumentFile documentFile);
    protected abstract T createFile(String name);

    private T createHomeDirObj() {
        return createFile();
    }

    public T getHomeDirectory() {
        logger.trace("getHomeDirectory()");

        return createHomeDirObj();
    }

    public T getWorkingDirectory() {
        logger.trace("getWorkingDirectory()");

        return createHomeDirObj();
    }

    public boolean changeWorkingDirectory(String dir) {
        logger.trace("changeWorkingDirectory({})", dir);

        // TODO SAF navigation

        return false;
    }

    public T getFile(String file) {
        logger.trace("getFile({})", file);

        // TODO SAF navigation

        if (!ROOT_PATH.equals(file)) {
            if (startsWithRoot(file)) {
                file = removeRoot(file);
            }
            DocumentFile startDocFile = DocumentFile.fromTreeUri(context, startUrl);
            DocumentFile docFile = startDocFile.findFile(file);
            if (docFile != null) {
                return createFile(docFile);
            } else {
                return createFile(file);
            }
        }

        return createFile();
    }

    public boolean isRandomAccessible() throws FtpException {
        logger.trace("isRandomAccessible()");

        return true;
    }

    public void dispose() {
        logger.trace("dispose()");
    }

    protected static boolean startsWithRoot(String name) {
        return name.charAt(0) == ROOT_PATH.charAt(0);
    }

    protected static String removeRoot(String name) {
        return name.substring(1, name.length());
    }
}
