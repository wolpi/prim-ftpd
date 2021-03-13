package org.primftpd.filesystem;

import android.content.ContentResolver;
import androidx.documentfile.provider.DocumentFile;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.primftpd.services.PftpdService;

public class SafFtpFile extends SafFile<FtpFile> implements FtpFile {

    private final User user;

    public SafFtpFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            String absPath,
            PftpdService pftpdService,
            User user) {
        super(contentResolver, parentDocumentFile, documentFile, absPath, pftpdService);
        this.user = user;
    }

    public SafFtpFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            String name,
            String absPath,
            PftpdService pftpdService,
            User user) {
        super(contentResolver, parentDocumentFile, name, absPath, pftpdService);
        this.user = user;
    }

    @Override
    protected FtpFile createFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            String absPath,
            PftpdService pftpdService) {
        return new SafFtpFile(contentResolver, parentDocumentFile, documentFile, absPath, pftpdService, user);
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
