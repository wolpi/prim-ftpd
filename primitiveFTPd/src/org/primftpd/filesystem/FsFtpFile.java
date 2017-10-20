package org.primftpd.filesystem;

import java.io.File;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;

public class FsFtpFile extends FsFile<FtpFile> implements FtpFile {
	private final User user;

	public FsFtpFile(File file, User user) {
		super(file);
		this.user = user;
	}

	@Override
	protected FtpFile createFile(File file) {
		return new FsFtpFile(file, user);
	}

	@Override
	public boolean move(FtpFile target) {
		return super.move((FsFile) target);
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
		return file;
	}

	@Override
	public boolean isHidden() {
		logger.trace("[{}] isHidden()", name);
		return file.isHidden();
	}
}
