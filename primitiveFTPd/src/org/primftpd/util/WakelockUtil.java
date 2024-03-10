package org.primftpd.util;

import android.os.PowerManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WakelockUtil {

    private static final Logger logger = LoggerFactory.getLogger(WakelockUtil.class);

    public static final String APP_NAME = "pFTPd";

    private static PowerManager.WakeLock wakeLock;

    public static void obtainWakeLock(PowerManager powerMgr) {
        if (wakeLock == null) {
            logger.debug("acquiring wake lock");
            // note: PARTIAL_WAKE_LOCK is not enough
            wakeLock = powerMgr.newWakeLock(
                    PowerManager.SCREEN_DIM_WAKE_LOCK,
                    APP_NAME + ":wakelock");
            wakeLock.acquire(60*60*1000L /*60 minutes*/);
        } else {
            logger.debug("wake lock already taken");
        }
    }

    public static void releaseWakeLock() {
        if (wakeLock != null) {
            if (wakeLock.isHeld()) {
                logger.debug("releasing wake lock");
                try {
                    wakeLock.release();
                } catch (Exception e) {
                    logger.warn("error while releasing wake lock", e);
                }
            } else {
                logger.debug("wake lock not held, not releasing it");
            }
            wakeLock = null;
        } else {
            logger.debug("wake lock already released");
        }
    }
}
