package org.primftpd.filesystem;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;

import java.util.List;

public class VirtualFtpFile extends VirtualFile<FtpFile, VirtualFtpFileSystemView> implements FtpFile {

    private final User user;

    public VirtualFtpFile(VirtualFtpFileSystemView fileSystemView, String absPath, AbstractFile<VirtualFtpFileSystemView> delegate, User user) {
        super(fileSystemView, absPath, delegate);
        this.user = user;
    }

    public VirtualFtpFile(VirtualFtpFileSystemView fileSystemView, String absPath, boolean exists, User user) {
        super(fileSystemView, absPath, exists);
        this.user = user;
    }

    @Override
    protected FtpFile createFile(String absPath, AbstractFile<VirtualFtpFileSystemView> delegate) {
        return new VirtualFtpFile(getFileSystemView(), absPath, delegate, user);
    }

    @Override
    protected FtpFile createFile(String absPath, boolean exists) {
        return new VirtualFtpFile(getFileSystemView(), absPath, exists, user);
    }

    @Override
    protected List<FtpFile> listDelegateFiles() {
        List<? extends FtpFile> files = ((FtpFile) delegate).listFiles();
        return (List<FtpFile>) files;
    }

    @Override
    public String getClientIp() {
        return FtpUtils.getClientIp(user);
    }

    @Override
    public boolean move(FtpFile target) {
        return super.move(((VirtualFtpFile)target).delegate);
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
