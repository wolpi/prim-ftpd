package org.primftpd.filesystem;

import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.primftpd.services.PftpdService;

import java.io.File;

public class VirtualFtpFileSystemView extends VirtualFileSystemView<
        FsFtpFile,
        RootFtpFile,
        SafFtpFile,
        RoSafFtpFile,
        FtpFile> implements FileSystemView {

    private final File homeDir;
    private final User user;

    private FtpFile workingDir;

    public VirtualFtpFileSystemView(
            PftpdService pftpdService,
            FsFtpFileSystemView fsFileSystemView,
            RootFtpFileSystemView rootFileSystemView,
            SafFtpFileSystemView safFileSystemView,
            RoSafFtpFileSystemView roSafFileSystemView,
            File homeDir,
            User user) {
        super(pftpdService, fsFileSystemView, rootFileSystemView, safFileSystemView, roSafFileSystemView);
        this.homeDir = homeDir;
        this.user = user;

        workingDir = getHomeDirectory();
    }

    @Override
    public FtpFile createFile(String absPath, AbstractFile delegate) {
        return new VirtualFtpFile(this, absPath, delegate, user);
    }

    @Override
    public FtpFile createFile(String absPath, boolean exists) {
        return new VirtualFtpFile(this, absPath, exists, user);
    }

    @Override
    public AbstractFile getConfigFile() {
        return new VirtualFtpConfigFile(this, user);
    }

    @Override
    protected String absolute(String file) {
        logger.trace("  finding abs path for '{}' with wd '{}'", file, (workingDir != null ? workingDir.getAbsolutePath() : "null"));
        if (workingDir == null) {
            return file; // during c-tor
        }
        return Utils.absolute(file, workingDir.getAbsolutePath());
    }

    @Override
    public FtpFile getHomeDirectory() {
        logger.trace("getHomeDirectory() -> {}", (homeDir != null ? homeDir.getAbsolutePath() : "null"));
        return getFile("/" + PREFIX_FS + homeDir.getAbsolutePath());
    }

    @Override
    public FtpFile getWorkingDirectory() {
        logger.trace("getWorkingDirectory() -> {}", (workingDir != null ? workingDir.getAbsolutePath() : "null"));
        return workingDir;
    }

    @Override
    public boolean changeWorkingDirectory(String dir) {
        logger.trace("changeWorkingDirectory({})", dir);
        String newPath;
        FtpFile newWorkingDir;
        boolean isAbsolute = dir != null && dir.charAt(0) == '/';
        if (!isAbsolute) {
            // curl ignores current WD and tries to switch to WD from root dir by dir
            String topLevelPath = "/" + dir;
            FtpFile topLevelDir = getFile(topLevelPath);
            if (topLevelDir.doesExist()) {
                newPath = topLevelDir.getAbsolutePath();
            } else {
                newPath = workingDir.getAbsolutePath() + File.separator + dir;
            }
            logger.trace("  using path for cwd operation: {}", newPath);
        } else {
            newPath = dir;
        }

        newWorkingDir = getFile(newPath);
        if (newWorkingDir.doesExist() && newWorkingDir.isDirectory()) {
            workingDir = newWorkingDir;
            return true;
        }
        return false;
    }

    public boolean isRandomAccessible() {
        logger.trace("isRandomAccessible()");
        return true;
    }

    public void dispose() {
        logger.trace("dispose()");
    }
}
