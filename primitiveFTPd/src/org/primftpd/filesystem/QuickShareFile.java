package org.primftpd.filesystem;

import org.primftpd.events.ClientActionEvent;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class QuickShareFile<TMina, TFileSystemView extends QuickShareFileSystemView> extends AbstractFile<TFileSystemView> {

    protected final File realFile;

    public QuickShareFile(TFileSystemView fileSystemView) {
        // this c-tor is to be used to access fake directory
        super(
                fileSystemView,
                QuickShareFileSystemView.ROOT_PATH,
                QuickShareFileSystemView.ROOT_PATH);
        this.realFile = null;
    }

    public QuickShareFile(TFileSystemView fileSystemView, File realFile) {
        // this c-tor is to be used to access actual file
        super(
                fileSystemView,
                QuickShareFileSystemView.ROOT_PATH + realFile.getName(),
                realFile.getName());
        this.realFile = realFile;
    }

    protected final File getTmpDir() {
        return getFileSystemView().getTmpDir();
    }

    abstract protected TMina createFile();
    abstract protected TMina createFile(File realFile);

    @Override
    public ClientActionEvent.Storage getClientActionStorage() {
        return ClientActionEvent.Storage.QUICKSHARE;
    }

    public boolean isDirectory() {
        boolean result = realFile == null;
        logger.trace("[{}] isDirectory() -> {}", name, result);
        return result;
    }

    public boolean doesExist() {
        boolean result = realFile == null || realFile.exists();
        logger.trace("[{}] doesExist() -> {}", name, result);
        return result;
    }

    public boolean isReadable() {
        boolean result = realFile == null || realFile.canRead();
        logger.trace("[{}] isReadable() -> {}", name, result);
        return result;
    }

    public long getLastModified() {
        long result = realFile != null ? realFile.lastModified() : 0;
        logger.trace("[{}] getLastModified() -> {}", name, result);
        return result;
    }

    public long getSize() {
        long result = realFile != null ? realFile.length() : 0;
        logger.trace("[{}] getSize() -> {}", name, result);
        return result;
    }

    public boolean isFile() {
        boolean result = realFile != null;
        logger.trace("[{}] isFile() -> {}", name, result);
        return result;
    }

    public boolean isWritable() {
        logger.trace("[{}] isWritable()", name);
        return false;
    }

    public boolean isRemovable() {
        logger.trace("[{}] isRemovable()", name);
        return false;
    }

    public boolean setLastModified(long time) {
        logger.trace("[{}] setLastModified({})", name, time);
        return false;
    }

    public boolean mkdir() {
        logger.trace("[{}] mkdir()", name);
        return false;
    }

    public boolean delete() {
        logger.trace("[{}] delete()", name);
        return false;
    }

    public boolean move(AbstractFile<TFileSystemView> destination) {
        logger.trace("[{}] move({})", name, destination.getAbsolutePath());
        return false;
    }

    public List<TMina> listFiles() {
        logger.trace("[{}] listFiles()", name);
        postClientAction(ClientActionEvent.ClientAction.LIST_DIR);

        File[] filesArray = getTmpDir().listFiles();
        if (filesArray != null) {
            List<TMina> files = new ArrayList<>(filesArray.length);
            for (File file : filesArray) {
                files.add(createFile(file));
            }
            return files;
        }
        return new ArrayList<>(0);
    }

    public OutputStream createOutputStream(long offset) throws IOException {
        logger.trace("[{}] createOutputStream(offset: {})", name, offset);
        throw new IOException(String.format("Can not write file '%s'", absPath));
    }

    public InputStream createInputStream(long offset) throws IOException {
        logger.trace("[{}] createInputStream(offset: {})", name, offset);
        postClientAction(ClientActionEvent.ClientAction.DOWNLOAD);

        if (realFile == null) {
            throw new IOException(String.format("Can not read file '%s'", absPath));
        }
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(realFile));
        bis.skip(offset);
        return bis;
    }
}
