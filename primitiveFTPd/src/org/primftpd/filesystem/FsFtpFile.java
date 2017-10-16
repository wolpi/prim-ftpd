package org.primftpd.filesystem;

import java.io.File;

import org.apache.ftpserver.ftplet.User;

public class FsFtpFile extends FsFile<org.apache.ftpserver.ftplet.FtpFile>
	implements org.apache.ftpserver.ftplet.FtpFile
{
	private final User user;

	public FsFtpFile(File file, User user)
	{
		super(file);
		this.user = user;
	}

	@Override
	protected org.apache.ftpserver.ftplet.FtpFile createFile(File file)
	{
		return new FsFtpFile(file, user);
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
	public boolean move(org.apache.ftpserver.ftplet.FtpFile target)
	{
		return super.move((FsFile) target);
	}

	public int getLinkCount() {
		logger.trace("[{}] getLinkCount()", name);
		return 0;
	}

	public boolean isHidden() {
		logger.trace("[{}] isHidden()", name);
		return file.isHidden();
	}

	public User getUser() {
		return user;
	}

	@Override
	public Object getPhysicalFile() {
		return file;
	}
}
