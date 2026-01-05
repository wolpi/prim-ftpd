package org.primftpd.prefs;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;

import org.primftpd.R;
import org.primftpd.log.LogController;
import org.primftpd.util.Defaults;
import org.primftpd.util.NotificationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import androidx.preference.Preference;
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
            PreferenceCategory prefCat = getPreferenceManager().findPreference("ftpPrefCatSystem");

            if (prefCat != null) {
                Preference announcePref = getPreferenceManager().findPreference(LoadPrefsUtil.PREF_KEY_ANNOUNCE);
                if (announcePref != null) {
                    prefCat.removePreference(announcePref);
                }

                Preference announceNamePref = getPreferenceManager().findPreference(LoadPrefsUtil.PREF_KEY_ANNOUNCE_NAME);
                if (announceNamePref != null) {
                    prefCat.removePreference(announceNamePref);
                }

                prefCat = getPreferenceManager().findPreference("ftpPrefCatUi");
                if (prefCat != null) {
                    Preference showConnInfoPref = getPreferenceManager().findPreference(LoadPrefsUtil.PREF_KEY_SHOW_CONN_INFO);
                    if (showConnInfoPref != null) {
                        prefCat.removePreference(showConnInfoPref);
                    }
                }
            }
        }
        final Context context = getContext();
        if (context != null) {
            // text parameter for pub key auth pref
            Resources res = getResources();
            String text = String.format(
                    res.getString(R.string.prefSummaryPubKeyAuth_v2),
                    Defaults.pubKeyAuthKeyPath(context));
            Preference pubKeyAuthPref = findPreference(LoadPrefsUtil.PREF_KEY_PUB_KEY_AUTH);
            if (pubKeyAuthPref != null) {
                pubKeyAuthPref.setSummary(text);
            }

            // text parameter for logging pref
            String textLogsPath = Defaults.homeDirScoped(context).getAbsolutePath()
                                  + '/' + LogController.LOGFILE_BASENAME + '*';
            if (textLogsPath.contains("//")) {
                textLogsPath = textLogsPath.replaceAll("//", "/");
            }
            String loggingText = String.format(
                    res.getString(R.string.prefSummaryLoggingV2),
                    textLogsPath);
            Preference loggingPref = findPreference(LoadPrefsUtil.PREF_KEY_LOGGING);
            if (loggingPref != null) {
                loggingPref.setSummary(loggingText);
            }

            // create / remove notification when pref is toggled
            Preference startStopNotificationPref = findPreference(LoadPrefsUtil.PREF_KEY_SHOW_START_STOP_NOTIFICATION);
            if (startStopNotificationPref != null) {
                startStopNotificationPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    if (Boolean.TRUE.equals(newValue)) {
                        NotificationUtil.createStartStopNotification(context);
                    } else {
                        NotificationUtil.removeStartStopNotification(context);
                    }
                    return true;
                });
            }
        }

        // directory picker for choosing home dir
        Preference startDirPref = findPreference(LoadPrefsUtil.PREF_KEY_START_DIR);
        if (startDirPref != null) {
            startDirPref.setOnPreferenceClickListener(preference -> {
                SharedPreferences sharedPrefs = startDirPref.getSharedPreferences();
                if (sharedPrefs != null) {
                    File startDirVal = LoadPrefsUtil.startDir(sharedPrefs);
                    Intent dirPickerIntent = Defaults.createPrefDirPicker(
                            context,
                            startDirVal,
                            LoadPrefsUtil.PREF_KEY_START_DIR);
                    startDirPref.setIntent(dirPickerIntent);
                }
                return false;
            });
        }
    }
}
