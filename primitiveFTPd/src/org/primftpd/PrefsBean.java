package org.primftpd;

import org.primftpd.prefs.ServerToStart;

import java.io.File;
import java.io.Serializable;

public class PrefsBean implements Serializable
{
	private static final long serialVersionUID = 1L;

	private final String userName;
	private final String password;
	private final String portStr;
	private final String securePortStr;
	private final int port;
	private final int securePort;
	private final File startDir;
	private final boolean announce;
	private final boolean wakelock;
	private final boolean pubKeyAuth;
    private final boolean anonymousLogin;
	private final ServerToStart serverToStart;

	public PrefsBean(
            String userName,
            String password,
            boolean anonymousLogin, int securePort, File startDir, boolean announce, boolean wakelock, boolean pubKeyAuth, int port,
            ServerToStart serverToStart)
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
		this.wakelock = wakelock;
		this.pubKeyAuth = pubKeyAuth;
		this.serverToStart = serverToStart;
	}

	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
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

	public boolean isAnnounce()
	{
		return announce;
	}

	public boolean isWakelock()
	{
		return wakelock;
	}

	public boolean isPubKeyAuth() {
		return pubKeyAuth;
	}

	public ServerToStart getServerToStart()
	{
		return serverToStart;
	}

    public boolean isAnonymousLogin() {
        return anonymousLogin;
    }
}
