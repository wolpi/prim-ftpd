package org.primftpd.filesystem;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.sshd.common.Session;

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
		logger.debug("create()");
		return file.createNewFile();
	}

	@Override
	public void createSymbolicLink(org.apache.sshd.common.file.SshFile arg0)
			throws IOException
	{
		// TODO ssh createSymbolicLink
		logger.debug("createSymbolicLink()");
	}

	@Override
	public Object getAttribute(Attribute arg0, boolean arg1) throws IOException
	{
		// TODO ssh getAttribute
		logger.debug("getAttribute()");
		return null;
	}

	@Override
	public Map<Attribute, Object> getAttributes(boolean arg0)
			throws IOException
	{
		// TODO ssh getAttributes
		logger.debug("getAttributes()");
		return null;
	}

	@Override
	public String getOwner()
	{
		logger.debug("getOwner()");
		return session.getUsername();
	}

	@Override
	public org.apache.sshd.common.file.SshFile getParentFile()
	{
		logger.debug("getParentFile()");
		return new SshFile(file.getParentFile(), session);
	}

	@Override
	public void handleClose() throws IOException
	{
		// TODO ssh handleClose
		logger.debug("handleClose()");
	}

	@Override
	public boolean isExecutable()
	{
		logger.debug("isExecutable()");
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
		return move(target);
	}

	@Override
	public String readSymbolicLink() throws IOException
	{
		// TODO ssh readSymbolicLink
		logger.debug("readSymbolicLink()");
		return null;
	}

	@Override
	public void setAttribute(Attribute arg0, Object arg1) throws IOException
	{
		// TODO ssh setAttribute
		logger.debug("setAttribute()");
	}

	@Override
	public void setAttributes(Map<Attribute, Object> arg0) throws IOException
	{
		// TODO ssh setAttributes
		logger.debug("setAttributes()");
	}

	@Override
	public void truncate() throws IOException
	{
		// TODO ssh truncate
		logger.debug("truncate()");
	}
}
