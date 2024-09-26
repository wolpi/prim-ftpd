package org.primftpd.filesystem;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.SshFile;
import org.primftpd.services.PftpdService;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FsSshFile extends FsFile<SshFile> implements SshFile {
	private final Session session;
    private final FsSshFileSystemView fileSystemView;

	public FsSshFile(File file, PftpdService pftpdService, int timeResolution, Session session, FsSshFileSystemView fileSystemView) {
		super(file, pftpdService, timeResolution);
		this.session = session;
		this.fileSystemView = fileSystemView;
	}

	@Override
	public String getClientIp() {
		return SshUtils.getClientIp(session);
	}

	@Override
	protected SshFile createFile(File file, PftpdService pftpdService) {
		return new FsSshFile(file, pftpdService, timeResolution, session, fileSystemView);
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
        String parentPath = file.getParent();
        if (parentPath == null || parentPath.length() == 0) {
            parentPath = File.separator;
        }
        logger.trace("[{}]   getParentFile() -> {}", name, parentPath);
        return fileSystemView.getFile(parentPath);
	}

	@Override
	public boolean isExecutable() {
		logger.trace("[{}] isExecutable()", name);
		return injectedDirectory || file.canExecute();
	}

	@Override
	public List<SshFile> listSshFiles() {
		return listFiles();
	}
}
