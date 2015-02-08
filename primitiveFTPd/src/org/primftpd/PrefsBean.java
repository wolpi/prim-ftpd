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
		result = prime * result + securePort;
		result = prime * result
				+ ((securePortStr == null) ? 0 : securePortStr.hashCode());
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
		if (securePort != other.securePort)
			return false;
		if (securePortStr == null) {
			if (other.securePortStr != null)
				return false;
		} else if (!securePortStr.equals(other.securePortStr))
			return false;
		if (userName == null) {
			if (other.userName != null)
				return false;
		} else if (!userName.equals(other.userName))
			return false;
		return true;
	}
}
