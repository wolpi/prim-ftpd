package org.primftpd.filesystem;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.SshFile;
import org.primftpd.pojo.LsOutputBean;

import java.io.File;

public class RootSshFileSystemView extends RootFileSystemView<RootSshFile, SshFile> implements FileSystemView {

    private final File homeDir;
    private final Session session;

    public RootSshFileSystemView(File homeDir, Session session) {
        this.homeDir = homeDir;
        this.session = session;
    }

    @Override
    protected RootSshFile createFile(LsOutputBean bean, String absPath) {
        return new RootSshFile(bean, absPath, session);
    }

    @Override
    protected String absolute(String file) {
        if (".".equals(file)) {
            return homeDir.getAbsolutePath();
        }
        // is abs always
        return file;
    }

    @Override
    public SshFile getFile(SshFile baseDir, String file) {
        logger.trace("getFile(baseDir, {})", file);
        return null;
    }

    @Override
    public FileSystemView getNormalizedView() {
        logger.trace("getNormalizedView()");
        return this;
    }
}
