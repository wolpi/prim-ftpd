package org.primftpd.util;

import android.app.ActivityManager;
import android.app.Notification;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.primftpd.prefs.PrefsBean;
import org.primftpd.R;
import org.primftpd.share.QuickShareBean;
import org.primftpd.ui.GenKeysAskDialogFragment;
import org.primftpd.ui.MainTabsActivity;
import org.primftpd.ui.PftpdFragment;
import org.primftpd.ui.StartServerAndExitActivity;
import org.primftpd.StartStopWidgetProvider;
import org.primftpd.prefs.LoadPrefsUtil;
import org.primftpd.remotecontrol.PftpdPowerTogglesPlugin;
import org.primftpd.remotecontrol.TaskerReceiver;
import org.primftpd.services.FtpServerService;
import org.primftpd.services.SshServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Utility methods to start and stop server services.
 */
public class ServicesStartStopUtil {

    public static final String EXTRA_PREFS_BEAN = "prefs.bean";
    public static final String EXTRA_FINGERPRINT_PROVIDER = "fingerprint.provider";
    public static final String EXTRA_QUICK_SHARE_BEAN = "quick.share.bean";
    public static final String EXTRA_CHOSEN_IP = "chosen.ip";

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicesStartStopUtil.class);

    public static void startServers(PftpdFragment fragment) {
        startServers(null, null, null, fragment, null);
    }
    public static void startServers(Context context) {
        startServers(context, null, null, null, null);
    }

    public static void startServers(Context context, QuickShareBean quickShareBean) {
        startServers(context, null, null, null, quickShareBean);
    }

    private static void startServers(
            Context context,
            PrefsBean prefsBean,
            KeyFingerprintProvider keyFingerprintProvider,
            PftpdFragment fragment,
            QuickShareBean quickShareBean) {
        LOGGER.trace("startServers()");

        if (context == null && fragment != null) {
            context = fragment.getContext();
        }
        if (context == null) {
            LOGGER.error("context is null, not starting server");
            return;
        }

        if (prefsBean == null) {
            if (fragment != null) {
                prefsBean = fragment.getPrefsBean();
            }
            if (prefsBean == null) {
                SharedPreferences prefs = LoadPrefsUtil.getPrefs(context);
                prefsBean = LoadPrefsUtil.loadPrefs(LOGGER, prefs);
            }
        }
        if (keyFingerprintProvider == null) {
            if (fragment != null) {
                keyFingerprintProvider = fragment.getKeyFingerprintProvider();
            }
            if (keyFingerprintProvider == null) {
                keyFingerprintProvider = new KeyFingerprintProvider();
            }
        }
        String chosenIp = fragment != null ? fragment.getChosenIp() : null;

        if (!isPasswordOk(prefsBean)) {
            Toast.makeText(
                context,
                R.string.haveToSetAuthMechanism,
                Toast.LENGTH_LONG).show();

            if (fragment == null) {
                // Launch the main activity so that the user may set their password.
                Intent activityIntent = new Intent(context, MainTabsActivity.class);
                activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(activityIntent);
            }
        } else {
            boolean continueServerStart = true;
            if (prefsBean.getServerToStart().startSftp()) {
                boolean keyPresent = true;
                if (fragment != null) {
                    keyPresent = isKeyPresent(keyFingerprintProvider, context);
                    if (!keyPresent) {
                        // cannot start sftp server when key is not present
                        // ask user to generate it
                        showGenKeyDialog(fragment);
                        continueServerStart = false;
                    }
                }
                if (keyPresent) {
                    LOGGER.debug("going to start sshd");
                    try {
                        Intent intent = createSshServiceIntent(
                                context,
                                prefsBean,
                                keyFingerprintProvider,
                                quickShareBean,
                                chosenIp);
                        startServerByIntent(intent, context);
                    } catch (Exception e) {
                        LOGGER.error("could not start sftp server", e);
                        Toast.makeText(
                                context,
                                "could not start sftp server, " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
            if (continueServerStart) {
                if (prefsBean.getServerToStart().startFtp()) {
                    LOGGER.debug("going to start ftpd");
                    try {
                        Intent intent = createFtpServiceIntent(
                                context,
                                prefsBean,
                                keyFingerprintProvider,
                                quickShareBean,
                                chosenIp);
                        startServerByIntent(intent, context);
                    } catch (Exception e) {
                        LOGGER.error("could not start ftp server", e);
                        Toast.makeText(
                                context,
                                "could not start ftp server, " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }


    private static boolean isKeyPresent(KeyFingerprintProvider keyFingerprintProvider, Context context) {
        if (keyFingerprintProvider != null && context != null) {
            if (!keyFingerprintProvider.areFingerprintsGenerated()) {
                LOGGER.debug("checking if key is present, but fingerprints have not been generated yet");
                keyFingerprintProvider.calcPubkeyFingerprints(context);
            }
            boolean keyPresent = keyFingerprintProvider.isKeyPresent();
            LOGGER.trace("isKeyPresent() -> {}", keyPresent);
            return keyPresent;
        } else {
            LOGGER.debug("params are null, cannot check for key");
            return false;
        }
    }

    private static void showGenKeyDialog(PftpdFragment fragment) {
        LOGGER.trace("showGenKeyDialog()");
        GenKeysAskDialogFragment askDiag = new GenKeysAskDialogFragment(fragment);
        Bundle diagArgs = new Bundle();
        diagArgs.putBoolean(GenKeysAskDialogFragment.KEY_START_SERVER, true);
        askDiag.setArguments(diagArgs);
        askDiag.show(fragment.requireActivity().getSupportFragmentManager(), PftpdFragment.DIALOG_TAG);
    }

    private static void startServerByIntent(Intent intent, Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception e) {
            LOGGER.error("could not start server, using workaround with activity", e);
            Intent activityIntent = new Intent(context, StartServerAndExitActivity.class);
            activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(activityIntent);
        }
    }

    public static void stopServers(Context context) {
        LOGGER.trace("stopServers()");
        context.stopService(createFtpServiceIntent(context, null, null, null, null));
        context.stopService(createSshServiceIntent(context, null, null, null, null));
    }

    protected static Intent createFtpServiceIntent(
            Context context,
            PrefsBean prefsBean,
            KeyFingerprintProvider keyFingerprintProvider,
            QuickShareBean quickShareBean,
            String chosenIp) {
        Intent intent = new Intent(context, FtpServerService.class);
        putPrefsInIntent(intent, prefsBean);
        putKeyFingerprintProviderInIntent(intent, keyFingerprintProvider);
        putQuickShareBeanInIntent(intent, quickShareBean);
        putChosenIpInIntent(intent, chosenIp);
        return intent;
    }

    protected static Intent createSshServiceIntent(
            Context context,
            PrefsBean prefsBean,
            KeyFingerprintProvider keyFingerprintProvider,
            QuickShareBean quickShareBean,
            String chosenIp) {
        Intent intent = new Intent(context, SshServerService.class);
        putPrefsInIntent(intent, prefsBean);
        putKeyFingerprintProviderInIntent(intent, keyFingerprintProvider);
        putQuickShareBeanInIntent(intent, quickShareBean);
        putChosenIpInIntent(intent, chosenIp);
        return intent;
    }

    protected static void putPrefsInIntent(Intent intent, PrefsBean prefsBean) {
        if (prefsBean != null) {
            intent.putExtra(EXTRA_PREFS_BEAN, prefsBean);
        }
    }

    protected static void putKeyFingerprintProviderInIntent(Intent intent, KeyFingerprintProvider keyFingerprintProvider) {
        if (keyFingerprintProvider != null) {
            intent.putExtra(EXTRA_FINGERPRINT_PROVIDER, keyFingerprintProvider);
        }
    }

    protected static void putQuickShareBeanInIntent(Intent intent, QuickShareBean quickShareBean) {
        if (quickShareBean != null) {
            intent.putExtra(EXTRA_QUICK_SHARE_BEAN, quickShareBean);
        }
    }

    protected static void putChosenIpInIntent(Intent intent, String chosenIp) {
        if (chosenIp != null) {
            intent.putExtra(EXTRA_CHOSEN_IP, chosenIp);
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

    private static void updateWidget(Context context, boolean running)
    {
        LOGGER.debug("updateWidget()");
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

    public static Notification updateNonActivityUI(
            Context ctxt,
            boolean serverRunning,
            PrefsBean prefsBean,
            KeyFingerprintProvider keyFingerprintProvider,
            QuickShareBean quickShareBean,
            String chosenIp) {
        LOGGER.trace("updateNonActivityUI()");
        Notification notification = null;
        updateWidget(ctxt, serverRunning);
        if (serverRunning) {
            notification = NotificationUtil.createStatusbarNotification(
                    ctxt,
                    prefsBean,
                    keyFingerprintProvider,
                    quickShareBean,
                    chosenIp);
        } else {
            LOGGER.debug("removeStatusbarNotification()");
            NotificationUtil.removeStatusbarNotification(ctxt);
        }
        new PftpdPowerTogglesPlugin().sendStateUpdate(ctxt, serverRunning);
        TaskerReceiver.sendRequestQueryCondition(ctxt);
        return notification;
    }
}
