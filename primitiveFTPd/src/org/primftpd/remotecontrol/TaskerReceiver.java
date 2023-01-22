package org.primftpd.remotecontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import org.primftpd.util.ServersRunningBean;
import org.primftpd.util.ServicesStartStopUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskerReceiver extends BroadcastReceiver {

    protected static Logger logger = LoggerFactory.getLogger(TaskerReceiver.class);

    // see
    // https://github.com/twofortyfouram/android-plugin-api-for-locale/blob/master/pluginApiLib/src/main/java/com/twofortyfouram/locale/api/Intent.java
    static final String ACTION_FIRE_SETTING = "com.twofortyfouram.locale.intent.action.FIRE_SETTING";
    static final String ACTION_QUERY_CONDITION = "com.twofortyfouram.locale.intent.action.QUERY_CONDITION";
    static final String ACTION_REQUEST_QUERY = "com.twofortyfouram.locale.intent.action.REQUEST_QUERY";
    static final String EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE";
    static final String EXTRA_STRING_BLURB = "com.twofortyfouram.locale.intent.extra.BLURB";
    static final String EXTRA_STRING_ACTIVITY_CLASS_NAME = "com.twofortyfouram.locale.intent.extra.ACTIVITY_CLASS_NAME";

    @Override
    public void onReceive(Context context, Intent intent) {
        // see:
        // https://github.com/twofortyfouram/android-plugin-api-for-locale
        // note: can be tested with:
        //   adb shell
        //   am broadcast \
        //      -a com.twofortyfouram.locale.intent.action.FIRE_SETTING \
        //      --es com.twofortyfouram.locale.intent.extra.BLURB "start server(s)" \
        //      -n org.primftpd/.remotecontrol.TaskerReceiverFire
        // see related logs with:
        //   adb logcat | grep -i twofortyfouram
        // leads to:
        //   BroadcastQueue: Background execution not allowed: receiving Intent

        // see derived classes
    }

    void startServer(Context context) {
        ServicesStartStopUtil.startServers(context);
    }

    void stopServer(Context context) {
        ServicesStartStopUtil.stopServers(context);
    }

    private static final String PACKAGE_NAME = TaskerReceiver.class.getPackage().getName();
    private static final String BUNDLE_EXTRA_INT_VERSION_CODE = PACKAGE_NAME + ".extra.INT_VERSION_CODE";

    public static Intent buildResultIntent(final Context context, String blurb) {
        final Intent resultIntent = new Intent();
        final Bundle resultBundle = generateBundle(context);
        logger.debug("tasker edit action result intent, blurb: {}, bundle: {}", blurb, resultBundle);
        resultIntent.putExtra(EXTRA_BUNDLE, resultBundle);
        resultIntent.putExtra(EXTRA_STRING_BLURB, blurb);
        return resultIntent;
    }
    private static Bundle generateBundle(final Context context) {
        final Bundle result = new Bundle();
        int versionCode = getVersionCode(context);
        result.putInt(BUNDLE_EXTRA_INT_VERSION_CODE, versionCode);
        logger.debug("bundle ver key: {}", BUNDLE_EXTRA_INT_VERSION_CODE);
        logger.debug("bundle ver val: {}", versionCode);
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

    public static void sendRequestQueryCondition(Context context) {
        final Intent intent = new Intent(ACTION_REQUEST_QUERY);
        intent.putExtra(EXTRA_STRING_ACTIVITY_CLASS_NAME, TaskerEditConditionActivity.class.getName());
        logger.debug("sending tasker RequestQueryCondition");
        context.sendBroadcast(intent);
    }
}
