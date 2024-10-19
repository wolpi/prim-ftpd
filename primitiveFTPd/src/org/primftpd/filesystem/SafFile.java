package org.primftpd.filesystem;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.widget.Toast;

import org.primftpd.events.ClientActionEvent;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.documentfile.provider.DocumentFile;

public abstract class SafFile<TMina, TFileSystemView extends SafFileSystemView> extends AbstractFile<TFileSystemView> {

    private final static Set<String> KNOWN_BAD_CHARS;
    static {
        Set<String> tmp = new HashSet<>();
        tmp.add("*");
        tmp.add("?");
        tmp.add("\\");
        KNOWN_BAD_CHARS = Collections.unmodifiableSet(tmp);
    }

    private DocumentFile parentDocumentFile;
    private List<String> parentNonexistentDirs;
    private DocumentFile documentFile;

    public SafFile(
            TFileSystemView fileSystemView,
            String absPath,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile) {
        // this c-tor is to be used to access existing files
        super(
                fileSystemView,
                absPath,
                null);
        String parentName = parentDocumentFile.getName();
        logger.trace("new SafFile() with documentFile, parent '{}' and absPath '{}'", parentName, absPath);

        name = documentFile.getName();
        if (name == null && SafFileSystemView.ROOT_PATH.equals(absPath)) {
            name = SafFileSystemView.ROOT_PATH;
        }

        this.parentDocumentFile = parentDocumentFile;
        this.parentNonexistentDirs = Collections.emptyList();
        this.documentFile = documentFile;
    }

    public SafFile(
            TFileSystemView fileSystemView,
            String absPath,
            DocumentFile parentDocumentFile,
            List<String> parentNonexistentDirs,
            String name) {
        // this c-tor is to be used to upload new files, create directories or renaming
        super(
                fileSystemView,
                absPath,
                name);
        String parentName = parentDocumentFile.getName();
        logger.trace("new SafFile() with name '{}', parent '{}', dirs '{}' and absPath '{}'",
                new Object[]{name, parentName, Utils.toPath(parentNonexistentDirs), absPath});

        this.parentDocumentFile = parentDocumentFile;
        this.parentNonexistentDirs = parentNonexistentDirs;
    }

    protected final Uri getStartUrl() {
        return getFileSystemView().getStartUrl();
    }

    private boolean mkParentNonexistentDirs() {
        if (0 < parentNonexistentDirs.size()) {
            DocumentFile parentDoc = parentDocumentFile;
            for (int i=0; i<parentNonexistentDirs.size(); i++) {
                String currentDir = parentNonexistentDirs.get(i);
                logger.trace("[{}] creating parent folder, parent of parent: '{}', parent: '{}'", new Object[]{name, parentDoc.getName(), currentDir});
                DocumentFile currentDoc = parentDoc.createDirectory(currentDir);
                if (currentDoc == null) {
                    return false;
                }
                parentDoc = currentDoc;
            }
            parentDocumentFile = parentDoc;
            parentNonexistentDirs = Collections.emptyList();
        }
        return true;
    }

    protected abstract TMina createFile(
            String absPath,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile);

    @Override
    public ClientActionEvent.Storage getClientActionStorage() {
        return ClientActionEvent.Storage.SAF;
    }

    public boolean isDirectory() {
        boolean result = documentFile != null && documentFile.isDirectory();
        logger.trace("[{}] isDirectory() -> {}", name, result);
        return result;
    }

    public boolean doesExist() {
        boolean result = documentFile != null && documentFile.exists();
        logger.trace("[{}] doesExist() -> {}", name, result);
        return result;
    }

    public boolean isReadable() {
        boolean result = documentFile != null && documentFile.canRead();
        logger.trace("[{}] isReadable() -> {}", name, result);
        return result;
    }

    public long getLastModified() {
        long result = documentFile != null ? getFileSystemView().getCorrectedTime(documentFile.lastModified()) : 0;
        logger.trace("[{}] getLastModified() -> {}", name, result);
        return result;
    }

    public long getSize() {
        long result = documentFile != null ? documentFile.length() : 0;
        logger.trace("[{}] getSize() -> {}", name, result);
        return result;
    }

    public boolean isFile() {
        boolean result = documentFile != null && documentFile.isFile();
        logger.trace("[{}] isFile() -> {}", name, result);
        return result;
    }

    public boolean isWritable() {
        boolean result = documentFile == null || documentFile.canWrite();
        logger.trace("[{}] isWritable() -> {}", name, result);
        return result;
    }

    public boolean isRemovable() {
        boolean result = documentFile == null || documentFile.canWrite();
        logger.trace("[{}] isRemovable() -> {}", name, result);
        return result;
    }

