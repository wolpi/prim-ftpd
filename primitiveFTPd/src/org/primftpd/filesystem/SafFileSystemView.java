package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.DocumentsContract;
import androidx.documentfile.provider.DocumentFile;
import android.widget.Toast;

import org.primftpd.services.PftpdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class SafFileSystemView<T extends SafFile<X>, X> {

    protected final static String ROOT_PATH = "/";

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final Context context;
    protected final Uri startUrl;
    protected final ContentResolver contentResolver;
    protected final PftpdService pftpdService;
    protected final int timeResolution;

    public SafFileSystemView(Context context, Uri startUrl, ContentResolver contentResolver, PftpdService pftpdService) {
        this.context = context;
        this.startUrl = startUrl;
        this.contentResolver = contentResolver;
        this.pftpdService = pftpdService;
        this.timeResolution = StorageManagerUtil.getFilesystemTimeResolutionForTreeUri(startUrl);
    }

    protected abstract T createFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            String absPath,
            PftpdService pftpdService);
    protected abstract T createFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            String name,
            String absPath,
            PftpdService pftpdService);

    protected abstract String absolute(String file);

    public long getCorrectedTime(long time) {
        return (time / timeResolution) * timeResolution;
    }

    public T getFile(String file) {
        logger.trace("getFile({}), startUrl: {}", file, startUrl);

        String abs = absolute(file);
        logger.trace("  getFile(abs: {})", abs);

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
                                    Uri docUri = DocumentsContract.buildDocumentUriUsingTree(startUrl, docId);
                                    Uri parentUri = DocumentsContract.buildDocumentUriUsingTree(startUrl, parentId);
                                    logger.trace("    calling createFile() for doc: {}, docId: {}, docUri: {}, parentId: {}, parentUri: {}", new Object[]{currentPart, docId, docUri, parentId, parentUri});
                                    DocumentFile parentDocFile = DocumentFile.fromTreeUri(context, parentUri);
                                    DocumentFile docFile = DocumentFile.fromTreeUri(context, docUri);
                                    String absPath = Utils.toPath(parts);
                                    return createFile(contentResolver, parentDocFile, docFile, absPath, pftpdService);
                                } else {
                                    parentId = docId;
                                    break;
                                }
                            }
                        }
                        if (childCursor.isAfterLast()) {
                            // not found
                            if (i == parts.size() - 1) {
                                // probably upload -> create object just with name
                                Uri parentUri = DocumentsContract.buildDocumentUriUsingTree(startUrl, parentId);
                                logger.trace("    calling createFile() for doc: {}, parentId: {}, parentUri: {}", new Object[]{currentPart, parentId, parentUri});
                                DocumentFile parentDocFile = DocumentFile.fromTreeUri(context, parentUri);
                                String absPath = Utils.toPath(parts);
                                return createFile(contentResolver, parentDocFile, currentPart, absPath, pftpdService);
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
            DocumentFile rootDocFile = DocumentFile.fromTreeUri(context, startUrl);
            logger.trace("    calling createFile() for root doc: {}", startUrl);
            return createFile(contentResolver, rootDocFile, rootDocFile, ROOT_PATH, pftpdService);
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

    private void closeQuietly(Cursor cursor) {
        if (cursor != null) {
            cursor.close();
        }
    }
}
