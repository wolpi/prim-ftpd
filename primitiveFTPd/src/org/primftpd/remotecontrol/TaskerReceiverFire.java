package org.primftpd.remotecontrol;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import org.primftpd.R;
import org.primftpd.prefs.LoadPrefsUtil;
import org.primftpd.util.ServersRunningBean;
import org.primftpd.util.ServicesStartStopUtil;

public class TaskerReceiverFire extends TaskerReceiver {

    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = LoadPrefsUtil.getPrefs(context);
        Boolean taskerEnabled = LoadPrefsUtil.taskerEnabled(prefs);
        if (!taskerEnabled) {
            Toast.makeText(context, R.string.taskerDisabledInPreferences, Toast.LENGTH_LONG).show();
            return;
        }

        String blurb = null;
        if (intent.getExtras() != null) {
            blurb = intent.getExtras().getString(EXTRA_STRING_BLURB);
        }
        logger.debug("onReceive() action: '{}', blurb: '{}'", intent.getAction(), blurb);
        if (ACTION_FIRE_SETTING.equals(intent.getAction())) {
            TaskerAction action = TaskerAction.byBlurb(blurb);
            if (action != null) {
                ServersRunningBean runningBean = ServicesStartStopUtil.checkServicesRunning(context);
                boolean running = runningBean.atLeastOneRunning();
                switch (action) {
                    case START:
                        if (!running) {
                            startServer(context);
                        }
                        break;
                    case STOP:
                        if (running) {
                            stopServer(context);
                        }
                        break;
                    case TOGGLE:
                        if (running) {
                            stopServer(context);
                        } else {
                            startServer(context);
                        }
                        break;
                }
            }
        } else {
            logger.error("invalid action for this activity");
        }
    }
}
