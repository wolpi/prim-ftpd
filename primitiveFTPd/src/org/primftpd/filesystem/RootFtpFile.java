package org.primftpd.filesystem;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.primftpd.services.PftpdService;
import org.primftpd.pojo.LsOutputBean;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import eu.chainfire.libsuperuser.Shell;

public class RootFtpFile extends RootFile<FtpFile> implements FtpFile {

    private final User user;

    public RootFtpFile(Shell.Interactive shell, LsOutputBean bean, String absPath, PftpdService pftpdService, User user) {
        super(shell, bean, absPath, pftpdService);
        this.user = user;
    }

    protected RootFtpFile createFile(Shell.Interactive shell, LsOutputBean bean, String absPath, PftpdService pftpdService) {
        return new RootFtpFile(shell, bean, absPath, pftpdService, user);
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
}
