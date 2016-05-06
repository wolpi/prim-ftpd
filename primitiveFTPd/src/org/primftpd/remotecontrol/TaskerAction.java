package org.primftpd.remotecontrol;

import org.primftpd.R;

import java.util.HashMap;
import java.util.Map;

public enum TaskerAction {
    START(R.string.startService, "start server(s)"),
    STOP(R.string.stopService, "stop server(s)"),
    TOGGLE(R.string.toggleService, "toggle server(s)");

    final private int stringId;
    final private String blurb;

    private TaskerAction(int stringId, String blurb) {
        this.stringId = stringId;
        this.blurb = blurb;
    }

    public int getStringId() {
        return stringId;
    }

    public String getBlurb() {
        return blurb;
    }

    private static Map<String, TaskerAction> BLURB_TO_ACTION;
    static {
        BLURB_TO_ACTION = new HashMap<>();
        for (TaskerAction action : values()) {
            BLURB_TO_ACTION.put(action.blurb, action);
        }
    }
    public static TaskerAction byBlurb(String blurb) {
        return BLURB_TO_ACTION.get(blurb);
    }
}
