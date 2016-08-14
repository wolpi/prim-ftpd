package org.primftpd.remotecontrol;

import org.primftpd.R;

import java.util.HashMap;
import java.util.Map;

public enum TaskerCondition {
    IS_SERVER_RUNNING(R.string.isServerRunning, "server running?");

    final private int stringId;
    final private String blurb;

    private TaskerCondition(int stringId, String blurb) {
        this.stringId = stringId;
        this.blurb = blurb;
    }

    public int getStringId() {
        return stringId;
    }

    public String getBlurb() {
        return blurb;
    }

    private static Map<String, TaskerCondition> BLURB_TO_CONDITION;
    static {
        BLURB_TO_CONDITION = new HashMap<>();
        for (TaskerCondition condition : values()) {
            BLURB_TO_CONDITION.put(condition.blurb, condition);
        }
    }
    public static TaskerCondition byBlurb(String blurb) {
        return BLURB_TO_CONDITION.get(blurb);
    }
}
