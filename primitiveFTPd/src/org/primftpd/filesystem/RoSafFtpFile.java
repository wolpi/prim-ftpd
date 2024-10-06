package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.primftpd.services.PftpdService;

public class RoSafFtpFile extends RoSafFile<FtpFile, RoSafFtpFileSystemView> implements FtpFile {

    private final User user;

    public RoSafFtpFile(
            RoSafFtpFileSystemView fileSystemView,
            String absPath,
            User user) {
        super(fileSystemView, absPath);
        this.user = user;
    }

    public RoSafFtpFile(
            RoSafFtpFileSystemView fileSystemView,
            String absPath,
            String docId,
            boolean exists,
            User user) {
        super(fileSystemView, absPath, docId, exists);
        this.user = user;
    }

    protected RoSafFtpFile(
            RoSafFtpFileSystemView fileSystemView,
            String absPath,
            Cursor cursor,
            User user) {
        super(fileSystemView, absPath, cursor);
        this.user = user;
    }

    @Override
    protected FtpFile createFile(String absPath, Cursor cursor) {
        return new RoSafFtpFile(getFileSystemView(), absPath, cursor, user);
    }

    @Override
    public String getClientIp() {
        return FtpUtils.getClientIp(user);
    }

    @Override
    public boolean move(FtpFile target) {
        return super.move((AbstractFile)target);
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
