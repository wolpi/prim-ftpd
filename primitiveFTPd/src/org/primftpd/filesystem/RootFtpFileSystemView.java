package org.primftpd.filesystem;

import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.User;
import org.primftpd.services.PftpdService;
import org.primftpd.pojo.LsOutputBean;

import java.io.File;

import eu.chainfire.libsuperuser.Shell;

public class RootFtpFileSystemView extends RootFileSystemView<RootFtpFile, FtpFile> implements FileSystemView {

    private final User user;
    private final RootFtpFile homeDir;
    private RootFtpFile workingDir;

    public RootFtpFileSystemView(Shell.Interactive shell, PftpdService pftpdService, File homeDir, User user) {
        super(shell, pftpdService);
        this.user = user;
        this.workingDir = this.homeDir = getFile(homeDir.getAbsolutePath());
    }

    @Override
    protected RootFtpFile createFile(LsOutputBean bean, String absPath, PftpdService pftpdService) {
        return new RootFtpFile(shell, bean, absPath, pftpdService, user);
    }

    @Override
    protected String absolute(String file) {
        logger.trace("  finding abs path for '{}' with wd '{}'", file, (workingDir != null ? workingDir.getAbsolutePath() : "null"));
        if (workingDir == null) {
            return file; // during c-tor
        }
        return Utils.absolute(file, workingDir.getAbsolutePath());
    }

    public RootFtpFile getHomeDirectory() {
        logger.trace("getHomeDirectory() -> {}", (homeDir != null ? homeDir.getAbsolutePath() : "null"));

        return homeDir;
    }

    public RootFtpFile getWorkingDirectory() {
        logger.trace("getWorkingDirectory() -> {}", (workingDir != null ? workingDir.getAbsolutePath() : "null"));

        return workingDir;
    }

    public boolean changeWorkingDirectory(String dir) {
        logger.trace("changeWorkingDirectory({})", dir);
        RootFtpFile newWorkingDir = getFile(dir);
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
