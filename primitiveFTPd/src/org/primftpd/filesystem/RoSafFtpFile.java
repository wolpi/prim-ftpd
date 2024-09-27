package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.primftpd.services.PftpdService;

public class RoSafFtpFile extends RoSafFile<FtpFile> implements FtpFile {

    private final User user;

    public RoSafFtpFile(
            ContentResolver contentResolver,
            Uri startUrl,
            String absPath,
            PftpdService pftpdService,
            RoSafFtpFileSystemView fileSystemView,
            User user) {
        super(contentResolver, startUrl, absPath, pftpdService, fileSystemView);
        this.user = user;
    }

    public RoSafFtpFile(
            ContentResolver contentResolver,
            Uri startUrl,
            String docId,
            String absPath,
            boolean exists,
            PftpdService pftpdService,
            RoSafFtpFileSystemView fileSystemView,
            User user) {
        super(contentResolver, startUrl, docId, absPath, exists, pftpdService, fileSystemView);
        this.user = user;
    }

    public RoSafFtpFile(
            ContentResolver contentResolver,
            Uri startUrl,
            Cursor cursor,
            String absPath,
            PftpdService pftpdService,
            RoSafFtpFileSystemView fileSystemView,
            User user) {
        super(contentResolver, startUrl, cursor, absPath, pftpdService, fileSystemView);
        this.user = user;
    }

    private RoSafFtpFileSystemView getFileSystemView() {
        return (RoSafFtpFileSystemView)fileSystemView;
    }

    @Override
    protected FtpFile createFile(
            ContentResolver contentResolver,
            Uri startUrl,
            Cursor cursor,
            String absPath,
            PftpdService pftpdService) {
        return new RoSafFtpFile(contentResolver, startUrl, cursor, absPath, pftpdService, getFileSystemView(), user);
    }

    @Override
    public String getClientIp() {
        return FtpUtils.getClientIp(user);
    }

    @Override
    public boolean move(FtpFile target) {
        logger.trace("move()");
        return super.move((RoSafFile)target);
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
