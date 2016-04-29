package org.primftpd.prefs;

import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;

import org.primftpd.R;

public class FtpPrefsFragment extends PreferenceFragment
{
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			Preference announcePref = getPreferenceManager().findPreference(LoadPrefsUtil.PREF_KEY_ANNOUNCE);
			PreferenceCategory prefCat = (PreferenceCategory) getPreferenceManager().findPreference("ftpPrefCat");
			prefCat.removePreference(announcePref);
		}
	}
}
