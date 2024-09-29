package org.primftpd.filesystem;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.SshFile;
import org.primftpd.services.PftpdService;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FsSshFile extends FsFile<SshFile, FsSshFileSystemView> implements SshFile {
	private final Session session;

	public FsSshFile(FsSshFileSystemView fileSystemView, File file, Session session) {
		super(fileSystemView, file);
		this.session = session;
	}

	@Override
	public String getClientIp() {
		return SshUtils.getClientIp(session);
	}

	@Override
	protected SshFile createFile(File file) {
		return new FsSshFile(getFileSystemView(), file, session);
	}

	@Override
	public boolean move(SshFile target) {
		return super.move((AbstractFile)target);
	}

	@Override
	public String getOwner() {
		logger.trace("[{}] getOwner()", name);
		return session.getUsername();
	}

	@Override
	public boolean create() throws IOException {
        // This call is required by SSHFS, because it calls STAT on created new files.
        // This call is not required by normal clients who simply open, write and close the file.
		boolean result = file.createNewFile();
		logger.trace("[{}] create() -> {}", name, result);
		return result;
	}

	@Override
	public SshFile getParentFile() {
		logger.trace("[{}] getParentFile()", name);
		return new FsSshFile(getFileSystemView(), file.getParentFile(), session);
	}

	@Override
	public boolean isExecutable() {
		boolean result = isInjectedDirectory || file.canExecute();
		logger.trace("[{}] isExecutable() -> {}", name, result);
		return result;
	}

	@Override
	public List<SshFile> listSshFiles() {
		return listFiles();
	}
}
