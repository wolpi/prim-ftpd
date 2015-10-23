package org.primftpd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Invoked on system boot. Creates intent to launch server(s).
 */
public class BootUpReceiver extends BroadcastReceiver
{
	public static final String EXTRAS_KEY = "BOOT";

	@Override
	public void onReceive(Context context, Intent intent) {
		// note: can be tested with:
		// adb shell
		// am broadcast -a android.intent.action.BOOT_COMPLETED

		Intent i = new Intent(context, PrimitiveFtpdActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.putExtra(EXTRAS_KEY, true);
		context.startActivity(i);
	}
}
