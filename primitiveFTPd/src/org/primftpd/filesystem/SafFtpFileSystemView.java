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

import java.util.List;

public class SafFtpFileSystemView extends SafFileSystemView<SafFtpFile, FtpFile> implements FileSystemView {

    private final User user;

    private SafFtpFile workingDir;

    public SafFtpFileSystemView(PftpdService pftpdService, Uri startUrl, User user) {
        super(pftpdService, startUrl);
        this.user = user;

        this.workingDir = getHomeDirectory();
    }

    @Override
    protected SafFtpFile createFile(
            String absPath,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile) {
        logger.trace("createFile(DocumentFile)");
        return new SafFtpFile(this, absPath, parentDocumentFile, documentFile, user);
    }

    @Override
    protected SafFtpFile createFile(
            String absPath,
            DocumentFile parentDocumentFile,
            List<String> parentNonexistentDirs,
            String name) {
        logger.trace("createFile(String)");
        return new SafFtpFile(this, absPath, parentDocumentFile, parentNonexistentDirs, name, user);
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
