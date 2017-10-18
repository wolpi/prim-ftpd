package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class RoSafFile<T> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final ContentResolver contentResolver;
    protected final Uri startUrl;


    private static final String MIME_TYPE_DIRECTORY = "vnd.android.document/directory";

    static final int CURSOR_INDEX_NAME = 1;
    static final String[] SAF_QUERY_COLUMNS = {
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
    };

    private String documentId;

    private boolean isDirectory = false;
    private String absPath;

    protected String name;
    private long lastModified;
    private long size;
    private boolean writable;
    private boolean readable;
    private boolean exists;

    public RoSafFile(ContentResolver contentResolver, Uri startUrl, String absPath) {
        // this c-tor is to be used for start directory
        super();
        this.contentResolver = contentResolver;
        this.startUrl = startUrl;
        this.absPath = absPath;

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
            name = SafFileSystemView.ROOT_PATH;
        }
    }

    public RoSafFile(ContentResolver contentResolver, Uri startUrl, String docId, String absPath) {
        // this c-tor is to be used for FileSystemView.getFile()
        super();
        this.contentResolver = contentResolver;
        this.startUrl = startUrl;
        this.absPath = absPath;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Uri uri = DocumentsContract.buildDocumentUriUsingTree(
                    startUrl,
                    docId);

            Cursor cursor = contentResolver.query(
                    uri,
                    SAF_QUERY_COLUMNS,
                    null,
                    null,
                    null);
            cursor.moveToNext();
            try {
                initByCursor(cursor);
            } finally {
                closeQuietly(cursor);
            }
        }
    }

    public RoSafFile(ContentResolver contentResolver, Uri startUrl, Cursor cursor, String absPath) {
        // this c-tor is to be used by listFiles()
        super();
        this.contentResolver = contentResolver;
        this.startUrl = startUrl;
        this.absPath = absPath;
        initByCursor(cursor);
    }

    private void initByCursor(Cursor cursor) {
        documentId = cursor.getString(0);
        name = cursor.getString(1);
        lastModified = cursor.getLong(2);
        size = cursor.getLong(3);
        int flags = cursor.getInt(4);
        writable = flagPresent(flags, DocumentsContract.Document.FLAG_SUPPORTS_WRITE);

        readable = true;
        exists = true;

        String mime = cursor.getString(5);
        isDirectory = MIME_TYPE_DIRECTORY.equals(mime);
    }

    private boolean flagPresent(int flags, int flag) {
        return ((flags & flag) == flag);
    }

    protected abstract T createFile(
            ContentResolver contentResolver,
            Uri startUrl,
            Cursor cursor,
            String absPath);

    public String getAbsolutePath() {
        logger.trace("[{}] getAbsolutePath() -> '{}'", name, absPath);
        return absPath;
    }

    public String getName() {
        logger.trace("[{}] getName()", name);
        return name;
    }

    public boolean isDirectory() {
        logger.trace("[{}] isDirectory() -> {}", name, isDirectory);
        return isDirectory;
    }

    public boolean isFile() {
        boolean result = !isDirectory;
        logger.trace("[{}] isFile() -> {}", name, result);
        return result;
    }

    public boolean doesExist() {
        logger.trace("[{}] doesExist() -> {}", name, exists);
        return exists;
    }

    public boolean isReadable() {
        logger.trace("[{}] isReadable() -> {}", name, readable);
        return readable;
    }

    public boolean isWritable() {
        boolean result = false;
        logger.trace("[{}] isWritable() -> {}", name, result);
        return result;
    }

    public boolean isRemovable() {
        boolean result = false;
        logger.trace("[{}] isRemovable() -> {}", name, result);
        return result;
    }

    public long getLastModified() {
        logger.trace("[{}] getLastModified() -> {}", name, lastModified);
        return lastModified;
    }

    public boolean setLastModified(long time) {
        logger.trace("[{}] setLastModified({})", name, time);
        return false;
    }

    public long getSize() {
        logger.trace("[{}] getSize() -> {}", name, size);
        return size;
    }

    public boolean mkdir() {
        logger.trace("[{}] mkdir()", name);
        return false;
    }

    public boolean delete() {
        logger.trace("[{}] delete()", name);
        return false;
    }

    public boolean move(RoSafFile<T> destination) {
        logger.trace("[{}] move({})", name, destination.getAbsolutePath());
        return false;
    }

    public List<T> listFiles() {
        logger.trace("[{}] listFiles()", name);

        List<T> result = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String parentId;
            if (documentId != null) {
                parentId = documentId;
            } else {
                parentId = DocumentsContract.getTreeDocumentId(startUrl);
            }

            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    startUrl,
                    parentId);
            Cursor childCursor = contentResolver.query(
                    childrenUri,
                    SAF_QUERY_COLUMNS,
                    null,
                    null,
                    null);
            try {
                while (childCursor.moveToNext()) {
                    String absPath = this.absPath.endsWith("/")
                            ? this.absPath + childCursor.getString(CURSOR_INDEX_NAME)
                            : this.absPath + "/" + childCursor.getString(CURSOR_INDEX_NAME);
                    result.add(createFile(contentResolver, startUrl, childCursor, absPath));
                }
            } finally {
                closeQuietly(childCursor);
            }
        }
        return result;
    }

    public OutputStream createOutputStream(long offset) throws IOException {
        logger.trace("[{}] createOutputStream(offset: {})", name, offset);

        return null;
    }

    public InputStream createInputStream(long offset) throws IOException {
        logger.trace("[{}] createInputStream(offset: {})", name, offset);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            Uri uri = DocumentsContract.buildDocumentUriUsingTree(
                    startUrl,
                    documentId);

            ParcelFileDescriptor pfd = contentResolver.openFileDescriptor(uri, "r");
            return new FileInputStream(pfd.getFileDescriptor());
        }
        return null;
    }

    private void closeQuietly(Cursor cursor) {
        cursor.close();
    }
}
