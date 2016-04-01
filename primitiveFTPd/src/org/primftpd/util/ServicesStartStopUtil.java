package org.primftpd.util;

import android.app.ActivityManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.MenuItem;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.primftpd.PrefsBean;
import org.primftpd.PrimitiveFtpdActivity;
import org.primftpd.R;
import org.primftpd.StartStopWidgetProvider;
import org.primftpd.events.ServerStatusUpdateEvent;
import org.primftpd.services.FtpServerService;
import org.primftpd.services.SshServerService;

import java.util.List;

/**
 * Utility methods to start and stop server services.
 */
public class ServicesStartStopUtil {

    public static final String EXTRA_PREFS_BEAN = "prefs.bean";

    public static void startServers(
            Context context,
            PrefsBean prefsBean,
            PrimitiveFtpdActivity activity,
            MenuItem startIcon,
            MenuItem stopIcon) {
        if (!isPasswordOk(prefsBean))
        {
            Toast.makeText(
                    context,
                    R.string.haveToSetPassword,
                    Toast.LENGTH_SHORT).show();

            if(activity == null ){
                // Launch the main activity so that the user may set their password.
                Intent activityIntent = new Intent(context, PrimitiveFtpdActivity.class);
                activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(activityIntent);
            }
        } else {
            boolean continueServerStart = true;
            if (prefsBean.getServerToStart().startSftp()) {
                boolean keyPresent = true;
                if (activity != null) {
                    keyPresent = activity.isKeyPresent();
                    if (!keyPresent) {
                        // cannot start sftp server when key is not present
                        // ask user to generate it
                        activity.showGenKeyDialog();
                        continueServerStart = false;
                    }
                }
                if (keyPresent) {
                    context.startService(createSshServiceIntent(context, prefsBean));
                }
            }
            if (continueServerStart) {
                if (prefsBean.getServerToStart().startFtp()) {
                    context.startService(createFtpServiceIntent(context, prefsBean));
                }
                if (startIcon != null && stopIcon != null) {
                    startIcon.setVisible(false);
                    stopIcon.setVisible(true);
                } else {
                    // Post a server status update event for the activity to respond to.
                    EventBus.getDefault().post(ServerStatusUpdateEvent.STARTING);
                }
                updateWidget(context, true);
            }
        }
    }

    public static void stopServers(Context context, MenuItem startIcon, MenuItem stopIcon) {
        context.stopService(createFtpServiceIntent(context, null));
        context.stopService(createSshServiceIntent(context, null));
        if (startIcon != null && stopIcon != null) {
            startIcon.setVisible(true);
            stopIcon.setVisible(false);
        } else {
            // Post a server status update event for the activity to respond to.
            EventBus.getDefault().post(ServerStatusUpdateEvent.STOPPING);
        }
        updateWidget(context, false);
    }

    protected static  Intent createFtpServiceIntent(Context context, PrefsBean prefsBean) {
        Intent intent = new Intent(context, FtpServerService.class);
        putPrefsInIntent(intent, prefsBean);
        return intent;
    }

    protected static Intent createSshServiceIntent(Context context, PrefsBean prefsBean) {
        Intent intent = new Intent(context, SshServerService.class);
        putPrefsInIntent(intent, prefsBean);
        return intent;
    }

    protected static void putPrefsInIntent(Intent intent, PrefsBean prefsBean) {
        if (prefsBean != null) {
            intent.putExtra(EXTRA_PREFS_BEAN, prefsBean);
        }
    }

    protected static boolean isPasswordOk(PrefsBean prefsBean) {
        if (!prefsBean.getServerToStart().isPasswordMandatory()
                && prefsBean.isPubKeyAuth())
        {
            return true;
        }
        return !StringUtils.isBlank(prefsBean.getPassword());
    }

    public static ServersRunningBean checkServicesRunning(Context context) {
        ServersRunningBean serversRunning = new ServersRunningBean();
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
        String ftpServiceClassName = FtpServerService.class.getName();
        String sshServiceClassName = SshServerService.class.getName();
        for (ActivityManager.RunningServiceInfo service : runningServices) {
            String currentClassName = service.service.getClassName();
            if (ftpServiceClassName.equals(currentClassName)) {
                serversRunning.ftp = true;
            }
            if (sshServiceClassName.equals(currentClassName)) {
                serversRunning.ssh = true;
            }
            if (serversRunning.ftp && serversRunning.ssh) {
                break;
            }
        }
        return serversRunning;
    }

    public static void updateWidget(Context context, boolean running)
    {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);

        if (running) {
            remoteViews.setInt(R.id.widgetLayout,
                    "setBackgroundResource",
                    R.drawable.widget_background_enabled);
            remoteViews.setImageViewResource(
                    R.id.widgetIcon,
                    R.drawable.ic_stop_white_48dp);
            remoteViews.setTextViewText(
                    R.id.widgetText,
                    context.getText(R.string.widgetTextStop));
        } else {
            remoteViews.setInt(R.id.widgetLayout,
                    "setBackgroundResource",
                    R.drawable.widget_background_disabled);
            remoteViews.setImageViewResource(
                    R.id.widgetIcon,
                    R.drawable.ic_play_white_48dp);
            remoteViews.setTextViewText(
                    R.id.widgetText,
                    context.getText(R.string.widgetTextStart));
        }

        ComponentName thisWidget = new ComponentName(context, StartStopWidgetProvider.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        manager.updateAppWidget(thisWidget, remoteViews);
    }
}
