package org.primftpd.filesystem;

import java.io.File;

import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.User;

public class FtpFileSystemView
	extends AndroidFileSystemView<FtpFile, org.apache.ftpserver.ftplet.FtpFile>
	implements FileSystemView
{
	private final User user;

	public FtpFileSystemView(File homeDir, User user) {
		super(homeDir);
		this.user = user;
	}

	@Override
	protected FtpFile createFile(File file)
	{
		return new FtpFile(file, user);
	}
}
