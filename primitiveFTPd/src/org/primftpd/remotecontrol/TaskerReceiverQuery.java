package org.primftpd.remotecontrol;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import org.primftpd.R;
import org.primftpd.prefs.LoadPrefsUtil;
import org.primftpd.util.ServersRunningBean;
import org.primftpd.util.ServicesStartStopUtil;

public class TaskerReceiverQuery extends TaskerReceiver {

    private static final int RESULT_CONDITION_SATISFIED = 16;
    private static final int RESULT_CONDITION_UNSATISFIED = 17;

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
        if (ACTION_QUERY_CONDITION.equals(intent.getAction())) {
            TaskerCondition condition = TaskerCondition.byBlurb(blurb);
            if (condition != null) {
                ServersRunningBean runningBean = ServicesStartStopUtil.checkServicesRunning(context);
                boolean running = runningBean.atLeastOneRunning();
                if (TaskerCondition.IS_SERVER_RUNNING.equals(condition)) {
                    int conditionResult = running ? RESULT_CONDITION_SATISFIED : RESULT_CONDITION_UNSATISFIED;
                    logger.debug("got query condition with blurb: {}, setting result: {}",
                            blurb, running);
                    setResultCode(conditionResult);
                }
            }
        } else {
            logger.error("invalid action for this activity");
        }
    }
}
