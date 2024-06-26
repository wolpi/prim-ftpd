package org.primftpd.filesystem;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.SshFile;
import org.primftpd.services.PftpdService;

import java.io.IOException;
import java.util.List;

public class VirtualSshFile extends VirtualFile<SshFile> implements SshFile {

    private final Session session;

    public VirtualSshFile(String absPath, AbstractFile delegate, PftpdService pftpdService, Session session) {
        super(absPath, delegate, pftpdService);
        this.session = session;
    }

    public VirtualSshFile(String absPath, AbstractFile delegate, boolean exists, PftpdService pftpdService, Session session) {
        super(absPath, delegate, exists, pftpdService);
        this.session = session;
    }

    @Override
    protected SshFile createFile(String absPath, AbstractFile delegate, PftpdService pftpdService) {
        return new VirtualSshFile(absPath, delegate, pftpdService, session);
    }

    @Override
    protected SshFile createFile(String absPath, AbstractFile delegate, boolean exists, PftpdService pftpdService) {
        return new VirtualSshFile(absPath, delegate, exists, pftpdService, session);
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
        logger.trace("move()");
        SshFile realTarget = (SshFile) ((VirtualSshFile) target).delegate;
        return delegate != null && ((SshFile) delegate).move(realTarget);
    }

    @Override
    public String getOwner() {
        logger.trace("[{}] getOwner()", name);
        return session.getUsername();
    }

    @Override
    public boolean create() throws IOException {
        logger.trace("[{}] create()", name);
        // This call and the update of the cached properties is required by SSHFS, because it calls STAT and later FSTAT on created new files,
        // STAT requires a created new file, FSTAT requires updated properties.
        // This call is not required by normal clients who simply open, write and close the file.
        if (delegate != null && ((SshFile) delegate).create()) {
            lastModified = delegate.getLastModified();
            size = 0;
            readable = delegate.isReadable();
            exists = true;
            return true;
        }
        return false;
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
