package org.primftpd.prefs;

import org.primftpd.R;

import android.app.AlertDialog;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import java.io.File;

public class FtpPrefsFragment extends PreferenceFragment
{
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		// validate start dir pref
		EditTextPreference startDirPref = (EditTextPreference) getPreferenceScreen().findPreference(
			LoadPrefsUtil.PREF_KEY_START_DIR);
		startDirPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				File startDir = new File((String)newValue);
				if (startDir.exists() && startDir.isDirectory()) {
					return true;
				}
				final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage(getText(R.string.invalidDir));
				builder.setPositiveButton(android.R.string.ok, null);
				builder.show();
				return false;
			}
		});
	}
}
