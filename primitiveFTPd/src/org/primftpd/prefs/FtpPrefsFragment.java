package org.primftpd.prefs;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import org.primftpd.R;

public class FtpPrefsFragment extends PreferenceFragment
{
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}
}
