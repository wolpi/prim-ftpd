package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.support.v4.provider.DocumentFile;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class SafFile<T> {

    private static final String MIME_TYPE_DIRECTORY = "vnd.android.document/directory";

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final Context context;
    private final ContentResolver contentResolver;
    private final Uri startUrl;

    private DocumentFile documentFile;

    private static final String[] SAF_QUERY_COLUMNS = {
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
    };

    private boolean isDirectory = false;

    protected String name;
    private long lastModified;
    private long size;
    private boolean writable;
    private boolean deletable;
    private boolean removable;
    //private boolean copyable;
    //private boolean movable;
    private boolean renameable;
    //private boolean supportsCreate;
    private boolean readable;
    private boolean exists;

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
            readable = true;
            exists = true;
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

    public SafFile(Context context, ContentResolver contentResolver, Uri startUrl, DocumentFile documentFile) {
        super();
        this.context = context;
        this.contentResolver = contentResolver;
        this.startUrl = startUrl;

        this.documentFile = documentFile;

        name = documentFile.getName();
        readable = documentFile.canRead();
        writable = documentFile.canWrite();
        renameable = /*movable = copyable =*/ removable = deletable = writable;
        exists = documentFile.exists();
        isDirectory = documentFile.isDirectory();
        lastModified = documentFile.lastModified();
        size = documentFile.length();
    }

    public SafFile(Context context, ContentResolver contentResolver, Uri startUrl, String name) {
        // this c-tor is to be used to upload new files, create directories or renaming
        super();
        this.context = context;
        this.contentResolver = contentResolver;
        this.startUrl = startUrl;
        this.name = name;
        this.writable = true;
    }

    private void initByCursor(Cursor cursor) {
        name = cursor.getString(0);
        lastModified = cursor.getLong(1);
        size = cursor.getLong(2);
        int flags = cursor.getInt(3);
        writable = flagPresent(flags, DocumentsContract.Document.FLAG_SUPPORTS_WRITE);
        deletable = flagPresent(flags, DocumentsContract.Document.FLAG_SUPPORTS_DELETE);
        removable = flagPresent(flags, DocumentsContract.Document.FLAG_SUPPORTS_REMOVE);
        //copyable = flagPresent(flags, DocumentsContract.Document.FLAG_SUPPORTS_COPY);
        //movable = flagPresent(flags, DocumentsContract.Document.FLAG_SUPPORTS_MOVE);
        renameable = flagPresent(flags, DocumentsContract.Document.FLAG_SUPPORTS_RENAME);
        //supportsCreate = flagPresent(flags, DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE);

        readable = true;
        exists = true;

        String mime = cursor.getString(4);
        logger.trace("{} has mime: {}", name, mime);
        isDirectory = MIME_TYPE_DIRECTORY.equals(mime);
    }

    private boolean flagPresent(int flags, int flag) {
        return ((flags & flag) == flag);
    }

    protected abstract T createFile(Context context, ContentResolver contentResolver, Uri startUrl, Cursor cursor);

    public String getAbsolutePath() {
        logger.trace("[{}] getAbsolutePath()", name);
        // TODO SAF navigation
        return name.charAt(0) == '/' ? name : "/" + name;
    }

    public String getName() {
        logger.trace("[{}] getName()", name);
        return name;
    }

    public boolean isHidden() {
        logger.trace("[{}] isHidden()", name);
        return name.charAt(0) == '.';
    }

    public boolean isDirectory() {
        logger.trace("[{}] isDirectory()", name);
        return isDirectory;
    }

    public boolean isFile() {
        logger.trace("[{}] isFile()", name);
        return !isDirectory;
    }

    public boolean doesExist() {
        logger.trace("[{}] doesExist()", name);
        return exists;
    }

    public boolean isReadable() {
        logger.trace("[{}] isReadable()", name);
        return readable;
    }

    public boolean isWritable() {
        logger.trace("[{}] isWritable(): {}", name, Boolean.valueOf(writable));
        return writable;
    }

    public boolean isRemovable() {
        logger.trace("[{}] isRemovable()", name);
        return removable || deletable;
    }

    public int getLinkCount() {
        logger.trace("[{}] getLinkCount()", name);
        return 0;
    }

    public long getLastModified() {
        logger.trace("[{}] getLastModified()", name);
        return lastModified;
    }

    public boolean setLastModified(long time) {
        logger.trace("[{}] setLastModified({})", name, Long.valueOf(time));
        return false;
    }

    public long getSize() {
        logger.trace("[{}] getSize()", name);
        return size;
    }

    public boolean mkdir() {
        logger.trace("[{}] mkdir()", name);
        DocumentFile startDocFile = DocumentFile.fromTreeUri(context, startUrl);
        return startDocFile.createDirectory(name) != null;
    }

    public boolean delete() {
        logger.trace("[{}] delete()", name);
        if (deletable && documentFile != null) {
            return documentFile.delete();
        }
        return false;
    }

    public boolean move(SafFile<T> destination) {
        logger.trace("[{}] move({})", name, destination.getAbsolutePath());
        if (renameable && documentFile != null) {
            return documentFile.renameTo(destination.getName());
        }
        return false;
    }

    public List<T> listFiles() {
        logger.trace("[{}] listFiles()", name);

        List<T> result = new ArrayList<>(0);
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

    public OutputStream createOutputStream(long offset) throws IOException {
        logger.trace("[{}] createOutputStream({})", name, offset);

        Uri uri;
        if (documentFile != null) {
            // existing files
            uri = documentFile.getUri();
        } else {
            // new files
            DocumentFile startDocFile = DocumentFile.fromTreeUri(context, startUrl);
            DocumentFile docFile = startDocFile.createFile(null, name);
            uri = docFile.getUri();
        }

        ParcelFileDescriptor pfd = contentResolver.openFileDescriptor(uri, "w");
        return new FileOutputStream(pfd.getFileDescriptor());
    }

    public InputStream createInputStream(long offset) throws IOException {
        logger.trace("[{}] createInputStream(), offset: {}", name, offset);

        if (documentFile != null) {
            return contentResolver.openInputStream(documentFile.getUri());
        }

        return null;
    }

    private void closeQuietly(Cursor cursor) {
        cursor.close();
    }
}
