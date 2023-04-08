package org.primftpd.ui;

import android.app.Activity;
import android.app.UiModeManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import org.primftpd.prefs.LoadPrefsUtil;
import org.primftpd.prefs.Theme;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeUtil {
    public static Theme applyTheme(Activity activity, SharedPreferences prefs) {
        Theme theme = LoadPrefsUtil.theme(prefs);
        activity.setTheme(theme.resourceId());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            UiModeManager uiModeManager = (UiModeManager)activity.getSystemService(Context.UI_MODE_SERVICE);
            uiModeManager.setApplicationNightMode(theme.getUiModeValue());
        } else {
            AppCompatDelegate.setDefaultNightMode(theme.getAppCompatValue());
        }
        return theme;
    }
}
