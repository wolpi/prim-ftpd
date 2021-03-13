package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;

import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.primftpd.services.PftpdService;

public class SafFtpFileSystemView extends SafFileSystemView<SafFtpFile, FtpFile> implements FileSystemView {

    private final User user;
    private SafFtpFile workingDir;

    public SafFtpFileSystemView(Context context, Uri startUrl, ContentResolver contentResolver, PftpdService pftpdService, User user) {
        super(context, startUrl, contentResolver, pftpdService);
        this.user = user;
        this.workingDir = getHomeDirectory();
    }

    @Override
    protected SafFtpFile createFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            String absPath,
            PftpdService pftpdService) {
        logger.trace("createFile(DocumentFile)");
        return new SafFtpFile(contentResolver, parentDocumentFile, documentFile, absPath, pftpdService, user);
    }

    @Override
    protected SafFtpFile createFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            String name,
            String absPath,
            PftpdService pftpdService) {
        logger.trace("createFile(String)");
        return new SafFtpFile(contentResolver, parentDocumentFile, name, absPath, pftpdService, user);
    }

    @Override
    protected String absolute(String file) {
        logger.trace("  finding abs path for '{}' with wd '{}'", file, (workingDir != null ? workingDir.getAbsolutePath() : "null"));
        if (workingDir == null) {
            return file; // during c-tor
        }
        return Utils.absolute(file, workingDir.getAbsolutePath());
    }

    public SafFtpFile getHomeDirectory() {
        logger.trace("getHomeDirectory() -> {}", SafFtpFileSystemView.ROOT_PATH);

        return getFile(SafFtpFileSystemView.ROOT_PATH);
    }

    public SafFtpFile getWorkingDirectory() {
        logger.trace("getWorkingDirectory() -> {}", (workingDir != null ? workingDir.getAbsolutePath() : "null"));

        return workingDir;
    }

    public boolean changeWorkingDirectory(String dir) {
        logger.trace("changeWorkingDirectory({})", dir);
        SafFtpFile newWorkingDir = getFile(dir);
        if (newWorkingDir.doesExist() && newWorkingDir.isDirectory()) {
            workingDir = newWorkingDir;
            return true;
        }
        return false;
    }

    public boolean isRandomAccessible() throws FtpException {
        logger.trace("isRandomAccessible()");

        return true;
    }

    public void dispose() {
        logger.trace("dispose()");
    }
}
