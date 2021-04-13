package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

import org.primftpd.events.ClientActionEvent;
import org.primftpd.services.PftpdService;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class RoSafFile<T> extends AbstractFile {

    private final ContentResolver contentResolver;
    protected final Uri startUrl;

    private String documentId;
    private boolean writable;
    private boolean deletable;

    private static final String MIME_TYPE_DIRECTORY = "vnd.android.document/directory";

    static final int CURSOR_INDEX_NAME = 1;
    static final String[] SAF_QUERY_COLUMNS = {
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_FLAGS,
    };

    public RoSafFile(
            ContentResolver contentResolver,
            Uri startUrl,
            String absPath,
            PftpdService pftpdService) {
        // this c-tor is to be used for start directory
        super(
                absPath,
                null,
                0,
                0,
                false,
                false,
                false,
                pftpdService);
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
            name = SafFileSystemView.ROOT_PATH;
        }
    }

    public RoSafFile(
            ContentResolver contentResolver,
            Uri startUrl,
            String docId,
            String absPath,
            boolean exists,
            PftpdService pftpdService) {
        // this c-tor is to be used for FileSystemView.getFile()
        super(
                absPath,
                null,
                0,
                0,
                false,
                exists,
                false,
                pftpdService);
        this.contentResolver = contentResolver;
        this.startUrl = startUrl;

        if (exists) {
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
        } else {
            name = docId;
            writable = true;
        }
    }

    public RoSafFile(
            ContentResolver contentResolver,
            Uri startUrl,
            Cursor cursor,
            String absPath,
            PftpdService pftpdService) {
        // this c-tor is to be used by listFiles()
        super(
                absPath,
                null,
                0,
                0,
                false,
                false,
                false,
                pftpdService);
        this.contentResolver = contentResolver;
        this.startUrl = startUrl;
        initByCursor(cursor);
    }

    private void initByCursor(Cursor cursor) {
        documentId = cursor.getString(0);
        name = cursor.getString(1);
        lastModified = cursor.getLong(2);
        size = cursor.getLong(3);

        logger.trace("    initByCursor, doc id: {}, name: {}", documentId, name);

        readable = true;
        exists = true;

        String mime = cursor.getString(4);
        isDirectory = MIME_TYPE_DIRECTORY.equals(mime);

        int flags = cursor.getInt(5);
        writable = flagPresent(flags, DocumentsContract.Document.FLAG_SUPPORTS_WRITE);
        deletable = flagPresent(flags, DocumentsContract.Document.FLAG_SUPPORTS_DELETE);
    }

    private boolean flagPresent(int flags, int flag) {
        return ((flags & flag) == flag);
    }

    protected abstract T createFile(
            ContentResolver contentResolver,
            Uri startUrl,
            Cursor cursor,
            String absPath,
            PftpdService pftpdService);

    @Override
    public ClientActionEvent.Storage getClientActionStorage() {
        return ClientActionEvent.Storage.ROSAF;
    }

    public boolean isFile() {
        boolean result = !isDirectory;
        logger.trace("[{}] isFile() -> {}", name, result);
        return result;
    }

    public boolean isWritable() {
        // TODO writing with SAF cursor/uri api
        //boolean result = writable;
        boolean result = false;
        logger.trace("[{}] isWritable() -> {}", name, result);
        return result;
    }

    public boolean isRemovable() {
        // TODO writing with SAF cursor/uri api
        //boolean result = deletable;
        boolean result = false;
        logger.trace("[{}] isRemovable() -> {}", name, result);
        return result;
    }

    public boolean setLastModified(long time) {
        logger.trace("[{}] setLastModified({})", name, time);
        return false;
    }

    public boolean mkdir() {
        logger.trace("[{}] mkdir()", name);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Uri parentUri;
            if (documentId != null) {
                parentUri = DocumentsContract.buildDocumentUriUsingTree(startUrl, documentId);
            } else {
                parentUri = startUrl;
            }
            logger.trace("mkdir(): parent uri: '{}'", parentUri);
            try {
                Uri newDirUri = DocumentsContract.createDocument(contentResolver, parentUri, MIME_TYPE_DIRECTORY, name);
                return newDirUri != null;
            } catch (FileNotFoundException e) {
                logger.error("could not create dir " + name, e);
            }
        }
        return false;
    }

    public boolean delete() {
        logger.trace("[{}] delete()", name);
        // TODO writing with SAF cursor/uri api
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            Uri docUri = DocumentsContract.buildDocumentUriUsingTree(startUrl, documentId);
//            logger.trace("delete(): docUri: '{}'", docUri);
//            try {
//                return DocumentsContract.deleteDocument(contentResolver, docUri);
//            } catch (FileNotFoundException e) {
//                logger.error("could not delete " + name, e);
//            }
//        }
        return false;
    }

    public boolean move(RoSafFile<T> destination) {
        logger.trace("[{}] move({})", name, destination.getAbsolutePath());
        // TODO writing with SAF cursor/uri api
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            Uri docUri = DocumentsContract.buildDocumentUriUsingTree(startUrl, documentId);
//            logger.trace("move(): docUri: '{}'", docUri);
//            try {
//                Uri newNameUri = DocumentsContract.renameDocument(contentResolver, docUri, destination.getName());
//                return newNameUri != null;
//            } catch (FileNotFoundException e) {
//                logger.error("could not rename " + name, e);
//            }
//        }
        return false;
    }

    public List<T> listFiles() {
        logger.trace("[{}] listFiles()", name);
        postClientAction(ClientActionEvent.ClientAction.LIST_DIR);

        List<T> result = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String parentId;
            if (documentId != null) {
                parentId = documentId;
            } else {
                parentId = DocumentsContract.getTreeDocumentId(startUrl);
            }

            logger.trace("  building children uri for doc: {}, parent: {}", documentId, parentId);
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    startUrl,
                    documentId);
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
                    result.add(createFile(contentResolver, startUrl, childCursor, absPath, pftpdService));
                }
            } finally {
                closeQuietly(childCursor);
            }
        }
        // log result
        for (T obj : result) {
            if (obj instanceof AbstractFile) {
                logger.trace("  returning child '{}'", ((AbstractFile)obj).getName());
            } else {
                logger.trace("  returning child of class '{}'", obj.getClass().getName());
            }
        }
        if (result.isEmpty()) {
            logger.trace("  no children");
        }
        // return result
        return result;
    }

    public OutputStream createOutputStream(long offset) throws IOException {
        logger.trace("[{}] createOutputStream(offset: {})", name, offset);
        postClientAction(ClientActionEvent.ClientAction.UPLOAD);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Uri uri = DocumentsContract.buildDocumentUriUsingTree(
                    startUrl,
                    documentId);
            return new TracingBufferedOutputStream(contentResolver.openOutputStream(uri), logger);
        }
        return null;
    }

    public InputStream createInputStream(long offset) throws IOException {
        logger.trace("[{}] createInputStream(offset: {})", name, offset);
        postClientAction(ClientActionEvent.ClientAction.DOWNLOAD);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Uri uri = DocumentsContract.buildDocumentUriUsingTree(
                    startUrl,
                    documentId);
            BufferedInputStream bis = new BufferedInputStream(contentResolver.openInputStream(uri));
            bis.skip(offset);
            return bis;
        }
        return null;
    }

    private void closeQuietly(Cursor cursor) {
        cursor.close();
    }
}
