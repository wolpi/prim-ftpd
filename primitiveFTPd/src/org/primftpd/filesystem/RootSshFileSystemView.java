package org.primftpd.filesystem;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.SshFile;
import org.primftpd.services.PftpdService;
import org.primftpd.pojo.LsOutputBean;

import java.io.File;

import eu.chainfire.libsuperuser.Shell;

public class RootSshFileSystemView extends RootFileSystemView<RootSshFile, SshFile> implements FileSystemView {

    private final File homeDir;
    private final Session session;

    public RootSshFileSystemView(Shell.Interactive shell, PftpdService pftpdService, File homeDir, Session session) {
        super(shell, pftpdService);
        this.homeDir = homeDir;
        this.session = session;
    }

    @Override
    protected RootSshFile createFile(LsOutputBean bean, String absPath, PftpdService pftpdService) {
        return new RootSshFile(shell, bean, absPath, pftpdService, session, this);
    }

    @Override
    protected String absolute(String file) {
        return Utils.absoluteOrHome(file, homeDir.getAbsolutePath());
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
