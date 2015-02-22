package org.primftpd.prefs;

import org.primftpd.R;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class FtpPrefsFragment extends PreferenceFragment
{
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}
}
