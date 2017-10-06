package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.support.v4.provider.DocumentFile;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;

public class SafFtpFile extends SafFile<FtpFile> implements FtpFile {

    private final User user;

    public SafFtpFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            String absPath,
            User user) {
        super(contentResolver, parentDocumentFile, documentFile, absPath);
        this.user = user;
    }

    public SafFtpFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            String name,
            String absPath,
            User user) {
        super(contentResolver, parentDocumentFile, name, absPath);
        this.user = user;
    }

    @Override
    protected FtpFile createFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            String absPath) {
        return new SafFtpFile(contentResolver, parentDocumentFile, documentFile, absPath, user);
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
