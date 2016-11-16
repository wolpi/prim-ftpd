package org.primftpd.filesystem;

import org.apache.sshd.common.Session;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SshFile extends AndroidFile<org.apache.sshd.common.file.SshFile>
	implements org.apache.sshd.common.file.SshFile
{
	private final Session session;

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
			// as there is no proper sym link support in java 7, just return false, see GH issue #68
			return false;
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
