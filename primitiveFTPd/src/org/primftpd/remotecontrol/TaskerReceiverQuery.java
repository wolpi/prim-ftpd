package org.primftpd.remotecontrol;

import android.content.Context;
import android.content.Intent;

import org.primftpd.util.ServersRunningBean;
import org.primftpd.util.ServicesStartStopUtil;

public class TaskerReceiverQuery extends TaskerReceiver {

    private static final int RESULT_CONDITION_SATISFIED = 16;
    private static final int RESULT_CONDITION_UNSATISFIED = 17;

    public void onReceive(Context context, Intent intent) {
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
