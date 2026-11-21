package org.primftpd.filesystem;

import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.primftpd.services.PftpdService;

import java.io.File;

public class QuickShareFtpFileSystemView extends QuickShareFileSystemView<QuickShareFtpFile, FtpFile> implements FileSystemView {

    private final User user;

    public QuickShareFtpFileSystemView(PftpdService pftpdService, File tmpDir, User user) {
        super(pftpdService, tmpDir);
        this.user = user;
    }

    protected QuickShareFtpFile createFile() {
        return new QuickShareFtpFile(this, user);
    }
    protected QuickShareFtpFile createFile(File realFile) {
        return new QuickShareFtpFile(this, realFile, user);
    }

    public QuickShareFtpFile getHomeDirectory() {
        logger.trace("getHomeDirectory()");

        // just tell clients: we are on root
        return createFile();
    }

    public QuickShareFtpFile getWorkingDirectory() {
        logger.trace("getWorkingDirectory()");

        // just tell clients: we are on root
        return createFile();
    }

    public boolean changeWorkingDirectory(String dir) {
        logger.trace("changeWorkingDirectory({}) - ignoring", dir);

        // just return true and stay in the only dir
        return true;
    }

    public boolean isRandomAccessible() {
        logger.trace("isRandomAccessible()");

        return true;
    }

    public void dispose() {
        logger.trace("dispose()");
    }
}
