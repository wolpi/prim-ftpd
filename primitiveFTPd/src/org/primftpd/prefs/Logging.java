package org.primftpd.prefs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum Logging
{
	TEXT("1"),
	ANDROID("2"),
	NONE("0");

	private final String xmlValue;
	private Logging(String xmlValue) {
		this.xmlValue = xmlValue;
	}
	public String xmlValue() {
		return xmlValue;
	}

	private static final Map<String, Logging> XML_TO_ENUM;
	static {
		Map<String, Logging> tmp = new HashMap<String, Logging>();
		for (Logging srvToStart : values()) {
			tmp.put(srvToStart.xmlValue, srvToStart);
		}
		XML_TO_ENUM = Collections.unmodifiableMap(tmp);
	}

	public static Logging byXmlVal(String xmlVal) {
		return XML_TO_ENUM.get(xmlVal);
	}
}
