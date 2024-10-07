package org.primftpd.filesystem;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.SshFile;

import java.io.File;
import java.util.List;

public class QuickShareSshFile extends QuickShareFile<SshFile, QuickShareSshFileSystemView> implements SshFile {
    private final Session session;

    public QuickShareSshFile(QuickShareSshFileSystemView fileSystemView, Session session) {
        super(fileSystemView);
        this.session = session;
    }

    public QuickShareSshFile(QuickShareSshFileSystemView fileSystemView, File realFile, Session session) {
        super(fileSystemView, realFile);
        this.session = session;
    }

    @Override
    protected SshFile createFile() {
        return new QuickShareSshFile(getFileSystemView(), session);
    }

    @Override
    protected SshFile createFile(File realFile) {
        return new QuickShareSshFile(getFileSystemView(), realFile, session);
    }

    @Override
    public String getClientIp() {
        return SshUtils.getClientIp(session);
    }

    @Override
    public boolean move(SshFile target) {
		return super.move((QuickShareSshFile) target);
    }

    @Override
    public String getOwner() {
        logger.trace("[{}] getOwner()", name);
        return session.getUsername();
    }

    @Override
    public boolean create() {
        boolean result = false;
        logger.trace("[{}] create() -> {}", name, result);
        return result;
    }

    @Override
    public SshFile getParentFile() {
        logger.trace("[{}] getParentFile()", name);
        return new QuickShareSshFile(getFileSystemView(), session);
    }

    @Override
    public List<SshFile> listSshFiles() {
        return listFiles();
    }
}
