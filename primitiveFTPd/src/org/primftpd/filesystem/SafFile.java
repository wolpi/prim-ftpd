package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class SafFile<T> {

    public static final String MIME_TYPE_DIRECTORY = "vnd.android.document/directory";

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final Context context;
    protected final ContentResolver contentResolver;
    protected final Uri startUrl;

    private static final String[] SAF_QUERY_COLUMNS = {
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
    };

    private boolean isDirectory = false;

    private String name;
    private long lastModified;
    private long size;
    private boolean writable;
    private boolean deletable;
    private boolean removable;
    private boolean copyable;
    private boolean movable;
    private boolean renameable;
    private boolean supportsCreate;

    public SafFile(final Context context, ContentResolver contentResolver, Uri startUrl) {
        super();
        this.context = context;
        this.contentResolver = contentResolver;
        this.startUrl = startUrl;

        try {
            Cursor cursor = contentResolver.query(
                    startUrl,
                    SAF_QUERY_COLUMNS,
                    null,
                    null,
                    null);
            try {
                initByCursor(cursor);
            } finally {
                closeQuietly(cursor);
            }
        } catch (UnsupportedOperationException e) {
            // this is probably a directory
            isDirectory = true;
            name = "/";
        } catch (Exception e) {
            final String msg = "[(s)ftpd] Error getting data from SAF: " + e.toString();
            logger.error(msg);
            Handler handler = new Handler(context.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                }
            });
            throw e;
        }
    }

    public SafFile(Context context, ContentResolver contentResolver, Uri startUrl, Cursor cursor) {
        super();
        this.context = context;
        this.contentResolver = contentResolver;
        this.startUrl = startUrl;
        initByCursor(cursor);
    }

    private void initByCursor(Cursor cursor) {
        name = cursor.getString(0);
        lastModified = cursor.getLong(1);
        size = cursor.getLong(2);
        int flags = cursor.getInt(3);
        writable = flagPresent(flags, DocumentsContract.Document.FLAG_SUPPORTS_WRITE);
        deletable = flagPresent(flags, DocumentsContract.Document.FLAG_SUPPORTS_DELETE);
        removable = flagPresent(flags, DocumentsContract.Document.FLAG_SUPPORTS_REMOVE);
        copyable = flagPresent(flags, DocumentsContract.Document.FLAG_SUPPORTS_COPY);
        movable = flagPresent(flags, DocumentsContract.Document.FLAG_SUPPORTS_MOVE);
        renameable = flagPresent(flags, DocumentsContract.Document.FLAG_SUPPORTS_RENAME);
        supportsCreate = flagPresent(flags, DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE);

        String mime = cursor.getString(4);
        logger.trace("{} has mime: {}", name, mime);
        isDirectory = MIME_TYPE_DIRECTORY.equals(mime);
    }

    private boolean flagPresent(int flags, int flag) {
        return ((flags & flag) == flag);
    }

    protected abstract T createFile(Context context, ContentResolver contentResolver, Uri startUrl, Cursor cursor);

    public String getAbsolutePath() {
        logger.trace("getAbsolutePath()");
        return "/";
    }

    public String getName() {
        logger.trace("getName()");
        return name;
    }

    public boolean isHidden() {
        logger.trace("isHidden()");
        return name.charAt(0) == '.';
    }

    public boolean isDirectory() {
        logger.trace("isDirectory()");
        return isDirectory;
    }

    public boolean isFile() {
        logger.trace("isFile()");
        return !isDirectory;
    }

    public boolean doesExist() {
        logger.trace("doesExist()");
        return true;
    }

    public boolean isReadable() {
        logger.trace("isReadable()");
        return true;
    }

    public boolean isWritable() {
        logger.trace("isWritable()");
        return writable;
    }

    public boolean isRemovable() {
        logger.trace("isRemovable()");
        return removable || deletable;
    }

    public int getLinkCount() {
        logger.trace("getLinkCount()");
        return 0;
    }

    public long getLastModified() {
        logger.trace("getLastModified()");
        return lastModified;
    }

    public boolean setLastModified(long time) {
        logger.trace("setLastModified({})", Long.valueOf(time));
        return false;
    }

    public long getSize() {
        logger.trace("getSize()");
        return size;
    }

    public boolean mkdir() {
        logger.trace("mkdir()");
        return false;
    }

    public boolean delete() {
        logger.trace("delete()");
        return false;
    }

    public boolean move(AndroidFile<T> destination) {
        logger.trace("move({})", destination.getAbsolutePath());
        return false;
    }

    public List<T> listFiles() {
        logger.trace("listFiles()");

        List<T> result = new ArrayList<T>(0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    startUrl,
                    DocumentsContract.getTreeDocumentId(startUrl));
            Cursor childCursor = contentResolver.query(
                    childrenUri,
                    SAF_QUERY_COLUMNS,
                    null,
                    null,
                    null);
            try {
                while (childCursor.moveToNext()) {
                    result.add(createFile(context, contentResolver, startUrl, childCursor));
                }
            } finally {
                closeQuietly(childCursor);
            }
        }
        return result;
    }

    public static final int BUFFER_SIZE = 1024 * 1024;

    public OutputStream createOutputStream(long offset) throws IOException {
        logger.trace("createOutputStream({})", offset);

        return null;
    }

    public InputStream createInputStream(long offset) throws IOException {
        logger.trace("createInputStream(), offset: {}", offset);

        return null;
    }

    private void closeQuietly(Cursor cursor) {
        cursor.close();
    }
}
