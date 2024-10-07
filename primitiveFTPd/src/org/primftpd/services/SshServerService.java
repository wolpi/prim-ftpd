package org.primftpd.services;

import android.net.Uri;
import android.os.Looper;
import android.util.Base64;
import android.widget.Toast;

import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.usermanager.AnonymousAuthentication;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.util.IoUtils;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.Signature;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.io.mina.MinaServiceFactoryFactory;
import org.apache.sshd.common.keyprovider.AbstractKeyPairProvider;
import org.apache.sshd.common.session.AbstractSession;
import org.apache.sshd.common.util.KeyUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.session.SessionFactory;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.primftpd.R;
import org.primftpd.crypto.HostKeyAlgorithm;
import org.primftpd.crypto.SignatureEd25519;
import org.primftpd.events.ClientActionEvent;
import org.primftpd.filesystem.FsSshFileSystemView;
import org.primftpd.filesystem.QuickShareSshFileSystemView;
import org.primftpd.filesystem.RoSafSshFileSystemView;
import org.primftpd.filesystem.RootSshFileSystemView;
import org.primftpd.filesystem.SafSshFileSystemView;
import org.primftpd.filesystem.VirtualSshFileSystemView;
import org.primftpd.pojo.KeyParser;
import org.primftpd.util.Defaults;
import org.primftpd.util.RemoteIpChecker;
import org.primftpd.util.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

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
	protected ClientActionEvent.Protocol getProtocol() {
		return ClientActionEvent.Protocol.SFTP;
	}

	@Override
	protected void stopServer()
	{
		try {
			if (sshServer == null) {
				logger.info("ssh server already null");
				return;
			}
			List<AbstractSession> activeSessions = sshServer.getActiveSessions();
			for (AbstractSession session : activeSessions) {
				try {
					session.disconnect(-1, "server close");
				} catch (IOException e) {
					logger.error("could not end active session", e);
				}
			}
			sshServer.stop(true);
			sshServer.close(true);
		} catch (InterruptedException e) {
			logger.error("could not stop ssh server", e);
		}
		sshServer = null;
	}

	@Override
	protected boolean launchServer(final Shell.Interactive shell)
	{
		sshServer = SshServer.setUpDefaultServer();
		sshServer.setPort(prefsBean.getSecurePort());
		String bindIp = prefsBean.getBindIp();
		if (bindIp != null) {
			sshServer.setHost(bindIp);
		}

		// causes exception when not set
		sshServer.setIoServiceFactoryFactory(new MinaServiceFactoryFactory());

		sshServer.setSessionFactory(new SessionFactory() {
			@Override
			protected AbstractSession createSession(IoSession ioSession) throws Exception {
				SocketAddress remoteAddress = ioSession.getRemoteAddress();
				boolean ipAllowed = RemoteIpChecker.ipAllowed(remoteAddress, prefsBean, logger);
				return ipAllowed ? super.createSession(ioSession) : null;
			}
		});

		// enable scp and sftp
		sshServer.setCommandFactory(new ScpCommandFactory());
		List<NamedFactory<Command>> factoryList = new ArrayList<>(1);
		factoryList.add(new SftpSubsystem.Factory());
		sshServer.setSubsystemFactories(factoryList);

		// PasswordAuthenticator based on android preferences
		if (StringUtils.isNotEmpty(prefsBean.getPassword())
				|| prefsBean.isAnonymousLogin())
		{
			final AndroidPrefsUserManager userManager = new AndroidPrefsUserManager(prefsBean);
			sshServer.setPasswordAuthenticator((username, password, session) -> {
			Authentication authentication = prefsBean.isAnonymousLogin()
				? new AnonymousAuthentication()
				: new UsernamePasswordAuthentication(username, password);
			logger.debug("auth type '{}' for user: {}", authentication.getClass().getName(), username);
			try {
				userManager.authenticate(authentication);
			} catch (AuthenticationFailedException e) {
				logger.debug("AuthenticationFailed", e);
				return false;
			}
			return true;
			});
		}

		if (prefsBean.isPubKeyAuth()) {
			String[] keyPaths = new String[] {
					Defaults.pubKeyAuthKeyPath(getApplicationContext()),
					Defaults.PUB_KEY_AUTH_KEY_PATH_OLD,
					Defaults.PUB_KEY_AUTH_KEY_PATH_OLDER,
			};
			final List<PublicKey> pubKeys = new ArrayList<>();
			for (String keyPath : keyPaths) {
				pubKeys.addAll(readKeyAuthKeys(keyPath, true));
			}
			logger.info("loaded {} keys for public key auth", pubKeys.size());
			if (!pubKeys.isEmpty()) {
				sshServer.setPublickeyAuthenticator(new PubKeyAuthenticator(pubKeys));
			} else {
				Toast.makeText(
					getApplicationContext(),
					getText(R.string.couldNotReadKeyAuthKey),
					Toast.LENGTH_SHORT).show();
			}
		}

		// android filesystem view
		sshServer.setFileSystemFactory(session -> {
			if (quickShareBean != null) {
				logger.debug("launching server in quick share mode");
				return new QuickShareSshFileSystemView(
						SshServerService.this,
						quickShareBean.getTmpDir(),
						session);
			} else {
				switch (prefsBean.getStorageType()) {
					case PLAIN:
						return new FsSshFileSystemView(
								SshServerService.this,
								Uri.parse(prefsBean.getSafUrl()),
								prefsBean.getStartDir(),
								session);
					case ROOT:
						return new RootSshFileSystemView(
								SshServerService.this,
								shell,
								prefsBean.getStartDir(),
								session);
					case SAF:
						return new SafSshFileSystemView(
								SshServerService.this,
								Uri.parse(prefsBean.getSafUrl()),
								session);
					case RO_SAF:
						return new RoSafSshFileSystemView(
								SshServerService.this,
								Uri.parse(prefsBean.getSafUrl()),
								session);
					case VIRTUAL:
						return new VirtualSshFileSystemView(
								SshServerService.this,
								new FsSshFileSystemView(
										SshServerService.this,
										Uri.parse(prefsBean.getSafUrl()),
										prefsBean.getStartDir(),
										session),
								new RootSshFileSystemView(
										SshServerService.this,
										shell,
										prefsBean.getStartDir(),
										session),
								new SafSshFileSystemView(
										SshServerService.this,
										Uri.parse(prefsBean.getSafUrl()),
										session),
								new RoSafSshFileSystemView(
										SshServerService.this,
										Uri.parse(prefsBean.getSafUrl()),
										session),
								prefsBean.getStartDir(),
								session
						);
				}
			}
			return null;
		});

		// ed25519
		List<NamedFactory<Signature>> origSigFactories = sshServer.getSignatureFactories();
		List<NamedFactory<Signature>> sigFactories = new ArrayList<>(origSigFactories.size() + 1);
		sigFactories.addAll(origSigFactories);
		sigFactories.add(new SignatureEd25519.Factory());
		sshServer.setSignatureFactories(sigFactories);

		// idle timeout
		// sec -> ms
		sshServer.getProperties().put(SshServer.IDLE_TIMEOUT, String.valueOf(prefsBean.getIdleTimeout() * 1000));

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
					private KeyPair ed25519KeyPair = null;

					@Override
					public Iterable<KeyPair> loadKeys() {
						// just return keys that have been loaded before
						return keys;
					}

					@Override
					public KeyPair loadKey(String type) {
						if ("ssh-ed25519".equals(type)) {
							return ed25519KeyPair;
						}
						return super.loadKey(type);
					}

					@Override
					public String getKeyTypes() {
						List<String> types = new ArrayList<>();
						for (KeyPair keyPair : keys) {
							String keyType = KeyUtils.getKeyType(keyPair);
							if (keyType == null) {
								String algo = keyPair.getPrivate().getAlgorithm();
								if ("Ed25519".equals(algo)) {
									keyType = "ssh-ed25519";
									ed25519KeyPair = keyPair;
								}
							}
							types.add(keyType);
						}

						StringBuilder sb = new StringBuilder();
						String delimiter = "";
						for (String type : types) {
							sb.append(delimiter);
							sb.append(type);
							delimiter = ",";
						}
						return sb.toString();
					}
				});
				sshServer.start();
				return true;
			}
		} catch (Throwable e) {
			sshServer = null;
			handleServerStartError(e);
		}
		return false;
	}

	protected List<KeyPair> loadKeys() {
		List<KeyPair> keyPairList = new ArrayList<>(1);
		for (HostKeyAlgorithm hka : HostKeyAlgorithm.values()) {
			FileInputStream pubkeyFis = null;
			FileInputStream privkeyFis = null;
			try {
				pubkeyFis = openFileInput(hka.getFilenamePublicKey());
				PublicKey publicKey = hka.readPublicKey(pubkeyFis);

				privkeyFis = openFileInput(hka.getFilenamePrivateKey());
				PrivateKey privateKey = hka.readPrivateKey(privkeyFis);

				// return key pair
				keyPairList.add(new KeyPair(publicKey, privateKey));
			} catch (Exception e) {
				logger.debug("could not read key: " + e.getClass().getName() + " " + e.getMessage());
			} finally {
				if (pubkeyFis != null) {
					IoUtils.close(pubkeyFis);
				}
				if (privkeyFis != null) {
					IoUtils.close(privkeyFis);
				}
			}
		}
		return keyPairList;
	}

	protected List<PublicKey> readKeyAuthKeys(String path, boolean ignoreErrors)
	{
		List<PublicKey> keys = null;
		FileInputStream fis = null;
		try {
			logger.debug("trying authorized keys file {}", path);
			fis = new FileInputStream(path);
			List<String> parserErrors = new ArrayList<>();
			keys = KeyParser.parsePublicKeys(
					fis,
					str -> Base64.decode(str, Base64.DEFAULT),
					parserErrors);

			for (String parserError : parserErrors) {
				logger.debug("{}", parserError);
			}

		} catch (Exception e) {
			logger.debug("could not read keys {}, {}", e.getClass().getSimpleName(), e.getMessage());
			if (!ignoreErrors) {
				logger.error("could not read key auth keys", e);
			}
		} finally {
			try {
				if (fis != null) {
					fis.close();
				}
			} catch (IOException e) {
				if (!ignoreErrors) {
					logger.error("could not close key auth keys file", e);
				}
			}
		}
		return keys != null ? keys : Collections.emptyList();
	}
}
