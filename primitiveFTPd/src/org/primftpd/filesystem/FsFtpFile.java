package org.primftpd.filesystem;

import java.io.File;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.primftpd.services.PftpdService;

public class FsFtpFile extends FsFile<FtpFile> implements FtpFile {
	private final User user;

	public FsFtpFile(File file, PftpdService pftpdService, User user) {
		super(file, pftpdService);
		this.user = user;
	}

	@Override
	public String getClientIp() {
		return FtpUtils.getClientIp(user);
	}

	@Override
	protected FtpFile createFile(File file, PftpdService pftpdService) {
		return new FsFtpFile(file, pftpdService, user);
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
		logger.trace("[{}] getPhysicalFile()", name);
		return file;
	}

	@Override
	public boolean isHidden() {
		//boolean result = file.isHidden();
		//logger.trace("[{}] isHidden() -> {}", name, result);
		//return result;
		return super.isHidden();
	}
}
