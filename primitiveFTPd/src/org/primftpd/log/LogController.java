package org.primftpd.log;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;

import org.primftpd.prefs.LoadPrefsUtil;
import org.primftpd.prefs.Logging;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.CoreConstants;

public final class LogController {

    // names must match what is set in assets/logback.xml
    public static final String LOGFILE_BASENAME = "prim-ftpd-log";
    private static final String TAG = "LogController";
    private static final String CSV_FILE = "CSV_FILE";
    private static final String LOGCAT = "LOGCAT";
    private static final String LOGBACK_XML = "logback.xml";

    /** Current/default. */
    private static Logging logging = Logging.NONE;

    private LogController() {
    }

    /**
     * Must be called from {@link Application#onCreate()}.
     *
     * @param context Application context
     */
    public static void init(@NonNull Context context) {
        final Logging p = readPrefs(context);
        setActiveConfig(context, p);
    }

    public static Logging readPrefs(@NonNull Context context) {
        SharedPreferences prefs = LoadPrefsUtil.getPrefs(context);
        String loggingStr = prefs.getString(
                LoadPrefsUtil.PREF_KEY_LOGGING,
                Logging.NONE.xmlValue());
        return Logging.byXmlVal(loggingStr);
    }

    @NonNull
    public static Logging getActiveConfig() {
        return logging;
    }

    public static void setActiveConfig(@NonNull Context context,
                                       @NonNull Logging logging) {
        LogController.logging = logging;

        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        // do NOT get the root logger before we've done the reconfigure!
        switch (logging) {
            case NONE: {
                // no reconfig needed, just switch it all off.
                Logger root = lc.getLogger(Logger.ROOT_LOGGER_NAME);
                root.setLevel(Level.OFF);
                root.detachAndStopAllAppenders();
                break;
            }
            case ANDROID: {
                reconfigure(context, lc);
                Logger root = lc.getLogger(Logger.ROOT_LOGGER_NAME);
                root.detachAppender(CSV_FILE);
                root.setLevel(Level.DEBUG);
                break;
            }
            case TEXT: {
                reconfigure(context, lc);
                Logger root = lc.getLogger(Logger.ROOT_LOGGER_NAME);
                root.detachAppender(LOGCAT);
                root.setLevel(Level.DEBUG);
                break;
            }
        }
    }

    private static void reconfigure(@NonNull Context context,
                                    @NonNull LoggerContext lc) {
        // stop/drop/amnesia....
        lc.reset();
        // There is no other path which is valid in all circumstances,
        // so we hard code it here instead of using homeDirScoped
        // as an explicit reminder.
        // Also note that logback-android contains a bug where it
        // wrongly assumes that method only to be available from API-29 onwards.
        // But it's in API-28 ! Exactly what we need...
        File dir = context.getExternalFilesDir(null);
        if (dir == null) {
            // as a desperate fallback... at least we won't crash.
            dir = context.getFilesDir();
        }
        lc.putProperty(CoreConstants.EXT_DIR_KEY, dir.getAbsolutePath());
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(lc);

        try {
            configurator.doConfigure(context.getAssets().open(LOGBACK_XML));
        } catch (Exception e) {
            Log.e(TAG, "LOGBACK_XML", e);
        }
    }
}
