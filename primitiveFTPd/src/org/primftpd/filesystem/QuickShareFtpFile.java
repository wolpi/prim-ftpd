package org.primftpd.filesystem;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;

import java.io.File;

public class QuickShareFtpFile extends QuickShareFile<FtpFile, QuickShareFtpFileSystemView> implements FtpFile {
    private final User user;

    public QuickShareFtpFile(QuickShareFtpFileSystemView fileSystemView, User user) {
        super(fileSystemView);
        this.user = user;
    }

    public QuickShareFtpFile(QuickShareFtpFileSystemView fileSystemView, File realFile, User user) {
        super(fileSystemView, realFile);
        this.user = user;
    }

    @Override
    public String getClientIp() {
        return FtpUtils.getClientIp(user);
    }

    @Override
    protected FtpFile createFile() {
        return new QuickShareFtpFile(getFileSystemView(), user);
    }

    @Override
    protected FtpFile createFile(File realFile) {
        return new QuickShareFtpFile(getFileSystemView(), realFile, user);
    }

    @Override
    public boolean move(FtpFile target) {
		return super.move((QuickShareFtpFile) target);
    }

    @Override
    public String getOwnerName() {
        logger.trace("[{}] getOwnerName()", name);
        return user.getName();
    }

    @Override
    public String getGroupName() {
        logger.trace("[{}] getGroupName()", name);
        return user.getName();
    }

    @Override
    public Object getPhysicalFile() {
        logger.trace("[{}] getPhysicalFile()", name);
        return this;
    }
}
