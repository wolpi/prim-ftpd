package org.primftpd.services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.util.IoUtils;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.io.mina.MinaServiceFactoryFactory;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.primftpd.AndroidPrefsUserManager;
import org.primftpd.R;
import org.primftpd.filesystem.SshFileSystemView;

import android.content.res.Resources;
import android.os.Environment;
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
		sshServer.setPort(prefsBean.getSecurePort());

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

		// causes exception when not set
		sshServer.setIoServiceFactoryFactory(new MinaServiceFactoryFactory());

		// enable sftp
		sshServer.setCommandFactory(new ScpCommandFactory());

		try {
			// load key
			String keyFile = copyKeyToFile();
			sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(keyFile));

    		sshServer.start();
    	} catch (Exception e) {
    		sshServer = null;
			handleServerStartError(e);
    	}
	}

	private String copyKeyToFile() throws FileNotFoundException, IOException {
		// TODO remove this, make it properly
		Resources resources = getResources();
		InputStream inputStream = resources.openRawResource(R.raw.ca);
		File dir = Environment.getExternalStorageDirectory();
		File keyFile = new File(dir, "pftpd-key.pem");
		IoUtils.copy(inputStream, new FileOutputStream(keyFile), 4096);
		return keyFile.getAbsolutePath();
	}
}
