package org.primftpd.util;

import android.os.PowerManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WakelockUtil {

    private static final Logger logger = LoggerFactory.getLogger(WakelockUtil.class);

    public static final String APP_NAME = "pFTPd";

    private static final long TIMEOUT_MILLIS = 60*60*1000L; // 60 minutes

    private static PowerManager.WakeLock wakeLock;

    public static synchronized void obtainWakeLock(PowerManager powerMgr) {
        if (wakeLock == null) {
            logger.debug("acquiring wake lock");
            // note: PARTIAL_WAKE_LOCK is not enough
            wakeLock = powerMgr.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    APP_NAME + ":wakelock");
            // timeout is renewed by renewWakeLock(), without ref count releasing once is sufficient
            wakeLock.setReferenceCounted(false);
            wakeLock.acquire(TIMEOUT_MILLIS);
        } else {
            logger.debug("wake lock already taken");
        }
    }

    /**
     * Restarts timeout of wake lock if it has been obtained.
     * To be called while data is transferred, as transfer of large files may take longer than timeout.
     */
    public static synchronized void renewWakeLock() {
        if (wakeLock != null) {
            try {
                wakeLock.acquire(TIMEOUT_MILLIS);
                logger.debug("renewed wake lock");
            } catch (Exception e) {
                logger.warn("error while renewing wake lock", e);
            }
        }
    }

    /**
     * @return true if wake lock is held, e.g. by a running server.
     */
    public static synchronized boolean isHeld() {
        try {
            return wakeLock != null && wakeLock.isHeld();
        } catch (Exception e) {
            return false;
        }
    }

    public static synchronized void releaseWakeLock() {
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
