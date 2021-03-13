package org.primftpd.filesystem;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.SshFile;
import org.primftpd.services.PftpdService;

import java.io.File;

public class QuickShareSshFileSystemView extends QuickShareFileSystemView<QuickShareSshFile, SshFile> implements FileSystemView {

    private final Session session;

    public QuickShareSshFileSystemView(File quickShareFile, PftpdService pftpdService, Session session) {
        super(quickShareFile, pftpdService);
        this.session = session;
    }

    protected QuickShareSshFile createFile(File quickShareFile, String dir, PftpdService pftpdService) {
        return new QuickShareSshFile(quickShareFile, dir, pftpdService, session);
    }
    protected QuickShareSshFile createFile(File quickShareFile, PftpdService pftpdService) {
        return new QuickShareSshFile(quickShareFile, pftpdService, session);
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
