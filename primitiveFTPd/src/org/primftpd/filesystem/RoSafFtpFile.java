package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;

public class RoSafFtpFile extends RoSafFile<FtpFile> implements FtpFile {

    private final User user;

    public RoSafFtpFile(
            ContentResolver contentResolver,
            Uri startUrl,
            String absPath,
            User user) {
        super(contentResolver, startUrl, absPath);
        this.user = user;
    }

    public RoSafFtpFile(
            ContentResolver contentResolver,
            Uri startUrl,
            String docId,
            String absPath,
            User user) {
        super(contentResolver, startUrl, docId, absPath);
        this.user = user;
    }

    public RoSafFtpFile(
            ContentResolver contentResolver,
            Uri startUrl,
            Cursor cursor,
            String absPath,
            User user) {
        super(contentResolver, startUrl, cursor, absPath);
        this.user = user;
    }

    @Override
    protected FtpFile createFile(
            ContentResolver contentResolver,
            Uri startUrl,
            Cursor cursor,
            String absPath) {
        return new RoSafFtpFile(contentResolver, startUrl, cursor, absPath, user);
    }

    @Override
    public boolean move(FtpFile target) {
        logger.trace("move()");
        return super.move((RoSafFile)target);
    }

    @Override
    public Object getPhysicalFile() {
        return this;
    }

    @Override
    public boolean isHidden() {
        logger.trace("[{}] isHidden()", name);
        return name.charAt(0) == '.';
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
    public int getLinkCount() {
        logger.trace("[{}] getLinkCount()", name);
        return 0;
    }

    public User getUser() {
        return user;
    }
}
