package org.primftpd.prefs;

import java.io.File;
import java.io.Serializable;

public class PrefsBean implements Serializable
{
	private static final long serialVersionUID = 1L;

	private final String userName;
	private final String password;
	private final boolean anonymousLogin;
	private final String portStr;
	private final String securePortStr;
	private final int port;
	private final int securePort;
	private final File startDir;
	private final boolean announce;
	private final String announceName;
	private final boolean wakelock;
	private final boolean pubKeyAuth;
	private final ServerToStart serverToStart;
	private final String ftpPassivePorts;
	private final Integer idleTimeout;
	private final boolean showConnectionInfoInNotification;
	private final StorageType storageType;
	private final String safUrl;
	private final String allowedIpsPattern;

	public PrefsBean(
		String userName,
		String password,
		boolean anonymousLogin,
		int port,
		int securePort,
		File startDir,
		boolean announce,
		String announceName,
		boolean wakelock,
		boolean pubKeyAuth,
		ServerToStart serverToStart,
		String ftpPassivePorts,
		Integer idleTimeout,
		boolean showConnectionInfoInNotification,
		StorageType storageType,
		String safUrl,
		String allowedIpsPattern)
	{
		super();
		this.userName = userName;
		this.password = password;
		this.anonymousLogin = anonymousLogin;
		this.port = port;
		this.securePort = securePort;
		this.portStr = String.valueOf(port);
		this.securePortStr = String.valueOf(securePort);
		this.startDir = startDir;
		this.announce = announce;
		this.announceName = announceName;
		this.wakelock = wakelock;
		this.pubKeyAuth = pubKeyAuth;
		this.serverToStart = serverToStart;
		this.ftpPassivePorts = ftpPassivePorts;
		this.idleTimeout = idleTimeout;
		this.showConnectionInfoInNotification = showConnectionInfoInNotification;
		this.storageType = storageType;
		this.safUrl = safUrl;
		this.allowedIpsPattern = allowedIpsPattern;
	}

	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}

	public boolean isAnonymousLogin() {
		return anonymousLogin;
	}

	public String getPortStr() {
		return portStr;
	}

	public int getPort() {
		return port;
	}

	public String getSecurePortStr() {
		return securePortStr;
	}

	public int getSecurePort()	{
		return securePort;
	}

	public File getStartDir() {
		return startDir;
	}

	public boolean isAnnounce() {
		return announce;
	}

	public String getAnnounceName() {
		return announceName;
	}

	public boolean isWakelock() {
		return wakelock;
	}

	public boolean isPubKeyAuth() {
		return pubKeyAuth;
	}

	public ServerToStart getServerToStart()
	{
		return serverToStart;
	}

	public String getFtpPassivePorts() {
		return ftpPassivePorts;
	}

	public Integer getIdleTimeout() {
		return idleTimeout;
	}

	public boolean showConnectionInfoInNotification() {
		return showConnectionInfoInNotification;
	}

	public StorageType getStorageType() {
		return storageType;
	}

	public String getSafUrl() {
		return safUrl;
	}

	public String getAllowedIpsPattern() {
		return allowedIpsPattern;
	}
}
