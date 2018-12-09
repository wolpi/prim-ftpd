package org.primftpd.prefs;

import android.content.Context;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import android.widget.Toast;

import org.primftpd.R;
import org.slf4j.LoggerFactory;

public class FtpPassivePortsEditTextPreference extends EditTextPreference implements Preference.OnPreferenceChangeListener
{
	public FtpPassivePortsEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setOnPreferenceChangeListener(this);
	}

	public FtpPassivePortsEditTextPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOnPreferenceChangeListener(this);
	}

	public FtpPassivePortsEditTextPreference(Context context) {
		super(context);
		setOnPreferenceChangeListener(this);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		boolean valid = newValue instanceof String;
		if (valid && newValue != null) {
			valid = LoadPrefsUtil.validateFtpPassivePorts((String)newValue);
			if (!valid) {
				Toast.makeText(
						getContext(),
						R.string.ftpPassivePortsInvalid,
						Toast.LENGTH_LONG).show();

			}
		}
		return valid;
	}
}
