package org.primftpd.remotecontrol;

import android.content.Context;

import org.primftpd.util.ServicesStartStopUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PftpdPowerTogglesPlugin extends PowerTogglesPlugin {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    protected void changeState(Context context, boolean newState) {
        logger.trace("changeState()");
        if (newState) {
            ServicesStartStopUtil.startServers(context);
        } else {
            ServicesStartStopUtil.stopServers(context);
        }
        sendStateUpdate(context, newState);
    }
}
