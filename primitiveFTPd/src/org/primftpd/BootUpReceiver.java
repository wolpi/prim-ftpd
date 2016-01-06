package org.primftpd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.primftpd.prefs.LoadPrefsUtil;
import org.primftpd.util.ServicesStartStopUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Invoked on system boot. Creates intent to launch server(s).
 */
public class BootUpReceiver extends BroadcastReceiver
{
	protected Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void onReceive(Context context, Intent intent) {
		// note: can be tested with:
		// adb shell
		// am broadcast -a android.intent.action.BOOT_COMPLETED

		SharedPreferences prefs = LoadPrefsUtil.getPrefs(context);
		Boolean startOnBoot = LoadPrefsUtil.startOnBoot(prefs);
		if (startOnBoot != null && startOnBoot.booleanValue()) {
			PrefsBean prefsBean = LoadPrefsUtil.loadPrefs(logger, prefs);
			ServicesStartStopUtil.startServers(context, prefsBean, null, null, null);
		}
	}
}
