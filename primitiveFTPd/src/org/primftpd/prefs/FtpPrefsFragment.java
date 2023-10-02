package org.primftpd.prefs;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;

import org.primftpd.R;
import org.primftpd.log.CsvLoggerFactory;
import org.primftpd.util.Defaults;
import org.primftpd.util.NotificationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;

public class FtpPrefsFragment extends PreferenceFragmentCompat
{
	protected Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		logger.debug("onCreatePreferences()");
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
		final Context context = getContext();

		// text parameter for pub key auth pref
		Resources res = getResources();
		String text = String.format(res.getString(R.string.prefSummaryPubKeyAuth_v2), Defaults.pubKeyAuthKeyPath(context));
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
		Preference startDirPref = findPreference(LoadPrefsUtil.PREF_KEY_START_DIR);
		startDirPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(@NonNull Preference preference) {
				File startDirVal = LoadPrefsUtil.startDir(startDirPref.getSharedPreferences());
				Intent dirPickerIntent = Defaults.createPrefDirPicker(context, startDirVal, LoadPrefsUtil.PREF_KEY_START_DIR);
				startDirPref.setIntent(dirPickerIntent);
				return false;
			}
		});
	}
}
