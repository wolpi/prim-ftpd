package org.primftpd.prefs;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;

import org.primftpd.R;
import org.primftpd.util.Defaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class FtpPrefsFragment extends PreferenceFragment
{
	protected Logger logger = LoggerFactory.getLogger(getClass());

	private EditTextPreference startDirPref;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		logger.debug("onCreate()");
		addPreferencesFromResource(R.xml.preferences);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			logger.debug("disabling announce pref, sdk: {}", Build.VERSION.SDK_INT);
			Preference announcePref = getPreferenceManager().findPreference(LoadPrefsUtil.PREF_KEY_ANNOUNCE);
			PreferenceCategory prefCat = (PreferenceCategory) getPreferenceManager().findPreference("ftpPrefCat");
			prefCat.removePreference(announcePref);
		}

		// text parameter for pub key auth pref
		Resources res = getResources();
		String text = String.format(res.getString(R.string.prefSummaryPubKeyAuth), Defaults.PUB_KEY_AUTH_KEY_PATH);
		Preference pubKeyAuthPref = findPreference(LoadPrefsUtil.PREF_KEY_PUB_KEY_AUTH);
		pubKeyAuthPref.setSummary(text);

		// directory picker for choosing home dir
		startDirPref = (EditTextPreference)findPreference(LoadPrefsUtil.PREF_KEY_START_DIR);
		startDirPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				// don't show default dialog
				startDirPref.getDialog().dismiss();

				File startDirVal = LoadPrefsUtil.startDir(startDirPref.getSharedPreferences());
				logger.debug("using initial start dir val: {}", startDirVal);
				Intent dirPickerIntent = Defaults.createDefaultDirPicker(getActivity().getApplicationContext(), startDirVal);
				startActivityForResult(dirPickerIntent, 0);

				return false;
			}
		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		logger.debug("onActivityResult()");
		super.onActivityResult(requestCode, resultCode, data);

		if(resultCode == Activity.RESULT_OK) {
			Uri uri = data.getData();
			File file = com.nononsenseapps.filepicker.Utils.getFileForUri(uri);
			String path = file.getAbsolutePath();
			logger.debug("got start dir path: {}", path);
			startDirPref.setText(path);
		}
	}
}
