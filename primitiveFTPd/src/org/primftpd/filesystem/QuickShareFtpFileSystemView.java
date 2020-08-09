package org.primftpd.filesystem;

import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;

import java.io.File;

public class QuickShareFtpFileSystemView extends QuickShareFileSystemView<QuickShareFtpFile, FtpFile> implements FileSystemView {

    private final User user;

    public QuickShareFtpFileSystemView(File quickShareFile, User user) {
        super(quickShareFile);
        this.user = user;
    }

    protected QuickShareFtpFile createFile(File quickShareFile, String dir) {
        return new QuickShareFtpFile(quickShareFile, dir, user);
    }
    protected QuickShareFtpFile createFile(File quickShareFile) {
        return new QuickShareFtpFile(quickShareFile, user);
    }

    public QuickShareFtpFile getHomeDirectory() {
        logger.trace("getHomeDirectory()");

        return createFile(quickShareFile, QuickShareFileSystemView.ROOT_PATH);
    }

    public QuickShareFtpFile getWorkingDirectory() {
        logger.trace("getWorkingDirectory()");

        return createFile(quickShareFile, QuickShareFileSystemView.ROOT_PATH);
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
