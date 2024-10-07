package org.primftpd.filesystem;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.primftpd.pojo.LsOutputBean;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RootFtpFile extends RootFile<FtpFile, RootFtpFileSystemView> implements FtpFile {

    private final User user;

    public RootFtpFile(RootFtpFileSystemView fileSystemView, String absPath, LsOutputBean bean, User user) {
        super(fileSystemView, absPath, bean);
        this.user = user;
    }

    protected RootFtpFile createFile(String absPath, LsOutputBean bean) {
        return new RootFtpFile(getFileSystemView(), absPath, bean, user);
    }

    @Override
    public String getClientIp() {
        return FtpUtils.getClientIp(user);
    }

    @Override
    public OutputStream createOutputStream(long offset) throws IOException {
        OutputStream superStream = super.createOutputStream(offset);
        return new BufferedOutputStream(superStream) {
            @Override
            public void close() throws IOException {
                super.close();
                logger.trace("calling sftp handleClose() for ftp file");
                handleClose();
            }
        };
    }

    @Override
    public InputStream createInputStream(long offset) throws IOException {
        InputStream superStream = super.createInputStream(offset);
        return new BufferedInputStream(superStream) {
            @Override
            public void close() throws IOException {
                super.close();
                logger.trace("calling sftp handleClose() for ftp file");
                handleClose();
            }
        };
    }

    @Override
    public boolean move(FtpFile target) {
        return super.move((RootFtpFile)target);
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
