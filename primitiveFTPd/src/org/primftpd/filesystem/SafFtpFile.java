package org.primftpd.filesystem;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;

import java.util.List;

import androidx.documentfile.provider.DocumentFile;

public class SafFtpFile extends SafFile<FtpFile, SafFtpFileSystemView> implements FtpFile {

    private final User user;

    public SafFtpFile(
            SafFtpFileSystemView fileSystemView,
            String absPath,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            User user) {
        super(fileSystemView, absPath, parentDocumentFile, documentFile);
        this.user = user;
    }

    public SafFtpFile(
            SafFtpFileSystemView fileSystemView,
            String absPath,
            DocumentFile parentDocumentFile,
            List<String> parentNonexistentDirs,
            String name,
            User user) {
        super(fileSystemView, absPath, parentDocumentFile, parentNonexistentDirs, name);
        this.user = user;
    }

    @Override
    protected FtpFile createFile(
            String absPath,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile) {
        return new SafFtpFile(getFileSystemView(), absPath, parentDocumentFile, documentFile, user);
    }

    @Override
    public String getClientIp() {
        return FtpUtils.getClientIp(user);
    }

    @Override
    public boolean move(FtpFile target) {
        return super.move((SafFtpFile)target);
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
