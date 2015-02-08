package org.primftpd.services;

import java.io.IOException;

import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.primftpd.AndroidPrefsUserManager;
import org.primftpd.filesystem.SshFileSystemView;

import android.os.Looper;

/**
 * Implements a SSH server. Intended to be used for sftp.
 */
public class SshServerService extends AbstractServerService
{
	private SshServer sshServer;

	@Override
	protected ServerServiceHandler createServiceHandler(
		Looper serviceLooper,
		AbstractServerService service)
	{
		return new ServerServiceHandler(serviceLooper, service, "ssh");
	}

	@Override
	protected Object getServer()
	{
		return sshServer;
	}

	@Override
	protected void stopServer()
	{
		try {
			sshServer.stop();
		} catch (InterruptedException e) {
			logger.error("could not stop ssh server", e);
		}
		sshServer = null;
	}

	@Override
	protected void launchServer()
	{
		sshServer = SshServer.setUpDefaultServer();
		sshServer.setPort(prefsBean.getSslPort());

		//sshServer.setUserAuthFactories(userAuthFactories);

		final AndroidPrefsUserManager userManager = new AndroidPrefsUserManager(prefsBean);
		sshServer.setPasswordAuthenticator(new PasswordAuthenticator() {
			@Override
			public boolean authenticate(
				String username,
				String password,
				ServerSession session)
			{
				try {
					userManager.authenticate(
						new UsernamePasswordAuthentication(
							username,
							password));
				} catch (AuthenticationFailedException e) {
					logger.debug("AuthenticationFailed", e);
					return false;
				}
				return true;
			}
		});
		sshServer.setFileSystemFactory(new FileSystemFactory() {
			@Override
			public FileSystemView createFileSystemView(Session session) throws IOException
			{
				return new SshFileSystemView(session);
			}
		});

    	try {
    		sshServer.start();
    	} catch (Exception e) {
    		sshServer = null;
			handleServerStartError(e);
    	}
	}
}
