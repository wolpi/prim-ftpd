package org.primftpd.filesystem;

import java.io.File;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;

public class FsFtpFile extends FsFile<FtpFile, FsFtpFileSystemView> implements FtpFile {
	private final User user;

	public FsFtpFile(FsFtpFileSystemView fileSystemView, File file, User user) {
		super(fileSystemView, file);
		this.user = user;
	}

	@Override
	public String getClientIp() {
		return FtpUtils.getClientIp(user);
	}

	@Override
	protected FtpFile createFile(File file) {
		return new FsFtpFile(getFileSystemView(), file, user);
	}

	@Override
	public boolean move(FtpFile target) {
		return super.move((FsFtpFile) target);
	}

	@Override
	public String getOwnerName() {
		logger.trace("[{}] getOwnerName()", name);
		return user.getName();
	}

	@Override
	public String getGroupName() {
		logger.trace("[{}] getGroupName()", name);
		return user.getName();
	}

	@Override
	public Object getPhysicalFile() {
		logger.trace("[{}] getPhysicalFile()", name);
		return file;
	}

	@Override
	public boolean isHidden() {
		boolean result = file.isHidden();
		logger.trace("[{}] isHidden() -> {}", name, result);
		return result;
	}
}
