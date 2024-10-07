package org.primftpd.filesystem;

import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

import org.primftpd.events.ClientActionEvent;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class RoSafFile<TMina, TFileSystemView extends RoSafFileSystemView> extends AbstractFile<TFileSystemView> {

    private String documentId;
    private boolean isDirectory;
    private boolean exists;
    private boolean readable;
    private long lastModified;
    private long size;
    private boolean writable;
    private boolean deletable;

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
            TFileSystemView fileSystemView,
            String absPath) {
        // this c-tor is to be used for start directory
        super(
                fileSystemView,
                absPath,
                null);
        logger.trace("  c-tor 1");

        try {
            Cursor cursor = getPftpdService().getContext().getContentResolver().query(
                    getStartUrl(),
                    SAF_QUERY_COLUMNS,
                    null,
                    null,
                    null);
            try {
                initByCursor(cursor);
            } catch (Exception e) {
                logger.warn("  exception opening cursor", e);
            } finally {
                closeQuietly(cursor);
            }
        } catch (UnsupportedOperationException e) {
            logger.warn("  UnsupportedOperationException opening cursor, creating fallback object", e);
            // this is probably a directory
            name = SafFileSystemView.ROOT_PATH;
            isDirectory = true;
            exists = true;
            readable = true;
        }
    }

    public RoSafFile(
            TFileSystemView fileSystemView,
            String absPath,
            String docId,
            boolean exists) {
        // this c-tor is to be used for FileSystemView.getFile()
        super(
                fileSystemView,
                absPath,
                null);
        logger.trace("  c-tor 2");

        this.exists = exists;
        if (exists) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Uri uri = DocumentsContract.buildDocumentUriUsingTree(
                        getStartUrl(),
                        docId);

                Cursor cursor = getPftpdService().getContext().getContentResolver().query(
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

    protected RoSafFile(
            TFileSystemView fileSystemView,
            String absPath,
            Cursor cursor) {
        // this c-tor is to be used by listFiles()
        super(
                fileSystemView,
                absPath,
                null);
        logger.trace("  c-tor 3");

        initByCursor(cursor);
    }

    private void initByCursor(Cursor cursor) {
        name = cursor.getString(1);
        documentId = cursor.getString(0);
        logger.trace("    initByCursor, doc id: {}, name: {}", documentId, name);

        isDirectory = DocumentsContract.Document.MIME_TYPE_DIR.equals(cursor.getString(4));
        exists = true;
        readable = true;
        lastModified = getFileSystemView().getCorrectedTime(cursor.getLong(2));
        size = cursor.getLong(3);

        int flags = cursor.getInt(5);
        writable = flagPresent(flags, DocumentsContract.Document.FLAG_SUPPORTS_WRITE);
        deletable = flagPresent(flags, DocumentsContract.Document.FLAG_SUPPORTS_DELETE);
    }

    private boolean flagPresent(int flags, int flag) {
        return ((flags & flag) == flag);
    }

    protected final Uri getStartUrl() {
        return getFileSystemView().getStartUrl();
    }

    protected abstract TMina createFile(String absPath, Cursor cursor);

    @Override
    public ClientActionEvent.Storage getClientActionStorage() {
        return ClientActionEvent.Storage.ROSAF;
    }

    public boolean isDirectory() {
        logger.trace("[{}] isDirectory() -> {}", name, isDirectory);
        return isDirectory;
    }

    public boolean doesExist() {
        logger.trace("[{}] doesExist() -> {}", name, exists);
        return exists;
    }

    public boolean isReadable() {
        logger.trace("[{}] isReadable() -> {}", name, readable);
        return readable;
    }

    public long getLastModified() {
        logger.trace("[{}] getLastModified() -> {}", name, lastModified);
        return lastModified;
    }

    public long getSize() {
        logger.trace("[{}] getSize() -> {}", name, size);
        return size;
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
                parentUri = DocumentsContract.buildDocumentUriUsingTree(getStartUrl(), documentId);
            } else {
                parentUri = getStartUrl();
            }
            logger.trace("mkdir(): parent uri: '{}'", parentUri);
            try {
                Uri newDirUri = DocumentsContract.createDocument(getPftpdService().getContext().getContentResolver(),
                    parentUri, DocumentsContract.Document.MIME_TYPE_DIR, name);
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
//            Uri docUri = DocumentsContract.buildDocumentUriUsingTree(getStartUrl(), documentId);
//            logger.trace("delete(): docUri: '{}'", docUri);
//            try {
//                return DocumentsContract.deleteDocument(getPftpdService().getContext().getContentResolver(), docUri);
//            } catch (FileNotFoundException e) {
//                logger.error("could not delete " + name, e);
//            }
//        }
        return false;
    }

    public boolean move(AbstractFile<TFileSystemView> destination) {
        logger.trace("[{}] move({})", name, destination.getAbsolutePath());
        // TODO writing with SAF cursor/uri api
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            Uri docUri = DocumentsContract.buildDocumentUriUsingTree(getStartUrl(), documentId);
//            logger.trace("move(): docUri: '{}'", docUri);
//            try {
//                Uri newNameUri = DocumentsContract.renameDocument(getPftpdService().getContext().getContentResolver(), docUri, destination.getName());
//                return newNameUri != null;
//            } catch (FileNotFoundException e) {
//                logger.error("could not rename " + name, e);
//            }
//        }
        return false;
    }

    public List<TMina> listFiles() {
        logger.trace("[{}] listFiles()", name);
        postClientAction(ClientActionEvent.ClientAction.LIST_DIR);

        List<TMina> result = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Uri startUrl = getStartUrl();
            String parentId;
            if (documentId != null) {
                parentId = documentId;
            } else {
                parentId = DocumentsContract.getTreeDocumentId(startUrl);
                logger.trace("  got parentId: {}, for startURL: {}", parentId, startUrl);
            }

            logger.trace("  building children uri for doc: {}, parent: {}", documentId, parentId);
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    startUrl,
                    parentId);
            Cursor childCursor = getPftpdService().getContext().getContentResolver().query(
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
                    result.add(createFile(absPath, childCursor));
                }
            } finally {
                closeQuietly(childCursor);
            }
        }
        // log result
        for (TMina obj : result) {
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
                    getStartUrl(),
                    documentId);
            return new TracingBufferedOutputStream(getPftpdService().getContext().getContentResolver().openOutputStream(uri), logger);
        }
        // TODO no null, throw IOException
        return null;
    }

    public InputStream createInputStream(long offset) throws IOException {
        logger.trace("[{}] createInputStream(offset: {})", name, offset);
        postClientAction(ClientActionEvent.ClientAction.DOWNLOAD);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Uri uri = DocumentsContract.buildDocumentUriUsingTree(
                    getStartUrl(),
                    documentId);
            BufferedInputStream bis = new BufferedInputStream(getPftpdService().getContext().getContentResolver().openInputStream(uri));
            bis.skip(offset);
            return bis;
        }
        // TODO no null, throw IOException
        return null;
    }

    private void closeQuietly(Cursor cursor) {
        if (cursor != null) {
            cursor.close();
        }
    }
}
