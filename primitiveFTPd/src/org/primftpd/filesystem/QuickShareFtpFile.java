package org.primftpd.filesystem;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;

import java.io.File;

public class QuickShareFtpFile extends QuickShareFile<FtpFile> implements FtpFile {
    private final User user;

    QuickShareFtpFile(File quickShareFile, String dir, User user) {
        super(quickShareFile, dir);
        this.user = user;
    }

    QuickShareFtpFile(File quickShareFile, User user) {
        super(quickShareFile);
        this.user = user;
    }

    @Override
    protected FtpFile createFile(File quickShareFile, String dir) {
        return new QuickShareFtpFile(quickShareFile, dir, user);
    }

    @Override
    protected FtpFile createFile(File quickShareFile) {
        return new QuickShareFtpFile(quickShareFile, user);
    }

    @Override
    public boolean move(FtpFile target) {
        return false;
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
