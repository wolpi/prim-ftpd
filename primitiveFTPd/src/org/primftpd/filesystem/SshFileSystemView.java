package org.primftpd.filesystem;

import java.io.File;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemView;

public class SshFileSystemView
	extends AndroidFileSystemView<SshFile, org.apache.sshd.common.file.SshFile>
	implements FileSystemView
{
	private final File homeDir;
	private final Session session;

	public SshFileSystemView(File homeDir, Session session) {
		this.homeDir = homeDir;
		this.session = session;
	}

	@Override
	protected SshFile createFile(File file)
	{
		return new SshFile(file, session);
	}

	@Override
	protected String absolute(String file) {
		if (".".equals(file)) {
			return homeDir.getAbsolutePath();
		}
		// is abs always
		return file;
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
		return this;
	}
}
