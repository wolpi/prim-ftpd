package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import org.primftpd.events.ClientActionEvent;
import org.primftpd.services.PftpdService;

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

public abstract class SafFile<T> extends AbstractFile {

    private final static Set<String> KNOWN_BAD_CHARS;
    static {
        Set<String> tmp = new HashSet<>();
        tmp.add("[");
        tmp.add("]");
        tmp.add("*");
        tmp.add("?");
        tmp.add("\\");
        KNOWN_BAD_CHARS = Collections.unmodifiableSet(tmp);
    }

    private final ContentResolver contentResolver;

    private DocumentFile documentFile;
    private final DocumentFile parentDocumentFile;
    protected final SafFileSystemView fileSystemView;

    private boolean writable;

    public SafFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            String absPath,
            PftpdService pftpdService,
            SafFileSystemView fileSystemView) {
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
        this.fileSystemView = fileSystemView;

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
            PftpdService pftpdService,
            SafFileSystemView fileSystemView) {
        // this c-tor is to be used to upload new files, create directories or renaming
        super(absPath, name, 0, 0, false, false, false, pftpdService);
        String parentName = parentDocumentFile.getName();
        logger.trace("new SafFile() with name '{}', parent '{}' and absPath '{}'",
                new Object[]{name, parentName, absPath});
        this.contentResolver = contentResolver;
        this.name = name;
        this.writable = true;

        this.parentDocumentFile = parentDocumentFile;
        this.fileSystemView = fileSystemView;
    }

    protected abstract T createFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            String absPath,
            PftpdService pftpdService,
            SafFileSystemView fileSystemView);

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
                Uri docUri = documentFile.getUri();
                Path filePath = Paths.get(StorageManagerUtil.getFullDocIdPathFromTreeUri(docUri, pftpdService.getContext()));
                int timeResolution = fileSystemView.getTimeResolution();
                long convertedTime = (time / timeResolution) * timeResolution;
                Files.getFileAttributeView(filePath, BasicFileAttributeView.class).setTimes(FileTime.fromMillis(convertedTime), null, null);
            } catch (Exception e) {
                String baseMsg = "could not set last modified time";
                logger.error(baseMsg, e);
                String clientActionMsg = baseMsg + ", error: " + e.getClass().getName();
                postClientActionError(clientActionMsg);
                String toastMsg = baseMsg + ", file: " + name + ", error: " + e.getClass().getName();
                try {
                    Toast.makeText(pftpdService.getContext(), toastMsg, Toast.LENGTH_SHORT).show();
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
            result.add(createFile(contentResolver, documentFile, child, absPath, pftpdService, fileSystemView));
        }
        logger.trace("  [{}] listFiles(): num children: {}", name, Integer.valueOf(result.size()));
        return result;
    }

    public OutputStream createOutputStream(long offset) throws IOException {
        logger.trace("[{}] createOutputStream(offset: {})", name, offset);
        postClientAction(ClientActionEvent.ClientAction.UPLOAD);

        // validate file name for known bad characters
        for (String badChar : KNOWN_BAD_CHARS) {
            if (name.contains(badChar)) {
                String msg = "filename contains known bad char: '" +  badChar + "', will not create file";
                logger.warn(msg);
                throw new IOException(msg);
            }
        }

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

    // This method is the equivalent of java.io.File.createNewFile(), it creates the file and updates the cached properties of it.
    // This method is required by SSHFS, because it calls STAT and later FSTAT on created new files,
    // STAT requires a created new file, FSTAT requires updated properties.
    // This method is not required by normal clients who simply open, write and close the file.
    boolean createNewFile() throws IOException {
        logger.trace("[{}] createNewFile()", name);
        try {
            documentFile = parentDocumentFile.createFile(null, name);
        } catch (Exception e) {
            throw new IOException("Failed to create file", e);
        }

        if (documentFile != null) {
            lastModified = documentFile.lastModified();
            size = 0;
            readable = documentFile.canRead();
            exists = true;
            writable = documentFile.canWrite();
            return true;
        }

        return false;
    }
}
