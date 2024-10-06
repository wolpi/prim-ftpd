package org.primftpd.filesystem;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.SshFile;
import org.primftpd.services.PftpdService;

import java.io.File;

public class QuickShareSshFileSystemView extends QuickShareFileSystemView<QuickShareSshFile, SshFile> implements FileSystemView {

    private final Session session;

    public QuickShareSshFileSystemView(PftpdService pftpdService, File tmpDir, Session session) {
        super(pftpdService, tmpDir);
        this.session = session;
    }

    protected QuickShareSshFile createFile() {
        return new QuickShareSshFile(this, session);
    }
    protected QuickShareSshFile createFile(File realFile) {
        return new QuickShareSshFile(this, realFile, session);
    }

    @Override
    public SshFile getFile(SshFile baseDir, String file) {
        logger.trace("getFile(baseDir: {}, file: {})", baseDir.getAbsolutePath(), file);
        // e.g. for scp
        return getFile(baseDir.getAbsolutePath() + "/" + file);
    }

    @Override
    public FileSystemView getNormalizedView() {
        logger.trace("getNormalizedView()");
        return this;
    }
}
