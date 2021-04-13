package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.net.Uri;

import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.primftpd.services.PftpdService;

public class RoSafFtpFileSystemView extends RoSafFileSystemView<RoSafFtpFile, FtpFile> implements FileSystemView {

    private final User user;
    private RoSafFtpFile workingDir;

    public RoSafFtpFileSystemView(Uri startUrl, ContentResolver contentResolver, PftpdService pftpdService, User user) {
        super(startUrl, contentResolver, pftpdService);
        this.user = user;
        this.workingDir = getHomeDirectory();
    }

    @Override
    protected RoSafFtpFile createFile(ContentResolver contentResolver, Uri startUrl, String absPath, PftpdService pftpdService) {
        return new RoSafFtpFile(contentResolver, startUrl, absPath, pftpdService, user);
    }

    @Override
    protected RoSafFtpFile createFile(ContentResolver contentResolver, Uri startUrl, String docId, String absPath, PftpdService pftpdService) {
        return new RoSafFtpFile(contentResolver, startUrl, docId, absPath, true, pftpdService, user);
    }

    protected RoSafFtpFile createFileNonExistant(ContentResolver contentResolver, Uri startUrl, String name, String absPath, PftpdService pftpdService) {
        return new RoSafFtpFile(contentResolver, startUrl, name, absPath, false, pftpdService, user);
    }

    @Override
    protected String absolute(String file) {
        logger.trace("  finding abs path for '{}' with wd '{}'", file, (workingDir != null ? workingDir.getAbsolutePath() : "null"));
        if (workingDir == null) {
            return file; // during c-tor
        }
        return Utils.absolute(file, workingDir.getAbsolutePath());
    }

    public RoSafFtpFile getHomeDirectory() {
        logger.trace("getHomeDirectory() -> {}", ROOT_PATH);

        return getFile(ROOT_PATH);
    }

    public RoSafFtpFile getWorkingDirectory() {
        logger.trace("getWorkingDirectory() -> {}", (workingDir != null ? workingDir.getAbsolutePath() : "null"));

        return workingDir;
    }

    public boolean changeWorkingDirectory(String dir) {
        logger.trace("changeWorkingDirectory({})", dir);
        RoSafFtpFile newWorkingDir = getFile(dir);
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
