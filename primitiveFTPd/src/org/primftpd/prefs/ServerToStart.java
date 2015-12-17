package org.primftpd.prefs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum ServerToStart
{
	FTP("1") {
		@Override
		public boolean startFtp() {
			return true;
		}
		@Override
		public boolean startSftp() {
			return false;
		}
	},
	SFTP("2") {
		@Override
		public boolean startFtp() {
			return false;
		}
		@Override
		public boolean startSftp() {
			return true;
		}
		@Override
		public boolean isPasswordMandatory() {
			return false;
		}
	},
	ALL("0") {
		@Override
		public boolean startFtp() {
			return true;
		}
		@Override
		public boolean startSftp() {
			return true;
		}
	};

	private final String xmlValue;
	private ServerToStart(String xmlValue) {
		this.xmlValue = xmlValue;
	}
	public String xmlValue() {
		return xmlValue;
	}

	private static final Map<String, ServerToStart> XML_TO_ENUM;
	static {
		Map<String, ServerToStart> tmp = new HashMap<String, ServerToStart>();
		for (ServerToStart srvToStart : values()) {
			tmp.put(srvToStart.xmlValue, srvToStart);
		}
		XML_TO_ENUM = Collections.unmodifiableMap(tmp);
	}

	public static ServerToStart byXmlVal(String xmlVal) {
		return XML_TO_ENUM.get(xmlVal);
	}

	public abstract boolean startFtp();
	public abstract boolean startSftp();
	public boolean isPasswordMandatory() {
		return true;
	}
}
