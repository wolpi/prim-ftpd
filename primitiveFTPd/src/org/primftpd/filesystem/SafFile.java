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
    private DocumentFile parentDocumentFile;

    static final int CURSOR_INDEX_NAME = 1;
    static final String[] SAF_QUERY_COLUMNS = {
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
    };

    private boolean isDirectory = false;
    private String absPath;

    protected String name;
    private String documentId;
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
        // this c-tor is to be used for start directory
        super();
        logger.trace("new SafFile() with startUrl");
        this.context = context;
        this.contentResolver = contentResolver;
        this.startUrl = startUrl;
        this.absPath = SafFileSystemView.ROOT_PATH;

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

    public SafFile(Context context, ContentResolver contentResolver, Uri startUrl, Cursor cursor, String absPath) {
        // this c-tor is to be used to list existing files
        super();
        logger.trace("new SafFile() with cursor and absPath '{}'", absPath);
        this.context = context;
        this.contentResolver = contentResolver;
        this.startUrl = startUrl;
        this.absPath = absPath;
        initByCursor(cursor);
    }

    public SafFile(
            Context context,
            ContentResolver contentResolver,
            Uri startUrl,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            String absPath) {
        // this c-tor is to be used to access existing files
        super();
        String parentName = parentDocumentFile != null ? parentDocumentFile.getName() : "null";
        logger.trace("new SafFile() with documentFile, parent '{}' and absPath '{}'", parentName, absPath);
        this.context = context;
        this.contentResolver = contentResolver;
        this.startUrl = startUrl;
        this.absPath = absPath;

        this.parentDocumentFile = parentDocumentFile;
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

    public SafFile(
            Context context,
            ContentResolver contentResolver,
            Uri startUrl,
            DocumentFile parentDocumentFile,
            String name,
            String absPath) {
        // this c-tor is to be used to upload new files, create directories or renaming
        super();
        String parentName = parentDocumentFile != null ? parentDocumentFile.getName() : "null";
        logger.trace("new SafFile() with name, parent '{}' and absPath '{}'", parentName, absPath);
        this.context = context;
        this.contentResolver = contentResolver;
        this.startUrl = startUrl;
        this.name = name;
        this.writable = true;
        this.absPath = absPath;

        this.parentDocumentFile = parentDocumentFile;
    }

    private void initByCursor(Cursor cursor) {
        documentId = cursor.getString(0);
        name = cursor.getString(1);
        lastModified = cursor.getLong(2);
        size = cursor.getLong(3);
        int flags = cursor.getInt(4);
        writable = flagPresent(flags, DocumentsContract.Document.FLAG_SUPPORTS_WRITE);
        deletable = flagPresent(flags, DocumentsContract.Document.FLAG_SUPPORTS_DELETE);
        removable = flagPresent(flags, DocumentsContract.Document.FLAG_SUPPORTS_REMOVE);
        //copyable = flagPresent(flags, DocumentsContract.Document.FLAG_SUPPORTS_COPY);
        //movable = flagPresent(flags, DocumentsContract.Document.FLAG_SUPPORTS_MOVE);
        renameable = flagPresent(flags, DocumentsContract.Document.FLAG_SUPPORTS_RENAME);
        //supportsCreate = flagPresent(flags, DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE);

        readable = true;
        exists = true;

        String mime = cursor.getString(5);
        logger.trace("[{}] has id: '{}', mime: '{}'", new Object[]{name, documentId, mime});
        isDirectory = MIME_TYPE_DIRECTORY.equals(mime);
    }

    private boolean flagPresent(int flags, int flag) {
        return ((flags & flag) == flag);
    }

    String getDocumentId() {
        return documentId;
    }

    protected abstract T createFile(Context context, ContentResolver contentResolver, Uri startUrl, Cursor cursor, String absPath);
    protected abstract T createFile(
            Context context,
            ContentResolver contentResolver,
            Uri startUrl,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
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
        logger.trace("[{}] isWritable() -> {}", name, writable);
        return writable;
    }

    public boolean isRemovable() {
        boolean result = removable || deletable;
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
        if (parentDocumentFile != null) {
            return parentDocumentFile.createDirectory(name) != null;
        }
        return false;
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
        if (documentFile == null) {
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
                        result.add(createFile(context, contentResolver, startUrl, childCursor, absPath));
                    }
                } finally {
                    closeQuietly(childCursor);
                }
            }
        } else {
            DocumentFile[] children = documentFile.listFiles();
            for (DocumentFile child : children) {
                String absPath = this.absPath.endsWith("/")
                        ? this.absPath + child.getName()
                        : this.absPath + "/" + child.getName();
                result.add(createFile(context, contentResolver, startUrl, documentFile, child, absPath));
            }
        }
        logger.trace("  [{}] listFiles(): num children: {}", name, Integer.valueOf(result.size()));
        return result;
    }

    public OutputStream createOutputStream(long offset) throws IOException {
        logger.trace("[{}] createOutputStream(offset: {})", name, offset);

        Uri uri;
        if (documentFile != null) {
            // existing files
            uri = documentFile.getUri();
        } else if (parentDocumentFile != null) {
            // new files is sub dir
            DocumentFile docFile = parentDocumentFile.createFile(null, name);
            uri = docFile.getUri();
        } else {
            // new files on root level
            DocumentFile startDocFile = DocumentFile.fromTreeUri(context, startUrl);
            DocumentFile docFile = startDocFile.createFile(null, name);
            uri = docFile.getUri();
        }

        logger.trace("   createOutputStream() uri: {}", uri);
        ParcelFileDescriptor pfd = contentResolver.openFileDescriptor(uri, "w");
        return new FileOutputStream(pfd.getFileDescriptor());
    }

    public InputStream createInputStream(long offset) throws IOException {
        logger.trace("[{}] createInputStream(offset: {})", name, offset);

        if (documentFile != null) {
            return contentResolver.openInputStream(documentFile.getUri());
        }

        return null;
    }

    private void closeQuietly(Cursor cursor) {
        cursor.close();
    }
}
