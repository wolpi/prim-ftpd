package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class RoSafFileSystemView<T extends RoSafFile<X>, X> {

    protected final static String ROOT_PATH = "/";

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final Uri startUrl;
    protected final ContentResolver contentResolver;

    public RoSafFileSystemView(Uri startUrl, ContentResolver contentResolver) {
        this.startUrl = startUrl;
        this.contentResolver = contentResolver;
    }

    protected abstract String absolute(String file);

    protected abstract T createFile(ContentResolver contentResolver, Uri startUrl, String absPath);
    protected abstract T createFile(ContentResolver contentResolver, Uri startUrl, String docId, String absPath);
    protected abstract T createFileNonExistant(ContentResolver contentResolver, Uri startUrl, String name, String absPath);

    public T getFile(String file) {
        logger.trace("getFile({})", file);

        file = absolute(file);

        logger.trace("    getFile(abs: {})", file);
        if (ROOT_PATH.equals(file)) {
            return createFile(contentResolver, startUrl, ROOT_PATH);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String parentId = DocumentsContract.getTreeDocumentId(startUrl);

            List<String> parts = Utils.normalizePath(file);
            logger.trace("    getFile(normalized: {})", parts);

            for (int i=0; i<parts.size(); i++) {
                String currentPart = parts.get(i);

                Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                        startUrl,
                        parentId);
                Cursor childCursor = contentResolver.query(
                        childrenUri,
                        new String[] {
                                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                                DocumentsContract.Document.COLUMN_DISPLAY_NAME
                        },
                        null,
                        null,
                        null);
                try {
                    while (childCursor.moveToNext()) {
                        String docId = childCursor.getString(0);
                        String docName = childCursor.getString(1);
                        if (currentPart.equals(docName)) {
                            if (i == parts.size() - 1) {
                                return createFile(contentResolver, startUrl, docId, Utils.toPath(parts));
                            } else {
                                parentId = docId;
                                break;
                            }
                        }
                    }
                    // not found -> probably upload -> create object just with name
                    return createFileNonExistant(contentResolver, startUrl, currentPart, Utils.toPath(parts));
                } finally {
                    closeQuietly(childCursor);
                }
            }
        }
        return createFile(contentResolver, startUrl, ROOT_PATH);
    }

    private void closeQuietly(Cursor cursor) {
        cursor.close();
    }
}
