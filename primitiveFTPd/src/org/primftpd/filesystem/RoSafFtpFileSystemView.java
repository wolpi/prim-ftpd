package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.net.Uri;

import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;

public class RoSafFtpFileSystemView extends RoSafFileSystemView<RoSafFtpFile, FtpFile> implements FileSystemView {

    private final User user;
    private RoSafFtpFile workingDir;

    public RoSafFtpFileSystemView(Uri startUrl, ContentResolver contentResolver, User user) {
        super(startUrl, contentResolver);
        this.user = user;
        this.workingDir = getHomeDirectory();
    }

    @Override
    protected RoSafFtpFile createFile(ContentResolver contentResolver, Uri startUrl, String absPath) {
        return new RoSafFtpFile(contentResolver, startUrl, absPath, user);
    }

    @Override
    protected RoSafFtpFile createFile(ContentResolver contentResolver, Uri startUrl, String docId, String absPath) {
        return new RoSafFtpFile(contentResolver, startUrl, docId, absPath, true, user);
    }

    protected RoSafFtpFile createFileNonExistant(ContentResolver contentResolver, Uri startUrl, String name, String absPath) {
        return new RoSafFtpFile(contentResolver, startUrl, name, absPath, false, user);
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
