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

        return createFile(tmpDir);
    }

    public QuickShareFtpFile getWorkingDirectory() {
        logger.trace("getWorkingDirectory()");

        return createFile(tmpDir);
    }

    public boolean changeWorkingDirectory(String dir) {
        logger.trace("changeWorkingDirectory({})", dir);

        return false;
    }

    public boolean isRandomAccessible() {
        logger.trace("isRandomAccessible()");

        return true;
    }

    public void dispose() {
        logger.trace("dispose()");
    }
}
