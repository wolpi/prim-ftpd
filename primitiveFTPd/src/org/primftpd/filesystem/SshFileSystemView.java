package org.primftpd.filesystem;

import java.io.File;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemView;

public class SshFileSystemView
	extends AndroidFileSystemView<SshFile, org.apache.sshd.common.file.SshFile>
	implements FileSystemView
{
	private final Session session;

	public SshFileSystemView(Session session) {
		this.session = session;
	}

	@Override
	protected SshFile createFile(File file)
	{
		return new SshFile(file, session);
	}

	@Override
	public org.apache.sshd.common.file.SshFile getFile(
			org.apache.sshd.common.file.SshFile arg0,
			String arg1)
	{
		return getFile(arg0.getAbsolutePath());
	}

	@Override
	public FileSystemView getNormalizedView()
	{
		// TODO ssh getNormalizedView
		return null;
	}
}
