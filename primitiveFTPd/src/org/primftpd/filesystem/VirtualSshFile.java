package org.primftpd.filesystem;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.SshFile;
import org.primftpd.services.PftpdService;

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
        return delegate != null && ((SshFile) delegate).move((SshFile) ((VirtualSshFile) target).delegate);
    }

    @Override
    public String getOwner() {
        logger.trace("[{}] getOwner()", name);
        return session.getUsername();
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
