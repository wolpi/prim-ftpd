package org.primftpd.prefs;

import android.content.Context;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import android.widget.Toast;

import org.primftpd.R;
import org.slf4j.LoggerFactory;

public class PortEditTextPreference extends EditTextPreference implements Preference.OnPreferenceChangeListener
{
	public PortEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setOnPreferenceChangeListener(this);
	}

	public PortEditTextPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOnPreferenceChangeListener(this);
	}

	public PortEditTextPreference(Context context) {
		super(context);
		setOnPreferenceChangeListener(this);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		int port = 0;
		try {
			port = Integer.parseInt((String) newValue);
		} catch (Exception e) {
			// never mind
		}
		boolean valid = LoadPrefsUtil.validatePort(port);
		if (!valid) {
			Toast.makeText(
				getContext(),
				R.string.portInvalid_v2,
				Toast.LENGTH_LONG).show();
		} else {
			// check that both ports have different values
			String thisKey = preference.getKey();
			String otherKey = LoadPrefsUtil.PREF_KEY_PORT.equals(thisKey)
				? LoadPrefsUtil.PREF_KEY_SECURE_PORT
				: LoadPrefsUtil.PREF_KEY_PORT;
			int otherDefaultVal = LoadPrefsUtil.PREF_KEY_PORT.equals(otherKey)
				? LoadPrefsUtil.PORT_DEFAULT_VAL
				: LoadPrefsUtil.SECURE_PORT_DEFAULT_VAL;
			String otherDefaultValStr = LoadPrefsUtil.PORT_DEFAULT_VAL == otherDefaultVal
				? LoadPrefsUtil.PORT_DEFAULT_VAL_STR
				: LoadPrefsUtil.SECURE_PORT_DEFAULT_VAL_STR;
			int otherVal = LoadPrefsUtil.loadPort(
				LoggerFactory.getLogger(getClass()),
				preference.getSharedPreferences(),
				otherKey,
				otherDefaultVal,
				otherDefaultValStr);

			if (port == otherVal) {
				valid = false;
				Toast.makeText(
					getContext(),
					R.string.portsEqual_v2,
					Toast.LENGTH_LONG).show();
			}
		}
		return valid;
	}
}
