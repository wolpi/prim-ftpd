package org.primftpd.filesystem;

import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

import org.primftpd.services.PftpdService;

import java.util.List;

public abstract class RoSafFileSystemView<TFile extends RoSafFile<TMina, ? extends RoSafFileSystemView>, TMina> extends AbstractFileSystemView {

    protected final static String ROOT_PATH = "/";

    protected final Uri startUrl;

    protected final int timeResolution;

    public RoSafFileSystemView(PftpdService pftpdService, Uri startUrl) {
        super(pftpdService);
        this.startUrl = startUrl;

        this.timeResolution = StorageManagerUtil.getFilesystemTimeResolutionForTreeUri(startUrl);
    }

    protected abstract String absolute(String file);

    protected abstract TFile createFile(
            String absPath);
    protected abstract TFile createFile(
            String absPath,
            String docId,
            boolean exists);

    public final Uri getStartUrl() {
        return startUrl;
    }

    public long getCorrectedTime(long time) {
        return (time / timeResolution) * timeResolution;
    }

    public TFile getFile(String file) {
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
                Cursor childCursor = pftpdService.getContext().getContentResolver().query(
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
                                return createFile(Utils.toPath(parts), docId, true);
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
                            return createFile(Utils.toPath(parts), currentPart, false);
                        } else {
                            // invalid path
                            String absPath = Utils.toPath(parts.subList(0, i+1));
                            logger.error("path does not exist: {}", absPath);
                            // fall through to returning the root document
                            // TODO follow SAF implementation
                            break;
                        }
                    }
                } finally {
                    closeQuietly(childCursor);
                }
            }
        }
        logger.trace("    calling createFile() for root doc: {}", startUrl);
        return createFile(ROOT_PATH);
    }

    private void closeQuietly(Cursor cursor) {
        if (cursor != null) {
            cursor.close();
        }
    }
}
