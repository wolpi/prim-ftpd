package org.primftpd.services;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import org.primftpd.R;
import org.primftpd.prefs.LoadPrefsUtil;
import org.primftpd.util.ServersRunningBean;
import org.primftpd.util.ServicesStartStopUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * QuickSettings tile support for Android N.
 */
@TargetApi(Build.VERSION_CODES.N)
public class QuickSettingsService extends TileService {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void onCreate() {
        logger.debug("onCreate");
        super.onCreate();
    }

    @Override
    public void onClick() {
        logger.debug("onClick");
        super.onClick();
        SharedPreferences prefs = LoadPrefsUtil.getPrefs(getBaseContext());
        boolean unlockedOnly = LoadPrefsUtil.quickSettingsRequiresUnlock(prefs);

        if (unlockedOnly && isLocked()) {
            // Check whether the device is locked or not.
            if(isLocked()){
                unlockAndRun(
                    new Runnable(){
                        @Override
                        public void run() {
                            toggle();
                            updateTile();
                        }
                    }
                );
            }
        } else {
            toggle();
            updateTile();
        }
    }

    private void toggle(){
        boolean isActive = isActive();
        if (isActive) {
            // Stop service if it is already running.
            ServicesStartStopUtil.stopServers(this);
        } else {
            // Start FTP service.
            ServicesStartStopUtil.startServers(this);
        }
    }

    /**
     * Update the appearance of the tile.
     */
    protected void updateTile() {
        logger.debug("updateTile");
        Tile tile = this.getQsTile();
        boolean isActive = isActive();

        String newLabel;
        int newState;

        // Change the tile to match the service status.
        if (isActive) {
            newLabel = getString(R.string.quickSettingsServerStarted);
            newState = Tile.STATE_ACTIVE;
        } else {
            newLabel = getString(R.string.quickSettingsServerStopped);
            newState = Tile.STATE_INACTIVE;
        }

        // Change the UI of the tile.
        tile.setLabel(newLabel);
        tile.setState(newState);

        tile.updateTile();
    }

    @Override
    public void onStartListening() {
        logger.debug("onStartListening");
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onStopListening() {
        logger.debug("onStopListening");
        super.onStopListening();
        updateTile();
    }

    /**
     *
     * @return Whether we have an FTP service running.
     */
    private boolean isActive() {
        ServersRunningBean serversRunningBean = ServicesStartStopUtil.checkServicesRunning(this);

        return serversRunningBean != null && serversRunningBean.atLeastOneRunning();
    }
}