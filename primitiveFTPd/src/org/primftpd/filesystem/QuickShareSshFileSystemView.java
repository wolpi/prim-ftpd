package org.primftpd.filesystem;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.SshFile;
import org.primftpd.events.ClientActionPoster;

import java.io.File;

public class QuickShareSshFileSystemView extends QuickShareFileSystemView<QuickShareSshFile, SshFile> implements FileSystemView {

    private final Session session;

    public QuickShareSshFileSystemView(File quickShareFile, ClientActionPoster clientActionPoster, Session session) {
        super(quickShareFile, clientActionPoster);
        this.session = session;
    }

    protected QuickShareSshFile createFile(File quickShareFile, String dir, ClientActionPoster clientActionPoster) {
        return new QuickShareSshFile(quickShareFile, dir, clientActionPoster, session);
    }
    protected QuickShareSshFile createFile(File quickShareFile, ClientActionPoster clientActionPoster) {
        return new QuickShareSshFile(quickShareFile, clientActionPoster, session);
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
