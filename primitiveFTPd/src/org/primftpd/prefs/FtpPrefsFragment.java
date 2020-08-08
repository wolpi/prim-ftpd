package org.primftpd.prefs;

import android.app.Activity;
import android.content.Context;
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
import org.primftpd.filepicker.nononsenseapps.Utils;
import org.primftpd.log.CsvLoggerFactory;
import org.primftpd.util.Defaults;
import org.primftpd.util.NotificationUtil;
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
			logger.debug("disabling announce prefs, sdk: {}", Build.VERSION.SDK_INT);
			PreferenceCategory prefCat = (PreferenceCategory) getPreferenceManager().findPreference("ftpPrefCatSystem");

			Preference announcePref = getPreferenceManager().findPreference(LoadPrefsUtil.PREF_KEY_ANNOUNCE);
			prefCat.removePreference(announcePref);

			Preference announceNamePref = getPreferenceManager().findPreference(LoadPrefsUtil.PREF_KEY_ANNOUNCE_NAME);
			prefCat.removePreference(announceNamePref);

			prefCat = (PreferenceCategory) getPreferenceManager().findPreference("ftpPrefCatUi");
			Preference showConnInfoPref = getPreferenceManager().findPreference(LoadPrefsUtil.PREF_KEY_SHOW_CONN_INFO);
			prefCat.removePreference(showConnInfoPref);
		}

		// context
		final Context context = getActivity().getApplicationContext();

		// text parameter for pub key auth pref
		Resources res = getResources();
		String text = String.format(res.getString(R.string.prefSummaryPubKeyAuth), Defaults.pubKeyAuthKeyPath(context));
		Preference pubKeyAuthPref = findPreference(LoadPrefsUtil.PREF_KEY_PUB_KEY_AUTH);
		pubKeyAuthPref.setSummary(text);

		// text parameter for logging pref
		String textLogsPath = Defaults.homeDirScoped(context) + "/" + CsvLoggerFactory.LOGFILE_BASENAME + "*";
		if (textLogsPath.contains("//")) {
			textLogsPath = textLogsPath.replaceAll("//", "/");
		}
		String loggingText = String.format(res.getString(R.string.prefSummaryLoggingV2), textLogsPath);
		Preference loggingPref = findPreference(LoadPrefsUtil.PREF_KEY_LOGGING);
		loggingPref.setSummary(loggingText);

		// create / remove notification when pref is toggled
		Preference startStopNotificationPref = findPreference(LoadPrefsUtil.PREF_KEY_SHOW_START_STOP_NOTIFICATION);
		startStopNotificationPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (Boolean.TRUE.equals(newValue)) {
					NotificationUtil.createStartStopNotification(getActivity());
				} else {
					NotificationUtil.removeStartStopNotification(getActivity());
				}
				return true;
			}
		});

		// directory picker for choosing home dir
		startDirPref = (EditTextPreference)findPreference(LoadPrefsUtil.PREF_KEY_START_DIR);
		startDirPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				// don't show default dialog
				startDirPref.getDialog().dismiss();

				File startDirVal = LoadPrefsUtil.startDir(startDirPref.getSharedPreferences());
				logger.debug("using initial start dir val: {}", startDirVal);
				Intent dirPickerIntent = Defaults.createDefaultDirPicker(context, startDirVal);
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
			File file = Utils.getFileForUri(uri);
			String path = file.getAbsolutePath();
			logger.debug("got start dir path: {}", path);
			startDirPref.setText(path);
		}
	}
}
