package org.primftpd.filesystem;

import android.content.Context;
import android.net.Uri;

import java.io.File;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.primftpd.services.PftpdService;

public class FsFtpFileSystemView extends FsFileSystemView<FsFtpFile, FtpFile> implements FileSystemView {

	private final User user;
	private final File homeDir;
	private FsFtpFile workingDir;

	public FsFtpFileSystemView(Context context, Uri safStartUrl, PftpdService pftpdService, File homeDir, User user) {
		super(context, safStartUrl, pftpdService);
		this.homeDir = homeDir;
		workingDir = getHomeDirectory();
		this.user = user;
	}

	@Override
	protected FsFtpFile createFile(File file, PftpdService pftpdService) {
		return new FsFtpFile(file, pftpdService, getTimeResolution(file.getAbsolutePath()), this, user);
	}

	@Override
	protected String absolute(String file) {
		logger.trace("  finding abs path for '{}' with wd '{}'", file, (workingDir != null ? workingDir.getAbsolutePath() : "null"));
		return Utils.absolute(file, workingDir.getAbsolutePath());
	}

	public FsFtpFile getHomeDirectory() {
		logger.trace("getHomeDirectory() -> {}", (homeDir != null ? homeDir.getAbsolutePath() : "null"));

		return createFile(homeDir, pftpdService);
	}

	public FsFtpFile getWorkingDirectory() {
		logger.trace("getWorkingDirectory() -> {}", (workingDir != null ? workingDir.getAbsolutePath() : "null"));

		return workingDir;
	}

	public boolean changeWorkingDirectory(String dir) {
		logger.trace("changeWorkingDirectory({})", dir);

		File dirObj = new File(dir);
		String path = dir;
		String currentAbsPath = workingDir.getAbsolutePath();
		if (!dirObj.isAbsolute()) {
			if ("..".equals(path)) {
				path = new File(currentAbsPath).getParent();
				if (path == null) {
					path = File.separator;
				}
			} else if (Utils.RUN_TESTS) {
				// curl ignores current WD and tries to switch to WD from root dir by dir
				File topLevelDir = new File(File.separator + dir);
				if (topLevelDir.exists()) {
					path = topLevelDir.getAbsolutePath();
				} else {
					path = currentAbsPath + File.separator + dir;
				}
			} else {
				path = currentAbsPath + File.separator + dir;
			}
		}
		dirObj = new File(path);
		logger.trace("using path for cwd operation: {}", path);

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
		workingDir = createFile(new File(path), pftpdService);

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
