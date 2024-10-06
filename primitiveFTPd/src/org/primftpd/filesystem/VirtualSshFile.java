package org.primftpd.filesystem;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.SshFile;
import org.primftpd.services.PftpdService;

import java.io.IOException;
import java.util.List;

public class VirtualSshFile extends VirtualFile<SshFile, VirtualSshFileSystemView> implements SshFile {

    private final Session session;

    public VirtualSshFile(VirtualSshFileSystemView fileSystemView, String absPath, AbstractFile delegate, Session session) {
        super(fileSystemView, absPath, delegate);
        this.session = session;
    }

    public VirtualSshFile(VirtualSshFileSystemView fileSystemView, String absPath, boolean exists, Session session) {
        super(fileSystemView, absPath, exists);
        this.session = session;
    }

    @Override
    protected SshFile createFile(String absPath, AbstractFile delegate) {
        return new VirtualSshFile(getFileSystemView(), absPath, delegate, session);
    }

    @Override
    protected SshFile createFile(String absPath, boolean exists) {
        return new VirtualSshFile(getFileSystemView(), absPath, exists, session);
    }

    @Override
    protected List<SshFile> listDelegateFiles() {
        return ((SshFile) delegate).listSshFiles();
    }

    @Override
    public String getClientIp() {
        return SshUtils.getClientIp(session);
    }

    @Override
    public boolean move(SshFile target) {
        return super.move(((VirtualFile)target).delegate);
    }

    @Override
    public String getOwner() {
        logger.trace("[{}] getOwner()", name);
        return session.getUsername();
    }

    @Override
    public boolean create() throws IOException {
        // This call is required by SSHFS, because it calls STAT on created new files.
        // This call is not required by normal clients who simply open, write and close the file.
        boolean result = delegate != null && ((SshFile) delegate).create();
        logger.trace("[{}] create() -> {}", name, result);
        return result;
    }

    @Override
    public boolean isExecutable() {
        logger.trace("[{}] isExecutable()", name);
        return delegate != null ? delegate.isExecutable() : true;
    }

    @Override
    public SshFile getParentFile() {
        logger.trace("[{}] getParentFile()", name);
        return delegate != null ? ((SshFile)delegate).getParentFile() : null;
    }

    @Override
    public List<SshFile> listSshFiles() {
        return listFiles();
    }
}
