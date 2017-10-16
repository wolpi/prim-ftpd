package org.primftpd.filesystem;

import java.io.File;

import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;

public class FsFtpFileSystemView
	extends FsFileSystemView<FsFtpFile, org.apache.ftpserver.ftplet.FtpFile>
	implements FileSystemView
{
	private final File homeDir;
	private FsFtpFile workingDir;

	private final User user;

	public FsFtpFileSystemView(File homeDir, User user) {
		this.homeDir = homeDir;
		workingDir = createHomeDirObj();
		this.user = user;
	}

	@Override
	protected FsFtpFile createFile(File file)
	{
		return new FsFtpFile(file, user);
	}

	@Override
	protected String absolute(String file) {
		if ('/' == file.charAt(0)) {
			return file;
		}

		return workingDir.getAbsolutePath() + File.separator + file;
	}

	private FsFtpFile createHomeDirObj() {
		return createFile(homeDir);
	}

	public FsFtpFile getHomeDirectory() {
		logger.trace("getHomeDirectory()");
		return createHomeDirObj();
	}

	public FsFtpFile getWorkingDirectory() {
		logger.trace("getWorkingDirectory()");
		return workingDir;
	}

	public boolean changeWorkingDirectory(String dir) {
		logger.trace("changeWorkingDirectory({})", dir);

		File dirObj = new File(dir);
		String currentAbsPath = workingDir.getAbsolutePath();
		String path = dir;
		if (!dirObj.isAbsolute()) {
			path = currentAbsPath + File.separator + dir;
		}
		logger.trace("using path for cwd operation: {}", path);
		dirObj = new File(path);

		// check if new path is a dir
		if (!dirObj.isDirectory()) {
			logger.trace("not changing WD as new one is not a directory");
			return false;
		}

		// some clients issue CWD commands
		// and are confused about home dir
		// do some checks to avoid issues
		// happened for keepass
		String paraAbs = dirObj.getAbsolutePath();
		if (currentAbsPath.length() * 2 == paraAbs.length()) {
			String pathDoubled = currentAbsPath + currentAbsPath;
			if (pathDoubled.equals(paraAbs)) {
				// this is the confusion case
				// just tell client everything is alright
				logger.trace(
						"client is confused about WD ({}), just tell him it is alright",
						currentAbsPath);
				return true;
			}
		}

		logger.trace("current WD '{}', new path '{}'",
				currentAbsPath,
				path);
		workingDir = createFile(new File(path));

		return true;
	}

	public boolean isRandomAccessible() throws FtpException {
		logger.trace("isRandomAccessible()");
		return true;
	}

	public void dispose() {
		logger.trace("dispose()");
	}
}
