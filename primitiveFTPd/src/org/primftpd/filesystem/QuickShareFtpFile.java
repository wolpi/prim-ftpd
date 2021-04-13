package org.primftpd.filesystem;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.primftpd.services.PftpdService;

import java.io.File;

public class QuickShareFtpFile extends QuickShareFile<FtpFile> implements FtpFile {
    private final User user;

    QuickShareFtpFile(File quickShareFile, String dir, PftpdService pftpdService, User user) {
        super(quickShareFile, dir, pftpdService);
        this.user = user;
    }

    QuickShareFtpFile(File quickShareFile, PftpdService pftpdService, User user) {
        super(quickShareFile, pftpdService);
        this.user = user;
    }

    @Override
    public String getClientIp() {
        return FtpUtils.getClientIp(user);
    }

    @Override
    protected FtpFile createFile(File quickShareFile, String dir, PftpdService pftpdService) {
        return new QuickShareFtpFile(quickShareFile, dir, pftpdService, user);
    }

    @Override
    protected FtpFile createFile(File quickShareFile, PftpdService pftpdService) {
        return new QuickShareFtpFile(quickShareFile, pftpdService, user);
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
