package org.primftpd.filesystem;

import android.content.ContentResolver;
import androidx.documentfile.provider.DocumentFile;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.primftpd.events.ClientActionPoster;

public class SafFtpFile extends SafFile<FtpFile> implements FtpFile {

    private final User user;

    public SafFtpFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            String absPath,
            ClientActionPoster clientActionPoster,
            User user) {
        super(contentResolver, parentDocumentFile, documentFile, absPath, clientActionPoster);
        this.user = user;
    }

    public SafFtpFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            String name,
            String absPath,
            ClientActionPoster clientActionPoster,
            User user) {
        super(contentResolver, parentDocumentFile, name, absPath, clientActionPoster);
        this.user = user;
    }

    @Override
    protected FtpFile createFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            String absPath,
            ClientActionPoster clientActionPoster) {
        return new SafFtpFile(contentResolver, parentDocumentFile, documentFile, absPath, clientActionPoster, user);
    }

    @Override
    public String getClientIp() {
        return FtpUtils.getClientIp(user);
    }

    @Override
    public boolean move(FtpFile target) {
        logger.trace("move()");
        return super.move((SafFile)target);
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
