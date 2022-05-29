package org.primftpd.filesystem;

import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.primftpd.services.PftpdService;

import java.io.File;

public class QuickShareFtpFileSystemView extends QuickShareFileSystemView<QuickShareFtpFile, FtpFile> implements FileSystemView {

    private final User user;

    public QuickShareFtpFileSystemView(File quickShareFile, User user, PftpdService pftpdService) {
        super(quickShareFile, pftpdService);
        this.user = user;
    }

    protected QuickShareFtpFile createFile(File tmpDir, PftpdService pftpdService) {
        return new QuickShareFtpFile(tmpDir, pftpdService, user);
    }
    protected QuickShareFtpFile createFile(File tmpDir, File realFile, PftpdService pftpdService) {
        return new QuickShareFtpFile(tmpDir, realFile, pftpdService, user);
    }

    public QuickShareFtpFile getHomeDirectory() {
        logger.trace("getHomeDirectory()");

        return createFile(tmpDir, pftpdService);
    }

    public QuickShareFtpFile getWorkingDirectory() {
        logger.trace("getWorkingDirectory()");

        return createFile(tmpDir, pftpdService);
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
