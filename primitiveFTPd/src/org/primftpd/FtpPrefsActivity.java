package org.primftpd;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class FtpPrefsActivity extends PreferenceActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}
}
