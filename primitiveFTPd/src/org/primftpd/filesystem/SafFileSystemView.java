package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import org.apache.ftpserver.ftplet.FtpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SafFileSystemView<T extends SafFile<X>, X> {

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

        return false;
    }

    public T getFile(String file) {
        logger.trace("getFile({})", file);

        return createFile();
    }

    public boolean isRandomAccessible() throws FtpException {
        logger.trace("isRandomAccessible()");

        return true;
    }

    public void dispose() {
        logger.trace("dispose()");
    }
}
