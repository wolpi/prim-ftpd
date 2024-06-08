package org.primftpd.filesystem;

import org.primftpd.events.ClientActionEvent;
import org.primftpd.services.PftpdService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class VirtualFile <T> extends AbstractFile {

    protected final AbstractFile delegate;

    public VirtualFile(
            String absPath,
            AbstractFile delegate,
            boolean exists,
            PftpdService pftpdService) {
        super(
                absPath,
                delegate != null ? delegate.getName() : absPath.length() > 1 && absPath.charAt(0) == '/' ? absPath.substring(1) : absPath,
                delegate != null ? delegate.getLastModified() : 0,
                delegate != null ? delegate.getSize() : 0,
                delegate == null || delegate.isReadable(),
                exists,
                delegate == null || delegate.isDirectory,
                pftpdService);
        this.delegate = delegate;
    }

    public VirtualFile(
            String absPath,
            AbstractFile delegate,
            PftpdService pftpdService) {
        this(absPath, delegate, delegate == null || delegate.exists, pftpdService);
    }

    protected abstract T createFile(
            String absPath,
            AbstractFile delegate,
            PftpdService pftpdService);

    protected abstract T createFile(
            String absPath,
            AbstractFile delegate,
            boolean exists,
            PftpdService pftpdService);

    protected abstract List<T> listDelegateFiles();

    public ClientActionEvent.Storage getClientActionStorage() {
        return delegate.getClientActionStorage();
    }

    public boolean isFile() {
        return delegate != null && delegate.isFile();
    }

    public boolean isWritable() {
        return delegate != null && delegate.isWritable();
    }

    public boolean isRemovable() {
        return delegate != null && delegate.isRemovable();
    }

    public boolean setLastModified(long time) {
        return delegate != null && delegate.setLastModified(time);
    }

    public boolean mkdir() {
        return delegate != null && delegate.mkdir();
    }

    public boolean delete() {
        return delegate != null && delegate.delete();
    }

    public List<T> listFiles() {
        if ("/".equals(absPath)) {
            List<T> files = new ArrayList<>(4);
            files.add(createFile("/" + VirtualFileSystemView.PREFIX_FS, null, pftpdService));
            files.add(createFile("/" + VirtualFileSystemView.PREFIX_ROOT, null, pftpdService));
            files.add(createFile("/" + VirtualFileSystemView.PREFIX_SAF, null, pftpdService));
            files.add(createFile("/" + VirtualFileSystemView.PREFIX_ROSAF, null, pftpdService));
            return Collections.unmodifiableList(files);
        }
        return listDelegateFiles();
    }

    public OutputStream createOutputStream(long offset) throws IOException {
        return delegate != null ? delegate.createOutputStream(offset) : null;
    }

    public InputStream createInputStream(long offset) throws IOException {
        return delegate != null ? delegate.createInputStream(offset) : null;
    }

}
