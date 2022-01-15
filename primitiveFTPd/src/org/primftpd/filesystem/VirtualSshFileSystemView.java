package org.primftpd.filesystem;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.SshFile;
import org.primftpd.services.PftpdService;

public class VirtualSshFileSystemView extends VirtualFileSystemView<
        SshFile,
        FsSshFile,
        RootSshFile,
        SafSshFile,
        RoSafSshFile> implements FileSystemView {

    private final Session session;

    public VirtualSshFileSystemView(
            FsSshFileSystemView fsFileSystemView,
            RootSshFileSystemView rootFileSystemView,
            SafSshFileSystemView safFileSystemView,
            RoSafSshFileSystemView roSafFileSystemView,
            PftpdService pftpdService,
            Session session) {
        super(fsFileSystemView, rootFileSystemView, safFileSystemView, roSafFileSystemView, pftpdService);
        this.session = session;
    }

    @Override
    public SshFile createFile(String absPath, AbstractFile delegate, PftpdService pftpdService) {
        return new VirtualSshFile(absPath, delegate, pftpdService, session);
    }

    @Override
    public SshFile createFile(String absPath, AbstractFile delegate, boolean exists, PftpdService pftpdService) {
        return new VirtualSshFile(absPath, delegate, exists, pftpdService, session);
    }

    @Override
    protected String absolute(String file) {
        return Utils.absoluteOrHome(file, "/");
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
