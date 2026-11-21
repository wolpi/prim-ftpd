package org.primftpd.filesystem;

import org.primftpd.events.ClientActionEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class VirtualFile<TMina, TFileSystemView extends VirtualFileSystemView> extends AbstractFile<TFileSystemView> {

    protected final AbstractFile<TFileSystemView> delegate;
    protected final boolean exists;

    private VirtualFile(
            TFileSystemView fileSystemView,
            String absPath,
            AbstractFile<TFileSystemView> delegate,
            boolean exists) {
        super(
            fileSystemView,
            absPath,
            delegate != null
                    ? delegate.getName()
                    : absPath.length() > 1 && absPath.charAt(0) == '/'
                        ? absPath.substring(1)
                        : absPath);
        this.delegate = delegate;
        this.exists = exists;
    }

    public VirtualFile(
            TFileSystemView fileSystemView,
            String absPath,
            AbstractFile<TFileSystemView> delegate) {
        this(fileSystemView, absPath, delegate, true);
    }

    public VirtualFile(
            TFileSystemView fileSystemView,
            String absPath,
            boolean exists) {
        this(fileSystemView, absPath, null, exists);
    }

    protected abstract TMina createFile(
            String absPath,
            AbstractFile<TFileSystemView> delegate);

    protected abstract TMina createFile(
            String absPath,
            boolean exists);

    protected abstract List<TMina> listDelegateFiles();

    public ClientActionEvent.Storage getClientActionStorage() {
        return delegate != null ? delegate.getClientActionStorage() : ClientActionEvent.Storage.VIRTUAL;
    }

    public boolean isDirectory() {
        return delegate == null || delegate.isDirectory();
    }

    public boolean doesExist() {
        return delegate != null ? delegate.doesExist() : exists;
    }

    public boolean isReadable() {
        return delegate == null || delegate.isReadable();
    }

    public long getLastModified() {
        return delegate != null ? delegate.getLastModified() : 0;
    }

    public long getSize() {
        return delegate != null ? delegate.getSize() : 0;
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

    public boolean move(AbstractFile<TFileSystemView> target) {
        // moving between different kinds of storage is not supported
        return delegate != null
                && target != null
                && delegate.getClass() == target.getClass()
                && delegate.move(target);
    }

    public List<TMina> listFiles() {
        if ("/".equals(absPath)) {
            List<TMina> files = new ArrayList<>(4);
            files.add(createFile("/" + VirtualFileSystemView.PREFIX_FS, null));
            files.add(createFile("/" + VirtualFileSystemView.PREFIX_ROOT, null));
            files.add(createFile("/" + VirtualFileSystemView.PREFIX_SAF, null));
            files.add(createFile("/" + VirtualFileSystemView.PREFIX_ROSAF, null));
            return Collections.unmodifiableList(files);
        }
        if (delegate == null) {
            return new ArrayList<>(0);
        }
        return listDelegateFiles();
    }

    public OutputStream createOutputStream(long offset) throws IOException {
        if (delegate == null) {
            throw new IOException(String.format("Can not write file '%s'", absPath));
        }
        return delegate.createOutputStream(offset);
    }

    public InputStream createInputStream(long offset) throws IOException {
        if (delegate == null) {
            throw new IOException(String.format("Can not read file '%s'", absPath));
        }
        return delegate.createInputStream(offset);
    }
}
