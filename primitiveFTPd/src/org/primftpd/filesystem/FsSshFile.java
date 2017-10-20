package org.primftpd.filesystem;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.SshFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FsSshFile extends FsFile<SshFile> implements SshFile {
	private final Session session;

	public FsSshFile(File file, Session session) {
		super(file);
		this.session = session;
	}

	@Override
	protected SshFile createFile(File file) {
		return new FsSshFile(file, session);
	}

	@Override
	public boolean move(org.apache.sshd.common.file.SshFile target) {
		return super.move((FsFile)target);
	}

	@Override
	public String getOwner() {
		logger.trace("[{}] getOwner()", name);
		return session.getUsername();
	}

	@Override
	public boolean create() throws IOException {
		logger.trace("[{}] create()", name);
		return file.createNewFile();
	}

	@Override
	public SshFile getParentFile() {
		logger.trace("[{}] getParentFile()", name);
		return new FsSshFile(file.getParentFile(), session);
	}

	@Override
	public boolean isExecutable() {
		logger.trace("[{}] isExecutable()", name);
		return file.canExecute();
	}

	@Override
	public List<SshFile> listSshFiles() {
		return listFiles();
	}
}
