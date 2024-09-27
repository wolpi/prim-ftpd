package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

import org.primftpd.services.PftpdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class RoSafFileSystemView<T extends RoSafFile<X>, X> {

    protected final static String ROOT_PATH = "/";

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final Uri startUrl;
    protected final ContentResolver contentResolver;
    protected final PftpdService pftpdService;
    protected final int timeResolution;

    public RoSafFileSystemView(Uri startUrl, ContentResolver contentResolver, PftpdService pftpdService) {
        this.startUrl = startUrl;
        this.contentResolver = contentResolver;
        this.pftpdService = pftpdService;
        this.timeResolution = StorageManagerUtil.getFilesystemTimeResolutionForTreeUri(startUrl);
    }

    protected abstract String absolute(String file);

    protected abstract T createFile(
            ContentResolver contentResolver,
            Uri startUrl,
            String absPath,
            PftpdService pftpdService);
    protected abstract T createFile(
            ContentResolver contentResolver,
            Uri startUrl,
            String docId,
            String absPath,
            PftpdService pftpdService);
    protected abstract T createFileNonExistent(
            ContentResolver contentResolver,
            Uri startUrl,
            String name,
            String absPath,
            PftpdService pftpdService);

    public int getTimeResolution() {
        return timeResolution;
    }

    public T getFile(String file) {
        logger.trace("getFile({}), startUrl: {}", file, startUrl);

        String abs = absolute(file);
        logger.trace("  getFile(abs: {})", abs);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && !ROOT_PATH.equals(abs)) {
            String parentId = DocumentsContract.getTreeDocumentId(startUrl);

            List<String> parts = Utils.normalizePath(abs);
            logger.trace("    getFile(normalized: {})", parts);

            for (int i=0; i<parts.size(); i++) {
                String currentPart = parts.get(i);

                logger.trace("    building children uri for parent: {}", parentId);
                Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                        startUrl,
                        parentId);
                // Do not use selection and selectionArgs for ContentResolver.query(), Android doesn't care, it will return all the files whatever you do.
                // See: https://stackoverflow.com/a/61214849
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
                                logger.trace("    calling createFile() for doc: {}, parent: {}", docName, parentId);
                                return createFile(contentResolver, startUrl, docId, Utils.toPath(parts), pftpdService);
                            } else {
                                parentId = docId;
                                break;
                            }
                        }
                    }
                    if (childCursor.isAfterLast()) {
                        // not found
                        if (i == parts.size() - 1) {
                            // TODO we are read only -> there is no upload anyway
                            // probably upload -> create object just with name
                            logger.trace("    calling createFile() for not found doc: {}", currentPart);
                            return createFileNonExistent(contentResolver, startUrl, currentPart, Utils.toPath(parts), pftpdService);
                        } else {
                            // invalid path
                            String absPath = Utils.toPath(parts.subList(0, i+1));
                            logger.error("path does not exist: {}", absPath);
                            // fall through to returning the root document
                            break;
                        }
                    }
                } finally {
                    closeQuietly(childCursor);
                }
            }
        }
        logger.trace("    calling createFile() for root doc: {}", startUrl);
        return createFile(contentResolver, startUrl, ROOT_PATH, pftpdService);
    }

    private void closeQuietly(Cursor cursor) {
        if (cursor != null) {
            cursor.close();
        }
    }
}
