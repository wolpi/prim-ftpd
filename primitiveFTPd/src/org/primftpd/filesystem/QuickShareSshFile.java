package org.primftpd.filesystem;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.SshFile;
import org.primftpd.services.PftpdService;

import java.io.File;
import java.util.List;

public class QuickShareSshFile extends QuickShareFile<SshFile> implements SshFile {
    private final Session session;

    QuickShareSshFile(File tmpDir, PftpdService pftpdService, Session session) {
        super(tmpDir, pftpdService);
        this.session = session;
    }

    QuickShareSshFile(File tmpDir, File realFile, PftpdService pftpdService, Session session) {
        super(tmpDir, realFile, pftpdService);
        this.session = session;
    }

    @Override
    protected SshFile createFile(File tmpDir, PftpdService pftpdService) {
        return new QuickShareSshFile(tmpDir, pftpdService, session);
    }

    @Override
    protected SshFile createFile(File tmpDir, File realFile, PftpdService pftpdService) {
        return new QuickShareSshFile(tmpDir, realFile, pftpdService, session);
    }

    @Override
    public String getClientIp() {
        return SshUtils.getClientIp(session);
    }

    @Override
    public boolean move(org.apache.sshd.common.file.SshFile target) {
        return false;
    }

    @Override
    public String getOwner() {
        logger.trace("[{}] getOwner()", name);
        return session.getUsername();
    }

    @Override
    public boolean create() {
        logger.trace("[{}] create()", name);
        return false;
    }

    @Override
    public SshFile getParentFile() {
        logger.trace("[{}] getParentFile()", name);
        return new QuickShareSshFile(tmpDir, pftpdService, session);
    }

    @Override
    public boolean isExecutable() {
        logger.trace("[{}] isExecutable()", name);
        return false;
    }

    @Override
    public List<SshFile> listSshFiles() {
        return listFiles();
    }
}
