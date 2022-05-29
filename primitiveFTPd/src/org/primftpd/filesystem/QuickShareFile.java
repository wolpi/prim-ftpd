package org.primftpd.filesystem;

import org.primftpd.events.ClientActionEvent;
import org.primftpd.services.PftpdService;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class QuickShareFile<T> extends AbstractFile {

    protected final File tmpDir;
    protected final File realFile;

    QuickShareFile(File tmpDir, PftpdService pftpdService) {
        // this c-tor is to be used to access fake directory
        super(
                QuickShareFileSystemView.ROOT_PATH,
                QuickShareFileSystemView.ROOT_PATH,
                0,
                0,
                true,
                true,
                true,
                pftpdService);
        this.tmpDir = tmpDir;
        this.realFile = null;
    }

    QuickShareFile(File tmpDir, File realFile, PftpdService pftpdService) {
        // this c-tor is to be used to access actual file
        super(
                QuickShareFileSystemView.ROOT_PATH + realFile.getName(),
                realFile.getName(),
                realFile.lastModified(),
                realFile.length(),
                realFile.canRead(),
                realFile.exists(),
                false,
                pftpdService);
        this.tmpDir = tmpDir;
        this.realFile = realFile;
    }

    abstract protected T createFile(File tmpDir, PftpdService pftpdService);
    abstract protected T createFile(File tmpDir, File realFile, PftpdService pftpdService);

    @Override
    public ClientActionEvent.Storage getClientActionStorage() {
        return ClientActionEvent.Storage.QUICKSHARE;
    }

    public boolean isFile() {
        boolean result = !isDirectory;
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

    public boolean move(SafFile<T> destination) {
        logger.trace("[{}] move({})", name, destination.getAbsolutePath());
        return false;
    }

    public List<T> listFiles() {
        logger.trace("[{}] listFiles()", name);
        postClientAction(ClientActionEvent.ClientAction.LIST_DIR);

        File[] filesArray = tmpDir.listFiles();
        if (filesArray != null) {
            List<T> files = new ArrayList<>(filesArray.length);
            for (File file : filesArray) {
                files.add(createFile(tmpDir, file, pftpdService));
            }
            return files;
        }
        return new ArrayList<>(0);
    }

    public OutputStream createOutputStream(long offset) {
        logger.trace("[{}] createOutputStream(offset: {})", name, offset);
        return new ByteArrayOutputStream();
    }

    public InputStream createInputStream(long offset) throws IOException {
        logger.trace("[{}] createInputStream(offset: {})", name, offset);
        postClientAction(ClientActionEvent.ClientAction.DOWNLOAD);

        if (realFile != null) {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(realFile));
            bis.skip(offset);
            return bis;
        }

        return null;
    }
}
