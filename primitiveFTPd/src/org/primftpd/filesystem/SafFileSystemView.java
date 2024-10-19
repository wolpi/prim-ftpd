package org.primftpd.filesystem;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.widget.Toast;

import org.primftpd.services.PftpdService;

import java.util.Collections;
import java.util.List;

import androidx.documentfile.provider.DocumentFile;

public abstract class SafFileSystemView<TFile extends SafFile<TMina, ? extends SafFileSystemView>, TMina> extends AbstractFileSystemView {

    protected final static String ROOT_PATH = "/";

    protected final Uri startUrl;

    protected final int timeResolution;

    public SafFileSystemView(PftpdService pftpdService, Uri startUrl) {
        super(pftpdService);
        this.startUrl = startUrl;

        this.timeResolution = StorageManagerUtil.getFilesystemTimeResolutionForTreeUri(startUrl);
    }

    public final Uri getStartUrl() {
        return startUrl;
    }

    protected abstract TFile createFile(
            String absPath,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile);
    protected abstract TFile createFile(
            String absPath,
            DocumentFile parentDocumentFile,
            List<String> parentNonexistentDirs,
            String name);

    protected abstract String absolute(String file);

    public long getCorrectedTime(long time) {
        return (time / timeResolution) * timeResolution;
    }

    public TFile getFile(String file) {
        logger.trace("getFile({}), startUrl: {}", file, startUrl);

        String abs = absolute(file);
        logger.trace("  getFile(abs: {})", abs);

        Context context = pftpdService.getContext();
        try {
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
                    // Never ever use DocumentFile.findFile(), it uses DocumentFile.listFile(), that queries the whole directory for IDs,
                    // then queries FOR EACH FILE AGAIN for the name, then findFile() selects from this name list. Unbelievable.
                    // See: https://www.reddit.com/r/androiddev/comments/bbejc4/caveats_with_documentfile/
                    // Do not use selection and selectionArgs for ContentResolver.query(), Android doesn't care, it will return all the files whatever you do.
                    // See: https://stackoverflow.com/a/61214849
                    Cursor childCursor = context.getContentResolver().query(
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
                                    Uri docUri = DocumentsContract.buildDocumentUriUsingTree(startUrl, docId);
                                    Uri parentUri = DocumentsContract.buildDocumentUriUsingTree(startUrl, parentId);
                                    logger.trace("    calling createFile() for doc: {}, docId: {}, docUri: {}, parentId: {}, parentUri: {}",
                                        new Object[]{currentPart, docId, docUri, parentId, parentUri});
                                    String absPath = Utils.toPath(parts);
                                    DocumentFile parentDocFile = DocumentFile.fromTreeUri(context, parentUri);
                                    DocumentFile docFile = DocumentFile.fromTreeUri(context, docUri);
                                    return createFile(absPath, parentDocFile, docFile);
                                } else {
                                    parentId = docId;
                                    break;
                                }
                            }
                        }
                        if (childCursor.isAfterLast()) {
                            // not found, probably upload or dir creation -> create object just with name
                            String absPath = Utils.toPath(parts);
                            Uri parentUri = DocumentsContract.buildDocumentUriUsingTree(startUrl, parentId);
                            DocumentFile parentDocFile = DocumentFile.fromTreeUri(context, parentUri);
                            if (i == parts.size() - 1) {
                                // file upload or dir creation in an existing dir
                                logger.trace("    calling createFile() for doc: {}, parentId: {}, parentUri: {}", new Object[]{currentPart, parentId, parentUri});
                                return createFile(absPath, parentDocFile, Collections.emptyList(), currentPart);
                            } else {
                                // file upload or dir creation in an nonexistent dir, probably called without first calling mkdirs
                                List<String> parentNonexistentDirs = parts.subList(i, parts.size() - 1);
                                String name = parts.get(parts.size() - 1);
                                logger.trace("    calling createFile() for doc: {}, parentId: {}, parentUri: {}, parentNonexistentDirs: {}",
                                    new Object[]{name, parentId, parentUri, Utils.toPath(parentNonexistentDirs)});
                                return createFile(absPath, parentDocFile, parentNonexistentDirs, name);
                            }
                        }
                    } finally {
                        closeQuietly(childCursor);
                    }
                }
            }
            DocumentFile rootDocFile = DocumentFile.fromTreeUri(context, startUrl);
            logger.trace("    calling createFile() for root doc: {}", startUrl);
            return createFile(ROOT_PATH, rootDocFile, rootDocFile);
        } catch (Exception e) {
            final String msg = "[(s)ftpd] Error getting data from SAF: " + e.getMessage();
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

    private void closeQuietly(Cursor cursor) {
        if (cursor != null) {
            cursor.close();
        }
    }
}
