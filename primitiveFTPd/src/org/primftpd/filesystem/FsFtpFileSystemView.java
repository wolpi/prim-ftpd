package org.primftpd.filesystem;

import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.primftpd.services.PftpdService;

import java.io.File;

public class FsFtpFileSystemView extends FsFileSystemView<FsFtpFile, FtpFile> implements FileSystemView {

	private final File homeDir;
	private final User user;

	private FsFtpFile workingDir;

	public FsFtpFileSystemView(PftpdService pftpdService, File homeDir, User user) {
		super(pftpdService);
		this.homeDir = homeDir;
		this.user = user;

		workingDir = getHomeDirectory();
	}

	@Override
	protected FsFtpFile createFile(File file) {
		return new FsFtpFile(this, file, user);
	}

	@Override
	protected String absolute(String file) {
		logger.trace("  finding abs path for '{}' with wd '{}'", file, (workingDir != null ? workingDir.getAbsolutePath() : "null"));
		return Utils.absolute(file, workingDir.getAbsolutePath());
	}

	public FsFtpFile getHomeDirectory() {
		logger.trace("getHomeDirectory() -> {}", (homeDir != null ? homeDir.getAbsolutePath() : "null"));

		return createFile(homeDir);
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
		workingDir = createFile(new File(path));

		return true;
	}

	public boolean isRandomAccessible() {
		logger.trace("isRandomAccessible()");

		return true;
	}

	public void dispose() {
		logger.trace("dispose()");
	}
}
