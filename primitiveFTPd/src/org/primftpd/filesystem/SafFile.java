package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.v4.provider.DocumentFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class SafFile<T> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final ContentResolver contentResolver;

    private DocumentFile documentFile;
    private DocumentFile parentDocumentFile;

    private boolean isDirectory = false;
    private String absPath;

    protected String name;
    private long lastModified;
    private long size;
    private boolean writable;
    private boolean readable;
    private boolean exists;

    public SafFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            String absPath) {
        // this c-tor is to be used to access existing files
        super();
        String parentName = parentDocumentFile.getName();
        logger.trace("new SafFile() with documentFile, parent '{}' and absPath '{}'", parentName, absPath);
        this.contentResolver = contentResolver;
        this.absPath = absPath;

        this.parentDocumentFile = parentDocumentFile;
        this.documentFile = documentFile;

        name = documentFile.getName();
        if (name == null && SafFileSystemView.ROOT_PATH.equals(absPath)) {
            name = SafFileSystemView.ROOT_PATH;
        }
        readable = documentFile.canRead();
        writable = documentFile.canWrite();
        exists = documentFile.exists();
        isDirectory = documentFile.isDirectory();
        lastModified = documentFile.lastModified();
        size = documentFile.length();
    }

    public SafFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            String name,
            String absPath) {
        // this c-tor is to be used to upload new files, create directories or renaming
        super();
        String parentName = parentDocumentFile.getName();
        logger.trace("new SafFile() with name '{}', parent '{}' and absPath '{}'",
                new Object[]{name, parentName, absPath});
        this.contentResolver = contentResolver;
        this.name = name;
        this.writable = true;
        this.absPath = absPath;

        this.parentDocumentFile = parentDocumentFile;
    }

    protected abstract T createFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            String absPath);

    public String getAbsolutePath() {
        logger.trace("[{}] getAbsolutePath() -> '{}'", name, absPath);
        return absPath;
    }

    public String getName() {
        logger.trace("[{}] getName()", name);
        return name;
    }

    public boolean isDirectory() {
        logger.trace("[{}] isDirectory() -> {}", name, isDirectory);
        return isDirectory;
    }

    public boolean isFile() {
        boolean result = !isDirectory;
        logger.trace("[{}] isFile() -> {}", name, result);
        return result;
    }

    public boolean doesExist() {
        logger.trace("[{}] doesExist() -> {}", name, exists);
        return exists;
    }

    public boolean isReadable() {
        logger.trace("[{}] isReadable() -> {}", name, readable);
        return readable;
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

    public long getLastModified() {
        logger.trace("[{}] getLastModified() -> {}", name, lastModified);
        return lastModified;
    }

    public boolean setLastModified(long time) {
        logger.trace("[{}] setLastModified({})", name, time);
        return false;
    }

    public long getSize() {
        logger.trace("[{}] getSize() -> {}", name, size);
        return size;
    }

    public boolean mkdir() {
        logger.trace("[{}] mkdir()", name);
        return parentDocumentFile.createDirectory(name) != null;
    }

    public boolean delete() {
        logger.trace("[{}] delete()", name);
        if (writable && documentFile != null) {
            return documentFile.delete();
        }
        return false;
    }

    public boolean move(SafFile<T> destination) {
        logger.trace("[{}] move({})", name, destination.getAbsolutePath());
        if (writable && documentFile != null) {
            return documentFile.renameTo(destination.getName());
        }
        return false;
    }

    public List<T> listFiles() {
        logger.trace("[{}] listFiles()", name);

        DocumentFile[] children = documentFile.listFiles();
        List<T> result = new ArrayList<>(children.length);
        for (DocumentFile child : children) {
            String absPath = this.absPath.endsWith("/")
                    ? this.absPath + child.getName()
                    : this.absPath + "/" + child.getName();
            result.add(createFile(contentResolver, documentFile, child, absPath));
        }
        logger.trace("  [{}] listFiles(): num children: {}", name, Integer.valueOf(result.size()));
        return result;
    }

    public OutputStream createOutputStream(long offset) throws IOException {
        logger.trace("[{}] createOutputStream(offset: {})", name, offset);

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
        ParcelFileDescriptor pfd = contentResolver.openFileDescriptor(uri, "w");
        return new FileOutputStream(pfd.getFileDescriptor());
    }

    public InputStream createInputStream(long offset) throws IOException {
        logger.trace("[{}] createInputStream(offset: {})", name, offset);

        if (documentFile != null) {
            return contentResolver.openInputStream(documentFile.getUri());
        }

        return null;
    }
}
