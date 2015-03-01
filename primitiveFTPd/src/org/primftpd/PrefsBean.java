package org.primftpd;

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
	private final boolean announce;

	public PrefsBean(
		String userName,
		String password,
		int port,
		int securePort,
		boolean announce)
	{
		super();
		this.userName = userName;
		this.password = password;
		this.port = port;
		this.securePort = securePort;
		this.portStr = String.valueOf(port);
		this.securePortStr = String.valueOf(securePort);
		this.announce = announce;
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

	public boolean isAnnounce()
	{
		return announce;
	}
}
