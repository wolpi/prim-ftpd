package org.primftpd.prefs;

import android.app.UiModeManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.primftpd.R;

import androidx.appcompat.app.AppCompatDelegate;

public enum Theme
{
	DARK("0", "PrimFtpdDarkTheme", R.style.PrimFtpdDarkTheme) {
		@Override
		public int getUiModeValue() {
			return UiModeManager.MODE_NIGHT_YES;
		}

		@Override
		public int getAppCompatValue() {
			return AppCompatDelegate.MODE_NIGHT_YES;
		}
	},

	LIGHT("1", "PrimFtpdLightTheme", R.style.PrimFtpdLightTheme) {
		@Override
		public int getUiModeValue() {
			return UiModeManager.MODE_NIGHT_NO;
		}

		@Override
		public int getAppCompatValue() {
			return AppCompatDelegate.MODE_NIGHT_NO;
		}
	},

	SYS_DEFAULT("2", "PrimFtpdDeviceTheme", R.style.PrimFtpdDeviceTheme) {
		@Override
		public int getUiModeValue() {
			return UiModeManager.MODE_NIGHT_AUTO;
		}

		@Override
		public int getAppCompatValue() {
			return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
		}
	};

	private final String xmlValue;
	private final int resourceId;

	Theme(String xmlValue, @SuppressWarnings("unused") String themeName, int resourceId) {
		this.xmlValue = xmlValue;
		this.resourceId = resourceId;
	}

	public String xmlValue() {
		return xmlValue;
	}
	public int resourceId() {
		return resourceId;
	}

	private static final Map<String, Theme> XML_TO_ENUM;
	static {
		Map<String, Theme> tmp = new HashMap<>();
		for (Theme theme : values()) {
			tmp.put(theme.xmlValue, theme);
		}
		XML_TO_ENUM = Collections.unmodifiableMap(tmp);
	}

	public static Theme byXmlVal(String xmlVal) {
		return XML_TO_ENUM.get(xmlVal);
	}

	public abstract int getUiModeValue();
	public abstract int getAppCompatValue();
}
