package org.primftpd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootUpReceiver extends BroadcastReceiver{
	@Override
	public void onReceive(Context context, Intent intent) {
		Intent i = new Intent(context, PrimitiveFtpdActivity.class);  
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.putExtra("BOOT", "1");
		context.startActivity(i);  
	}
}