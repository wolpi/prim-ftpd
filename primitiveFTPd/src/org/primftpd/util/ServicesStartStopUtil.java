package org.primftpd.util;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.primftpd.PrefsBean;
import org.primftpd.PrimitiveFtpdActivity;
import org.primftpd.R;
import org.primftpd.StartStopWidgetProvider;
import org.primftpd.services.FtpServerService;
import org.primftpd.services.ServicesStartingService;
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
            PrimitiveFtpdActivity activity) {
        if (!isPasswordOk(prefsBean))
        {
            Toast.makeText(
                context,
                R.string.haveToSetPassword,
                Toast.LENGTH_LONG).show();

                if (activity == null) {
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
            }
        }
    }

    public static void stopServers(Context context) {
        context.stopService(createFtpServiceIntent(context, null));
        context.stopService(createSshServiceIntent(context, null));
    }

    protected static Intent createFtpServiceIntent(Context context, PrefsBean prefsBean) {
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
        if (!prefsBean.getServerToStart().isPasswordMandatory(prefsBean)) {
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

    public static void createStatusbarNotification(Context ctxt) {
        // create pending intent
        Intent notificationIntent = new Intent(ctxt, PrimitiveFtpdActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(ctxt, 0, notificationIntent, 0);

        Intent stopIntent = new Intent(ctxt, ServicesStartingService.class);
        PendingIntent pendingStopIntent = PendingIntent.getService(ctxt, 0, stopIntent, 0);

        // create notification
        int icon = R.drawable.ic_notification;
        CharSequence tickerText = ctxt.getText(R.string.serverRunning);
        CharSequence contentTitle = ctxt.getText(R.string.notificationTitle);
        CharSequence contentText = tickerText;

        // use main icon as large one
        Bitmap largeIcon = BitmapFactory.decodeResource(
                ctxt.getResources(),
                R.drawable.ic_launcher);

        long when = System.currentTimeMillis();

        Notification.Builder builder = new Notification.Builder(ctxt)
                .setTicker(tickerText)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setSmallIcon(icon)
                .setLargeIcon(largeIcon)
                .setContentIntent(contentIntent)
                .setWhen(when);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Notification.Action stopAction = new Notification.Action.Builder(
                    Icon.createWithResource("", R.drawable.ic_stop_white_24dp),
                    ctxt.getString(R.string.stopService),
                    pendingStopIntent).build();
            builder.addAction(stopAction);
        } else {
            builder.addAction(
                    R.drawable.ic_stop_white_24dp,
                    ctxt.getString(R.string.stopService),
                    pendingStopIntent);
        }
        Notification notification =builder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR;

        // notification manager
        NotificationUtil.createStatusbarNotification(ctxt, notification);
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