    public boolean setLastModified(long time) {
        logger.trace("[{}] setLastModified({})", name, time);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                Uri docUri = documentFile.getUri();
                Path filePath = Paths.get(StorageManagerUtil.getFullDocIdPathFromTreeUri(docUri, getPftpdService().getContext()));
                long correctedTime = getFileSystemView().getCorrectedTime(time);
                Files.getFileAttributeView(filePath, BasicFileAttributeView.class).setTimes(FileTime.fromMillis(correctedTime), null, null);
                return true;
            } catch (Exception e) {
                String baseMsg = "could not set last modified time";
                logger.error(baseMsg, e);
                String clientActionMsg = baseMsg + ", error: " + e.getClass().getName();
                postClientActionError(clientActionMsg);
                String toastMsg = baseMsg + ", file: " + name + ", error: " + e.getClass().getName();
                try {
                    Toast.makeText(getPftpdService().getContext(), toastMsg, Toast.LENGTH_SHORT).show();
                } catch (Exception e2) {
                    logger.error("cannot show toast: {}: {}", e2.getClass(), e2.getMessage());
                }
            }
        }
        return false;
    }

    public boolean mkdir() {
        logger.trace("[{}] mkdir()", name);
        postClientAction(ClientActionEvent.ClientAction.CREATE_DIR);
        return mkParentNonexistentDirs() && (documentFile = parentDocumentFile.createDirectory(name)) != null;
    }

    public boolean delete() {
        logger.trace("[{}] delete()", name);
        if (isWritable() && documentFile != null) {
            postClientAction(ClientActionEvent.ClientAction.DELETE);
            boolean success = documentFile.delete();
            if (success) {
                documentFile = null;
            }
            return success;
        }
        return false;
    }

    public boolean move(AbstractFile<TFileSystemView> destination) {
        logger.trace("[{}] move({})", name, destination.getAbsolutePath());
        // check if file is renamed in same dir as move to other dir is not supported by documentFile
        boolean isRename = Utils.parent(this.absPath).equals(Utils.parent(destination.getAbsolutePath()));
        if (isWritable() && documentFile != null && isRename) {
            postClientAction(ClientActionEvent.ClientAction.RENAME);
            return documentFile.renameTo(destination.getName());
        }
        return false;
    }

    public List<TMina> listFiles() {
        logger.trace("[{}] listFiles()", name);
        postClientAction(ClientActionEvent.ClientAction.LIST_DIR);

        // listFiles() is very slow, use URI-based-SAF-API as in RoSAF instead
//        DocumentFile[] children = documentFile.listFiles();
//        List<TMina> result = new ArrayList<>(children.length);
//        for (DocumentFile child : children) {
//            String absPath = this.absPath.endsWith("/")
//                    ? this.absPath + child.getName()
//                    : this.absPath + "/" + child.getName();
//            result.add(createFile(absPath, documentFile, child));
//        }

        List<TMina> result = new ArrayList<>();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Uri startUrl = getStartUrl();
            Context context = getPftpdService().getContext();

            Cursor childCursor = null;
            try {
                String documentId = DocumentsContract.getDocumentId(documentFile.getUri());
                Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                        startUrl,
                        documentId);
                childCursor = context.getContentResolver().query(
                        childrenUri,
                        RoSafFile.SAF_QUERY_COLUMNS,
                        null,
                        null,
                        null);
                while (childCursor.moveToNext()) {
                    String absPath = this.absPath.endsWith("/")
                            ? this.absPath + childCursor.getString(RoSafFile.CURSOR_INDEX_NAME)
                            : this.absPath + "/" + childCursor.getString(RoSafFile.CURSOR_INDEX_NAME);
                    String childId = childCursor.getString(RoSafFile.CURSOR_INDEX_ID);
                    Uri childUri = DocumentsContract.buildDocumentUriUsingTree(startUrl, childId);
                    DocumentFile childDocFile = DocumentFile.fromTreeUri(context, childUri);
                    result.add(createFile(absPath, documentFile, childDocFile));
                }
            } catch (Exception e) {
                logger.error("", e);
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            } finally {
                closeQuietly(childCursor);
            }
        }

        logger.trace("  [{}] listFiles(): num children: {}", name, result.size());
        return result;
    }

    private void closeQuietly(Cursor cursor) {
        if (cursor != null) {
            cursor.close();
        }
    }

    public OutputStream createOutputStream(long offset) throws IOException {
        logger.trace("[{}] createOutputStream(offset: {})", name, offset);
        postClientAction(ClientActionEvent.ClientAction.UPLOAD);

        if (offset != 0) {
            throw new UnsupportedOperationException("Only offset=0 is supported");
        }

        // validate file name for known bad characters
        for (String badChar : KNOWN_BAD_CHARS) {
            if (name.contains(badChar)) {
                String msg = "filename contains known bad char: '" +  badChar + "', will not create file";
                logger.warn(msg);
                throw new IOException(msg);
            }
        }

        if (documentFile == null) {
            // may be necessary to create dirs
            // some clients do not issue mkdir commands like filezilla
            if (!mkParentNonexistentDirs()) {
                throw new IOException(String.format("Failed to create parent folder(s) '%s'", absPath));
            }
            if (!createNewFile()) {
                throw new IOException(String.format("Failed to create file '%s'", absPath));
            }
        }
        Uri uri = documentFile.getUri();
        logger.trace("   createOutputStream() uri: {}", uri);
        return new TracingBufferedOutputStream(getPftpdService().getContext().getContentResolver().openOutputStream(uri), logger);
    }

    public InputStream createInputStream(long offset) throws IOException {
        logger.trace("[{}] createInputStream(offset: {})", name, offset);
        postClientAction(ClientActionEvent.ClientAction.DOWNLOAD);

        if (documentFile == null) {
            throw new IOException(String.format("File '%s' doesn't exist", absPath));
        }
        BufferedInputStream bis = new BufferedInputStream(getPftpdService().getContext().getContentResolver().openInputStream(documentFile.getUri()));
        bis.skip(offset);
        return bis;
    }

    // This method is the equivalent of java.io.File.createNewFile().
    // This method is required by SSHFS, because it calls STAT on created new files.
    // This method is not required by normal clients who simply open, write and close the file.
    boolean createNewFile() {
        logger.trace("[{}] createNewFile()", name);
        return (documentFile = parentDocumentFile.createFile(null, name)) != null;
    }
}
