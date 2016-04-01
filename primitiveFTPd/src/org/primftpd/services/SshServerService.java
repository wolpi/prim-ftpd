package org.primftpd.services;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.usermanager.AnonymousAuthentication;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.util.IoUtils;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.io.mina.MinaServiceFactoryFactory;
import org.apache.sshd.common.keyprovider.AbstractKeyPairProvider;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.primftpd.AndroidPrefsUserManager;
import org.primftpd.PrimitiveFtpdActivity;
import org.primftpd.R;
import org.primftpd.filesystem.SshFileSystemView;
import org.primftpd.util.Defaults;
import org.primftpd.util.KeyInfoProvider;
import org.primftpd.util.StringUtils;

import android.os.Looper;
import android.widget.Toast;

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
		return new ServerServiceHandler(serviceLooper, service, getServiceName());
	}

	@Override
	protected Object getServer()
	{
		return sshServer;
	}

	@Override
	protected int getPort()
	{
		return prefsBean.getSecurePort();
	}

	@Override
	protected String getServiceName()
	{
		return "sftp-ssh";
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
	protected boolean launchServer()
	{
		sshServer = SshServer.setUpDefaultServer();
		sshServer.setPort(prefsBean.getSecurePort());

		// causes exception when not set
		sshServer.setIoServiceFactoryFactory(new MinaServiceFactoryFactory());

		// enable scp and sftp
		sshServer.setCommandFactory(new ScpCommandFactory());
		List<NamedFactory<Command>> factoryList = new ArrayList<NamedFactory<Command>>(1);
		factoryList.add(new SftpSubsystem.Factory());
		sshServer.setSubsystemFactories(factoryList);

		// PasswordAuthenticator based on android preferences
		if (StringUtils.isNotEmpty(prefsBean.getPassword())
				|| prefsBean.isAnonymousLogin())
		{
			final AndroidPrefsUserManager userManager = new AndroidPrefsUserManager(prefsBean);
			sshServer.setPasswordAuthenticator(new PasswordAuthenticator() {
				@Override
				public boolean authenticate(
					String username,
					String password,
					ServerSession session) {
				logger.debug("password auth for user: {}", username);
				Authentication authentication = AndroidPrefsUserManager.ANONYMOUS_USER_NAME.equals(username)
					? new AnonymousAuthentication()
					: new UsernamePasswordAuthentication(username, password);
				try {
					userManager.authenticate(authentication);
				} catch (AuthenticationFailedException e) {
					logger.debug("AuthenticationFailed", e);
					return false;
				}
				return true;
				}
			});
		}

		if (prefsBean.isPubKeyAuth()) {
			String pubKeyPath = Defaults.PUB_KEY_AUTH_KEY_PATH;
			final PublicKey pubKey = new KeyInfoProvider().readKeyAuthKey(pubKeyPath);
			if (pubKey != null) {
				sshServer.setPublickeyAuthenticator(new PublickeyAuthenticator() {
					@Override
					public boolean authenticate(String username, PublicKey key, ServerSession session) {
						// never mind username
						boolean keyEquals = pubKey.equals(key);
						logger.debug("pub key auth, success: {}", keyEquals);
						return keyEquals;
					}
				});
			} else {
				Toast.makeText(
					getApplicationContext(),
					getText(R.string.couldNotReadKeyAuthKey),
					Toast.LENGTH_SHORT).show();
			}
		}

		// android filesystem view
		sshServer.setFileSystemFactory(new FileSystemFactory() {
			@Override
			public FileSystemView createFileSystemView(Session session) throws IOException
			{
				return new SshFileSystemView(prefsBean.getStartDir(), session);
			}
		});

		try {
			// XXX preference to enable shell? seems to need root to access /dev/tty
//			sshServer.setShellFactory(new ProcessShellFactory(new String[] {
//				"/system/bin/sh",
//				"-i",
//				"-l"
//			}));

			// read keys here, cannot open private files on server callback
			final List<KeyPair> keys = loadKeys();

			// keys may not be present when started via widget
			if (!keys.isEmpty()) {
				// setKeyPairProvider
				sshServer.setKeyPairProvider(new AbstractKeyPairProvider() {
					@Override
					public Iterable<KeyPair> loadKeys() {
						// just return keys that have been loaded before
						return keys;
					}
				});
				sshServer.start();
				return true;
			}
		} catch (Exception e) {
			sshServer = null;
			handleServerStartError(e);
		}
		return false;
	}

	protected List<KeyPair> loadKeys() {
		List<KeyPair> keyPairList = new ArrayList<KeyPair>(1);
		FileInputStream pubkeyFis = null;
		FileInputStream privkeyFis = null;
		try {
			// read pub key
			KeyInfoProvider keyInfoProvider = new KeyInfoProvider();

			pubkeyFis = openFileInput(PrimitiveFtpdActivity.PUBLICKEY_FILENAME);
			PublicKey publicKey = keyInfoProvider.readPublicKey(pubkeyFis);

			// read priv key from it's own file
			privkeyFis = openFileInput(PrimitiveFtpdActivity.PRIVATEKEY_FILENAME);
			PrivateKey privateKey = keyInfoProvider.readPrivatekey(privkeyFis);

			// return key pair
			keyPairList.add(new KeyPair(publicKey, privateKey));
		} catch (Exception e) {
			logger.debug("could not read key: " + e.getMessage(), e);
		} finally {
			if (pubkeyFis != null) {
				IoUtils.close(pubkeyFis);
			}
			if (privkeyFis != null) {
				IoUtils.close(privkeyFis);
			}
		}
		return keyPairList;
	}
}
