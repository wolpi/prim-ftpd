package org.primftpd.filesystem;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sshd.common.Session;

public class SshFile extends AndroidFile<org.apache.sshd.common.file.SshFile>
	implements org.apache.sshd.common.file.SshFile
{
	private final Session session;

	private Boolean symLinkCache;

	public SshFile(File file, Session session)
	{
		super(file);
		this.session = session;
	}

	@Override
	protected org.apache.sshd.common.file.SshFile createFile(File file)
	{
		return new SshFile(file, session);
	}

	@Override
	public boolean create() throws IOException
	{
		logger.trace("create()");
		return file.createNewFile();
	}

	@Override
	public void createSymbolicLink(org.apache.sshd.common.file.SshFile arg0)
			throws IOException
	{
		// TODO ssh createSymbolicLink
		logger.trace("createSymbolicLink()");
	}

	@Override
	public Object getAttribute(Attribute attribute, boolean followLinks)
		throws IOException
	{
		logger.trace("getAttribute({}, {})", file, attribute);
		switch (attribute) {
		case Size:
			return Long.valueOf(getSize());
		case Uid:
			// TODO ssh uid
			return Integer.valueOf(1);
		case Owner:
			return getOwner();
		case Gid:
			// TODO ssh gid
			return Integer.valueOf(1);
		case Group:
			return getOwner();
		case IsDirectory:
			return Boolean.valueOf(isDirectory());
		case IsRegularFile:
			return Boolean.valueOf(isFile());
		case IsSymbolicLink:
			return isSymLink();
		case Permissions:
			boolean read = isReadable();
			boolean write = isWritable();
			boolean exec = isExecutable();
			Set<Permission> tmp = new HashSet<Permission>();
			if (read) {
				tmp.add(Permission.UserRead);
				tmp.add(Permission.GroupRead);
				tmp.add(Permission.OthersRead);
			}
			if (write) {
				tmp.add(Permission.UserWrite);
				tmp.add(Permission.GroupWrite);
				tmp.add(Permission.OthersWrite);
			}
			if (exec) {
				tmp.add(Permission.UserExecute);
				tmp.add(Permission.GroupExecute);
				tmp.add(Permission.OthersExecute);
			}
			return tmp.isEmpty()
				? EnumSet.noneOf(Permission.class)
				: EnumSet.copyOf(tmp);
		case CreationTime:
			// TODO ssh creation time
			return Long.valueOf(getLastModified());
		case LastModifiedTime:
			return Long.valueOf(getLastModified());
		case LastAccessTime:
			// TODO ssh access time
			return Long.valueOf(getLastModified());
		default:
			return null;
		}
	}

	protected boolean isSymLink() throws IOException {
		if (symLinkCache == null) {
			File fileToUseForCheck = this.file;
			if (file.isDirectory()) {
				File[] children = file.listFiles();
				if (children != null && children.length > 0) {
					File firstChild = children[0];
					fileToUseForCheck = firstChild;
				}
			}
			String absPath = fileToUseForCheck.getAbsolutePath();
			String canonPath = fileToUseForCheck.getCanonicalPath();
			Boolean isSymLink = Boolean.valueOf(!absPath.equals(canonPath));
			symLinkCache = isSymLink;
			logger.trace("  sym link {}, canon path used: {}", isSymLink, canonPath);
		}
		return symLinkCache.booleanValue();
	}

	@Override
	public boolean doesExist() {
		boolean superExists = super.doesExist();
		boolean isSymlink = false;
		try {
			isSymlink = isSymLink();
		} catch (IOException e) {
			logger.error("cannot figure out if file is sym link", e);
		}
		if (!superExists && isSymlink) {
			logger.trace("  doesExist() sym link check -> seems to be sym link, returning true");
			return true;
		}
		return superExists;
	}

	@Override
	public Map<Attribute, Object> getAttributes(boolean followLinks)
		throws IOException
	{
		logger.trace("getAttributes()");

		Map<SshFile.Attribute, Object> attributes =
			new HashMap<SshFile.Attribute, Object>();
		for (SshFile.Attribute attr : SshFile.Attribute.values()) {
			attributes.put(attr, getAttribute(attr, followLinks));
		}

		return attributes;
	}

	@Override
	public String getOwner()
	{
		logger.trace("getOwner()");
		return session.getUsername();
	}

	@Override
	public org.apache.sshd.common.file.SshFile getParentFile()
	{
		logger.trace("getParentFile()");
		return new SshFile(file.getParentFile(), session);
	}

	@Override
	public void handleClose() throws IOException
	{
		// TODO ssh handleClose
		logger.trace("handleClose()");
	}

	@Override
	public boolean isExecutable()
	{
		logger.trace("isExecutable()");
		return file.canExecute();
	}

	@Override
	public List<org.apache.sshd.common.file.SshFile> listSshFiles()
	{
		return listFiles();
	}

	@Override
	public boolean move(org.apache.sshd.common.file.SshFile target)
	{
		return super.move((AndroidFile)target);
	}

	@Override
	public String readSymbolicLink() throws IOException
	{
		logger.trace("readSymbolicLink()");
		logger.trace("sym link abs path: {}", file.getAbsolutePath());
		logger.trace("sym link can path: {}", file.getCanonicalPath());
		return file.getCanonicalPath();
	}

	@Override
	public void setAttribute(Attribute attribute, Object value) throws IOException
	{
		// TODO ssh setAttribute
		logger.trace("setAttribute()");
	}

	@Override
	public void setAttributes(Map<Attribute, Object> attributes) throws IOException
	{
		// TODO ssh setAttributes
		logger.trace("setAttributes()");
	}

	@Override
	public void truncate() throws IOException
	{
		// TODO ssh truncate
		logger.trace("truncate()");
	}
}
