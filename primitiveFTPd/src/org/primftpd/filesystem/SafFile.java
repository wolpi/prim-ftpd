package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

import androidx.documentfile.provider.DocumentFile;

import org.primftpd.events.ClientActionEvent;
import org.primftpd.services.PftpdService;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class SafFile<T> extends AbstractFile {

    private final ContentResolver contentResolver;

    private DocumentFile documentFile;
    private DocumentFile parentDocumentFile;

    private boolean writable;

    public SafFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            String absPath,
            PftpdService pftpdService) {
        // this c-tor is to be used to access existing files
        super(
                absPath,
                null,
                documentFile.lastModified(),
                documentFile.length(),
                documentFile.canRead(),
                documentFile.exists(),
                documentFile.isDirectory(),
                pftpdService);
        String parentName = parentDocumentFile.getName();
        logger.trace("new SafFile() with documentFile, parent '{}' and absPath '{}'", parentName, absPath);
        this.contentResolver = contentResolver;

        this.parentDocumentFile = parentDocumentFile;
        this.documentFile = documentFile;

        name = documentFile.getName();
        if (name == null && SafFileSystemView.ROOT_PATH.equals(absPath)) {
            name = SafFileSystemView.ROOT_PATH;
        }
        writable = documentFile.canWrite();
    }

    public SafFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            String name,
            String absPath,
            PftpdService pftpdService) {
        // this c-tor is to be used to upload new files, create directories or renaming
        super(absPath, name, 0, 0, false, false, false, pftpdService);
        String parentName = parentDocumentFile.getName();
        logger.trace("new SafFile() with name '{}', parent '{}' and absPath '{}'",
                new Object[]{name, parentName, absPath});
        this.contentResolver = contentResolver;
        this.name = name;
        this.writable = true;

        this.parentDocumentFile = parentDocumentFile;
    }

    protected abstract T createFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            String absPath,
            PftpdService pftpdService);

    @Override
    public ClientActionEvent.Storage getClientActionStorage() {
        return ClientActionEvent.Storage.SAF;
    }

    public boolean isFile() {
        boolean result = !isDirectory;
        logger.trace("[{}] isFile() -> {}", name, result);
        return result;
    }

    public boolean isWritable() {
        logger.trace("[{}] isWritable() -> {}", name, writable);
        return writable;
    }

    public boolean isRemovable() {
        boolean result = writable;
        logger.trace("[{}] isRemovable() -> {}", name, result);
        return result;
    }

    public boolean setLastModified(long time) {
        logger.trace("[{}] setLastModified({})", name, time);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                ContentValues updateValues = new ContentValues();
                //time = Utils.sshTimeToFileTime(time);
                updateValues.put(DocumentsContract.Document.COLUMN_LAST_MODIFIED, time);
                Uri docUri = documentFile.getUri();
                int updated = contentResolver.update(docUri, updateValues, null, null);
                return updated == 1;
            } catch (Exception e) {
                logger.error("could not set last modified time", e);
            }
        }
        return false;
    }

    public boolean mkdir() {
        logger.trace("[{}] mkdir()", name);
        postClientAction(ClientActionEvent.ClientAction.CREATE_DIR);
        return parentDocumentFile.createDirectory(name) != null;
    }

    public boolean delete() {
        logger.trace("[{}] delete()", name);
        if (writable && documentFile != null) {
            postClientAction(ClientActionEvent.ClientAction.DELETE);
            return documentFile.delete();
        }
        return false;
    }

    public boolean move(SafFile<T> destination) {
        logger.trace("[{}] move({})", name, destination.getAbsolutePath());
        if (writable && documentFile != null) {
            postClientAction(ClientActionEvent.ClientAction.RENAME);
            return documentFile.renameTo(destination.getName());
        }
        return false;
    }

    public List<T> listFiles() {
        logger.trace("[{}] listFiles()", name);
        postClientAction(ClientActionEvent.ClientAction.LIST_DIR);

        DocumentFile[] children = documentFile.listFiles();
        List<T> result = new ArrayList<>(children.length);
        for (DocumentFile child : children) {
            String absPath = this.absPath.endsWith("/")
                    ? this.absPath + child.getName()
                    : this.absPath + "/" + child.getName();
            result.add(createFile(contentResolver, documentFile, child, absPath, pftpdService));
        }
        logger.trace("  [{}] listFiles(): num children: {}", name, Integer.valueOf(result.size()));
        return result;
    }

    public OutputStream createOutputStream(long offset) throws IOException {
        logger.trace("[{}] createOutputStream(offset: {})", name, offset);
        postClientAction(ClientActionEvent.ClientAction.UPLOAD);

        Uri uri;
        if (documentFile != null) {
            // existing files
            uri = documentFile.getUri();
        } else {
            // new files
            DocumentFile docFile = parentDocumentFile.createFile(null, name);
            uri = docFile.getUri();
        }

        logger.trace("   createOutputStream() uri: {}", uri);
        return new TracingBufferedOutputStream(contentResolver.openOutputStream(uri), logger);
    }

    public InputStream createInputStream(long offset) throws IOException {
        logger.trace("[{}] createInputStream(offset: {})", name, offset);
        postClientAction(ClientActionEvent.ClientAction.DOWNLOAD);

        if (documentFile != null) {
            BufferedInputStream bis = new BufferedInputStream(contentResolver.openInputStream(documentFile.getUri()));
            bis.skip(offset);
            return bis;
        }

        return null;
    }
}
