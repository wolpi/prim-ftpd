package org.primftpd.remotecontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import org.primftpd.PrefsBean;
import org.primftpd.prefs.LoadPrefsUtil;
import org.primftpd.util.ServersRunningBean;
import org.primftpd.util.ServicesStartStopUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PftpdTaskerReceiver extends BroadcastReceiver {

    protected static Logger logger = LoggerFactory.getLogger(PftpdTaskerReceiver.class);

    private static final String ACTION_FIRE_SETTING = "com.twofortyfouram.locale.intent.action.FIRE_SETTING";
    private static final String EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE";
    private static final String EXTRA_STRING_BLURB = "com.twofortyfouram.locale.intent.extra.BLURB";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_FIRE_SETTING.equals(intent.getAction())) {
            String blurb = intent.getExtras().getString(EXTRA_STRING_BLURB);
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
        }
    }

    private void startServer(Context context) {
        SharedPreferences prefs = LoadPrefsUtil.getPrefs(context);
        PrefsBean prefsBean = LoadPrefsUtil.loadPrefs(logger, prefs);
        ServicesStartStopUtil.startServers(context, prefsBean, null);
    }

    private void stopServer(Context context) {
        ServicesStartStopUtil.stopServers(context);
    }

    private static final String PACKAGE_NAME = PftpdTaskerReceiver.class.getPackage().getName();
    private static final String BUNDLE_EXTRA_INT_VERSION_CODE = PACKAGE_NAME + ".extra.INT_VERSION_CODE";

    public static Intent buildResultIntent(final Context context, TaskerAction selectedAction) {
        final Intent resultIntent = new Intent();
        final Bundle resultBundle = PftpdTaskerReceiver.generateBundle(context);
        resultIntent.putExtra(EXTRA_BUNDLE, resultBundle);
        resultIntent.putExtra(EXTRA_STRING_BLURB, selectedAction.getBlurb());
        return resultIntent;
    }
    private static Bundle generateBundle(final Context context) {
        final Bundle result = new Bundle();
        result.putInt(BUNDLE_EXTRA_INT_VERSION_CODE, getVersionCode(context));
        return result;
    }
    private static int getVersionCode(final Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            logger.error("", e);
            return 0;
        }
    }
}
