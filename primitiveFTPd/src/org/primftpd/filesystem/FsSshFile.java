package org.primftpd.filesystem;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.SshFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	public String readSymbolicLink() throws IOException {
		logger.trace("[{}] readSymbolicLink()", name);
		logger.trace("sym link abs path: {}", file.getAbsolutePath());
		logger.trace("sym link can path: {}", file.getCanonicalPath());
		return file.getCanonicalPath();
	}

	@Override
	public void createSymbolicLink(org.apache.sshd.common.file.SshFile arg0)
			throws IOException
	{
		// TODO ssh createSymbolicLink
		logger.trace("[{}] createSymbolicLink()", name);
	}

	@Override
	public String getOwner() {
		logger.trace("[{}] getOwner()", name);
		return session.getUsername();
	}

	@Override
	public Object getAttribute(Attribute attribute, boolean followLinks)
		throws IOException
	{
		logger.trace("[{}] getAttribute({})", name, attribute);
		return SshUtils.getAttribute(this, attribute, followLinks);
	}

	@Override
	public Map<Attribute, Object> getAttributes(boolean followLinks)
		throws IOException
	{
		logger.trace("[{}] getAttributes()", name);

		Map<SshFile.Attribute, Object> attributes = new HashMap<>();
		for (SshFile.Attribute attr : SshFile.Attribute.values()) {
			attributes.put(attr, getAttribute(attr, followLinks));
		}

		return attributes;
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
	public void handleClose() throws IOException {
		// TODO ssh handleClose
		logger.trace("[{}] handleClose()", name);
	}

	@Override
	public List<SshFile> listSshFiles() {
		return listFiles();
	}

	@Override
	public void setAttribute(Attribute attribute, Object value) throws IOException {
		// TODO ssh setAttribute
		logger.trace("[{}] setAttribute()", name);
	}

	@Override
	public void setAttributes(Map<Attribute, Object> attributes) throws IOException {
		// TODO ssh setAttributes
		logger.trace("[{}] setAttributes()", name);
	}

	@Override
	public void truncate() throws IOException {
		// TODO ssh truncate
		logger.trace("[{}] truncate()", name);
	}
}
