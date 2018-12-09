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
    private static final String ACTION_FIRE_SETTING = "com.twofortyfouram.locale.intent.action.FIRE_SETTING";
    private static final String ACTION_QUERY_CONDITION = "com.twofortyfouram.locale.intent.action.QUERY_CONDITION";
    private static final String ACTION_REQUEST_QUERY = "com.twofortyfouram.locale.intent.action.REQUEST_QUERY";
    private static final String EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE";
    private static final String EXTRA_STRING_BLURB = "com.twofortyfouram.locale.intent.extra.BLURB";
    private static final String EXTRA_STRING_ACTIVITY_CLASS_NAME = "com.twofortyfouram.locale.intent.extra.ACTIVITY_CLASS_NAME";
    private static final int RESULT_CONDITION_SATISFIED = 16;
    private static final int RESULT_CONDITION_UNSATISFIED = 17;

    @Override
    public void onReceive(Context context, Intent intent) {
        // note: can be tested with:
        // adb shell
        // am broadcast -a com.twofortyfouram.locale.intent.action.FIRE_SETTING --es com.twofortyfouram.locale.intent.extra.BLURB "start server(s)"

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
        } else if (ACTION_QUERY_CONDITION.equals(intent.getAction())) {
            TaskerCondition condition = TaskerCondition.byBlurb(blurb);
            if (condition != null) {
                ServersRunningBean runningBean = ServicesStartStopUtil.checkServicesRunning(context);
                boolean running = runningBean.atLeastOneRunning();
                switch (condition) {
                    case IS_SERVER_RUNNING:
                        int conditionResult = running ? RESULT_CONDITION_SATISFIED : RESULT_CONDITION_UNSATISFIED;
                        logger.debug("got query condition with blurb: {}, setting result: {}",
                                blurb, Boolean.valueOf(running));
                        setResultCode(conditionResult);
                        break;
                }
            }
        }
    }

    private void startServer(Context context) {
        ServicesStartStopUtil.startServers(context);
    }

    private void stopServer(Context context) {
        ServicesStartStopUtil.stopServers(context);
    }

    private static final String PACKAGE_NAME = TaskerReceiver.class.getPackage().getName();
    private static final String BUNDLE_EXTRA_INT_VERSION_CODE = PACKAGE_NAME + ".extra.INT_VERSION_CODE";

    public static Intent buildResultIntent(final Context context, String blurb) {
        final Intent resultIntent = new Intent();
        final Bundle resultBundle = generateBundle(context);
        resultIntent.putExtra(EXTRA_BUNDLE, resultBundle);
        resultIntent.putExtra(EXTRA_STRING_BLURB, blurb);
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

    public static void sendRequestQueryCondition(Context context) {
        final Intent intent = new Intent(ACTION_REQUEST_QUERY);
        intent.putExtra(EXTRA_STRING_ACTIVITY_CLASS_NAME, TaskerEditConditionActivity.class.getName());
        logger.debug("sending tasker RequestQueryCondition");
        context.sendBroadcast(intent);
    }
}
