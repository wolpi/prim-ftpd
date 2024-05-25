package org.primftpd.filesystem;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.primftpd.services.PftpdService;

import java.util.List;

public class VirtualFtpFile extends VirtualFile<FtpFile> implements FtpFile {

    private final User user;

    public VirtualFtpFile(String absPath, AbstractFile delegate, PftpdService pftpdService, User user) {
        super(absPath, delegate, pftpdService);
        this.user = user;
    }

    public VirtualFtpFile(String absPath, AbstractFile delegate, boolean exists, PftpdService pftpdService, User user) {
        super(absPath, delegate, exists, pftpdService);
        this.user = user;
    }

    @Override
    protected FtpFile createFile(String absPath, AbstractFile delegate, PftpdService pftpdService) {
        return new VirtualFtpFile(absPath, delegate, pftpdService, user);
    }

    @Override
    protected FtpFile createFile(String absPath, AbstractFile delegate, boolean exists, PftpdService pftpdService) {
        return new VirtualFtpFile(absPath, delegate, exists, pftpdService, user);
    }

    @Override
    protected List<FtpFile> listDelegateFiles() {
        List<? extends FtpFile> files = ((FtpFile) delegate).listFiles();
        return (List<FtpFile>)files;
    }

    @Override
    public String getClientIp() {
        return FtpUtils.getClientIp(user);
    }

    @Override
    public boolean move(FtpFile target) {
        logger.trace("move()");
        return delegate != null && ((FtpFile) delegate).move((FtpFile) ((VirtualFtpFile) target).delegate);
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
}
