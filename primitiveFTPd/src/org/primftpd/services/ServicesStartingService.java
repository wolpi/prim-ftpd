package org.primftpd.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

import org.primftpd.PrefsBean;
import org.primftpd.prefs.LoadPrefsUtil;
import org.primftpd.util.ServersRunningBean;
import org.primftpd.util.ServicesStartStopUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper class which can be called by an @{link Intent} and which handles starting and stopping of services.
 */
public class ServicesStartingService extends Service {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Context context = getBaseContext();
        ServersRunningBean serversRunningBean = ServicesStartStopUtil.checkServicesRunning(context);
        if (!serversRunningBean.atLeastOneRunning()) {
            SharedPreferences prefs = LoadPrefsUtil.getPrefs(context);
            PrefsBean prefsBean = LoadPrefsUtil.loadPrefs(logger, prefs);
            ServicesStartStopUtil.startServers(context, prefsBean, null, null, null);
        } else {
            ServicesStartStopUtil.stopServers(context, null, null);
        }

        return 0;
    }

    @Override
    public void onDestroy() {
    }
}
