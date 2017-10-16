package org.primftpd.filesystem;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.primftpd.pojo.LsOutputBean;

public class RootFtpFile extends RootFile<FtpFile> implements FtpFile {

    private final User user;

    public RootFtpFile(LsOutputBean bean, String absPath, User user) {
        super(bean, absPath);
        this.user = user;
    }

    protected RootFtpFile createFile(LsOutputBean bean, String absPath) {
        return new RootFtpFile(bean, absPath, user);
    }

    @Override
    public boolean move(FtpFile target) {
        logger.trace("move()");
        return super.move((RootFile)target);
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
    public boolean isHidden() {
        logger.trace("[{}] isHidden()", name);
        return name.charAt(0) == '.';
    }

    @Override
    public int getLinkCount() {
        logger.trace("[{}] getLinkCount()", name);
        return 0;
    }


    public User getUser() {
        return user;
    }

    @Override
    public Object getPhysicalFile() {
        return this;
    }
}
