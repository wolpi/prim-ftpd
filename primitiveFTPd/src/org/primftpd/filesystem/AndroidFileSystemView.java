package org.primftpd.filesystem;

import java.io.File;

import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AndroidFileSystemView implements FileSystemView {

	private static final Logger logger = LoggerFactory.getLogger(AndroidFileSystemView.class);

	private final User user;

	private AndroidFtpFile workingDir;

	public AndroidFileSystemView(User user) {
		this.user = user;
		workingDir = createHomeDirObj(user);
	}

	private AndroidFtpFile createHomeDirObj(User user) {
		return new AndroidFtpFile(new File(user.getHomeDirectory()), user);
	}

	@Override
	public FtpFile getHomeDirectory() throws FtpException {
		logger.debug("getHomeDirectory()");
		return createHomeDirObj(user);
	}

	@Override
	public FtpFile getWorkingDirectory() throws FtpException {
		logger.debug("getWorkingDirectory()");

		return workingDir;
	}

	@Override
	public boolean changeWorkingDirectory(String dir) throws FtpException {
		logger.debug("changeWorkingDirectory({})", dir);

		File dirObj = new File(dir);

		if (dirObj.isFile()) {
			return false;
		}

		String path = dir;
		if (!dirObj.isAbsolute()) {
			path = workingDir.getAbsolutePath() + File.separator + dir;
		}

		workingDir = new AndroidFtpFile(new File(path), user);

		return true;
	}

	@Override
	public FtpFile getFile(String file) throws FtpException {
		logger.debug("getFile({})", file);

		File fileObj = new File(file);
		if (fileObj.isAbsolute()) {
			logger.debug("getFile(), returning abs: '{}'", file);
			return new AndroidFtpFile(fileObj, user);
		}

		// handle relative paths
		file = workingDir.getAbsolutePath() + File.separator + file;

		logger.debug("getFile(), returning rel: '{}'", file);

		return new AndroidFtpFile(new File(file), user);
	}

	@Override
	public boolean isRandomAccessible() throws FtpException {
		logger.debug("isRandomAccessible()");
		return true;
	}

	@Override
	public void dispose() {
		logger.debug("dispose()");
	}

}
