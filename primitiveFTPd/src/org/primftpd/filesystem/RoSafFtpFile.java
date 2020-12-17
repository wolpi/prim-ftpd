package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.primftpd.events.ClientActionPoster;

public class RoSafFtpFile extends RoSafFile<FtpFile> implements FtpFile {

    private final User user;

    public RoSafFtpFile(
            ContentResolver contentResolver,
            Uri startUrl,
            String absPath,
            ClientActionPoster clientActionPoster,
            User user) {
        super(contentResolver, startUrl, absPath, clientActionPoster);
        this.user = user;
    }

    public RoSafFtpFile(
            ContentResolver contentResolver,
            Uri startUrl,
            String docId,
            String absPath,
            boolean exists,
            ClientActionPoster clientActionPoster,
            User user) {
        super(contentResolver, startUrl, docId, absPath, exists, clientActionPoster);
        this.user = user;
    }

    public RoSafFtpFile(
            ContentResolver contentResolver,
            Uri startUrl,
            Cursor cursor,
            String absPath,
            ClientActionPoster clientActionPoster,
            User user) {
        super(contentResolver, startUrl, cursor, absPath, clientActionPoster);
        this.user = user;
    }

    @Override
    protected FtpFile createFile(
            ContentResolver contentResolver,
            Uri startUrl,
            Cursor cursor,
            String absPath,
            ClientActionPoster clientActionPoster) {
        return new RoSafFtpFile(contentResolver, startUrl, cursor, absPath, clientActionPoster, user);
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
