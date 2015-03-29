package org.primftpd.filesystem;

import java.io.File;

import org.apache.ftpserver.ftplet.FtpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Environment;

public abstract class AndroidFileSystemView<T extends AndroidFile<X>, X> {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private T workingDir;

	public AndroidFileSystemView() {
		workingDir = createHomeDirObj();
	}

	protected abstract T createFile(File file);

	private T createHomeDirObj() {
		File androidHomeDir = Environment.getExternalStorageDirectory();
		return createFile(androidHomeDir);
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

		if (dirObj.isFile()) {
			return false;
		}

		String path = dir;
		if (!dirObj.isAbsolute()) {
			path = workingDir.getAbsolutePath() + File.separator + dir;
		}

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
