package org.primftpd.filesystem;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.SshFile;
import org.primftpd.services.PftpdService;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FsSshFile extends FsFile<SshFile> implements SshFile {
	private final Session session;

	public FsSshFile(File file, PftpdService pftpdService, Session session) {
		super(file, pftpdService);
		this.session = session;
	}

	@Override
	public String getClientIp() {
		return SshUtils.getClientIp(session);
	}

	@Override
	protected SshFile createFile(File file, PftpdService pftpdService) {
		return new FsSshFile(file, pftpdService, session);
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
		return new FsSshFile(file.getParentFile(), pftpdService, session);
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
