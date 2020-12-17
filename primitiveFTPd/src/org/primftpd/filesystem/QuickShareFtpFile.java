package org.primftpd.filesystem;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.primftpd.events.ClientActionPoster;

import java.io.File;

public class QuickShareFtpFile extends QuickShareFile<FtpFile> implements FtpFile {
    private final User user;

    QuickShareFtpFile(File quickShareFile, String dir, ClientActionPoster clientActionPoster, User user) {
        super(quickShareFile, dir, clientActionPoster);
        this.user = user;
    }

    QuickShareFtpFile(File quickShareFile, ClientActionPoster clientActionPoster, User user) {
        super(quickShareFile, clientActionPoster);
        this.user = user;
    }

    @Override
    public String getClientIp() {
        return FtpUtils.getClientIp(user);
    }

    @Override
    protected FtpFile createFile(File quickShareFile, String dir, ClientActionPoster clientActionPoster) {
        return new QuickShareFtpFile(quickShareFile, dir, clientActionPoster, user);
    }

    @Override
    protected FtpFile createFile(File quickShareFile, ClientActionPoster clientActionPoster) {
        return new QuickShareFtpFile(quickShareFile, clientActionPoster, user);
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
