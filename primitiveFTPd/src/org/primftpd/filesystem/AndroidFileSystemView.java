package org.primftpd.filesystem;

import org.apache.ftpserver.ftplet.FtpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public abstract class AndroidFileSystemView<T extends AndroidFile<X>, X> {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final File homeDir;
	private T workingDir;

	public AndroidFileSystemView(File homeDir) {
		this.homeDir = homeDir;
		workingDir = createHomeDirObj();
	}

	protected abstract T createFile(File file);

	private T createHomeDirObj() {
		return createFile(homeDir);
	}

	public T getHomeDirectory() {
		logger.trace("getHomeDirectory()");
		return createHomeDirObj();
	}

	public T getWorkingDirectory() {
		logger.trace("getWorkingDirectory()");
		return workingDir;
	}

	public boolean changeWorkingDirectory(String dir) {
		logger.trace("changeWorkingDirectory({})", dir);

		File dirObj = new File(dir);

		// don't use isFile() as that does not give correct result
		if (!dirObj.isDirectory()) {
			logger.trace("not changing WD as new one is file");
			return false;
		}

		// some clients issue CWD commands
		// and are confused about home dir
		// do some checks to avoid issues
		// happened for keepass
		String currentAbsPath = workingDir.getAbsolutePath();
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

		String path = dir;
		if (!dirObj.isAbsolute()) {
			path = currentAbsPath + File.separator + dir;
		}

		logger.trace("current WD '{}', new path '{}'",
			currentAbsPath,
			path);
		workingDir = createFile(new File(path));

		return true;
	}

	public T getFile(String file) {
		logger.trace("getFile({})", file);

		File fileObj = new File(file);
		if (fileObj.isAbsolute()) {
			logger.trace("getFile(), returning abs: '{}'", file);
			return createFile(fileObj);
		}

		// handle relative paths
		file = workingDir.getAbsolutePath() + File.separator + file;

		logger.trace("getFile(), returning rel: '{}'", file);

		return createFile(new File(file));
	}

	public boolean isRandomAccessible() throws FtpException {
		logger.trace("isRandomAccessible()");
		return true;
	}

	public void dispose() {
		logger.trace("dispose()");
	}
}
