package org.primftpd.prefs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.primftpd.R;

public enum Theme
{
	DARK("0", "PrimFtpdDarkTheme", R.style.PrimFtpdDarkTheme),
	LIGHT("1", "PrimFtpdLightTheme", R.style.PrimFtpdLightTheme);

	private final String xmlValue;
	private final String themeName;
	private final int resourceId;

	private Theme(String xmlValue, String themeName, int resourceId) {
		this.xmlValue = xmlValue;
		this.themeName = themeName;
		this.resourceId = resourceId;
	}

	public String xmlValue() {
		return xmlValue;
	}
	public String themeName() {
		return themeName;
	}
	public int resourceId() {
		return resourceId;
	}

	private static final Map<String, Theme> XML_TO_ENUM;
	static {
		Map<String, Theme> tmp = new HashMap<String, Theme>();
		for (Theme theme : values()) {
			tmp.put(theme.xmlValue, theme);
		}
		XML_TO_ENUM = Collections.unmodifiableMap(tmp);
	}

	public static Theme byXmlVal(String xmlVal) {
		return XML_TO_ENUM.get(xmlVal);
	}
}
