package org.primftpd.filesystem;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.SshFile;
import org.primftpd.services.PftpdService;

import java.io.File;

public class VirtualSshFileSystemView extends VirtualFileSystemView<
        FsSshFile,
        RootSshFile,
        SafSshFile,
        RoSafSshFile,
        SshFile> implements FileSystemView {

    private final File homeDir;
    private final Session session;

    public VirtualSshFileSystemView(
            PftpdService pftpdService,
            FsSshFileSystemView fsFileSystemView,
            RootSshFileSystemView rootFileSystemView,
            SafSshFileSystemView safFileSystemView,
            RoSafSshFileSystemView roSafFileSystemView,
            File homeDir,
            Session session) {
        super(pftpdService, fsFileSystemView, rootFileSystemView, safFileSystemView, roSafFileSystemView);
        this.homeDir = homeDir;
        this.session = session;
    }

    @Override
    public SshFile createFile(String absPath, AbstractFile delegate) {
        return new VirtualSshFile(this, absPath, delegate, session);
    }

    @Override
    public SshFile createFile(String absPath, boolean exists) {
        return new VirtualSshFile(this, absPath, exists, session);
    }

    @Override
    protected String absolute(String file) {
        return Utils.absoluteOrHome(file, "/" + PREFIX_FS + homeDir.getAbsolutePath());
    }

    @Override
    public SshFile getFile(SshFile baseDir, String file) {
        logger.trace("getFile(baseDir: {}, file: {})", baseDir.getAbsolutePath(), file);
        // e.g. for scp
        return getFile(baseDir.getAbsolutePath() + "/" + file);
    }

    @Override
    public FileSystemView getNormalizedView() {
        return this;
    }
}
