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
import java.util.Collections;
import java.util.List;

public abstract class QuickShareFile<T> extends AbstractFile {

    protected File quickShareFile;

    QuickShareFile(File quickShareFile, String dir, PftpdService pftpdService) {
        // this c-tor is to be used to access fake directory
        super(
                dir,
                dir,
                0,
                0,
                true,
                true,
                true,
                pftpdService);
        this.quickShareFile = quickShareFile;
    }

    QuickShareFile(File quickShareFile, PftpdService pftpdService) {
        // this c-tor is to be used to access actual file
        super(
                QuickShareFileSystemView.ROOT_PATH + quickShareFile.getName(),
                quickShareFile.getName(),
                quickShareFile.lastModified(),
                quickShareFile.length(),
                quickShareFile.canRead(),
                quickShareFile.exists(),
                false,
                pftpdService);
        this.quickShareFile = quickShareFile;
    }

    abstract protected T createFile(File quickShareFile, String dir, PftpdService pftpdService);
    abstract protected T createFile(File quickShareFile, PftpdService pftpdService);

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

        T result = createFile(quickShareFile, pftpdService);
        return Collections.singletonList(result);
    }

    public OutputStream createOutputStream(long offset) {
        logger.trace("[{}] createOutputStream(offset: {})", name, offset);
        postClientAction(ClientActionEvent.ClientAction.UPLOAD);
        return new ByteArrayOutputStream();
    }

    public InputStream createInputStream(long offset) throws IOException {
        logger.trace("[{}] createInputStream(offset: {})", name, offset);
        postClientAction(ClientActionEvent.ClientAction.DOWNLOAD);

        if (quickShareFile != null) {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(quickShareFile));
            bis.skip(offset);
            return bis;
        }

        return null;
    }
}
