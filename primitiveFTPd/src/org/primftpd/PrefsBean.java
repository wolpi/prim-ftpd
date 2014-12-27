package org.primftpd;

import java.io.Serializable;

public class PrefsBean implements Serializable
{
	private static final long serialVersionUID = 1L;

	private final String userName;
	private final String password;
	private final String portStr;
	private final String sslPortStr;
	private final int port;
	private final int sslPort;
	private final boolean announce;
	private final boolean bootstart;
	private final boolean wifimode;
	private final String allowedInterfaces;

	public PrefsBean(
		String userName,
		String password,
		int port,
		int sslPort,
		boolean announce,
		boolean bootstart,
		boolean wifimode,
		String allowedInterfaces)
	{
		super();
		this.userName = userName;
		this.password = password;
		this.port = port;
		this.sslPort = sslPort;
		this.portStr = String.valueOf(port);
		this.sslPortStr = String.valueOf(sslPort);
		this.announce = announce;
		this.bootstart = bootstart;
		this.wifimode = wifimode;
		this.allowedInterfaces = allowedInterfaces;
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

	public String getSslPortStr() {
		return sslPortStr;
	}

	public int getSslPort()	{
		return sslPort;
	}

	public boolean isAnnounce()
	{
		return announce;
	}

	public boolean isBootStart()
	{
		return bootstart;
	}

	public boolean isWifiMode()
	{
		return wifimode;
	}

	public String getAllowedInterfaces()
	{
		return allowedInterfaces;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (announce ? 1231 : 1237);
		result = prime * result
				+ ((password == null) ? 0 : password.hashCode());
		result = prime * result + port;
		result = prime * result + ((portStr == null) ? 0 : portStr.hashCode());
		result = prime * result + sslPort;
		result = prime * result
				+ ((sslPortStr == null) ? 0 : sslPortStr.hashCode());
		result = prime * result
				+ ((userName == null) ? 0 : userName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PrefsBean other = (PrefsBean) obj;
		if (announce != other.announce)
			return false;
		if (password == null) {
			if (other.password != null)
				return false;
		} else if (!password.equals(other.password))
			return false;
		if (port != other.port)
			return false;
		if (portStr == null) {
			if (other.portStr != null)
				return false;
		} else if (!portStr.equals(other.portStr))
			return false;
		if (sslPort != other.sslPort)
			return false;
		if (sslPortStr == null) {
			if (other.sslPortStr != null)
				return false;
		} else if (!sslPortStr.equals(other.sslPortStr))
			return false;
		if (userName == null) {
			if (other.userName != null)
				return false;
		} else if (!userName.equals(other.userName))
			return false;
		return true;
	}
}
