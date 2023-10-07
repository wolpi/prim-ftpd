package org.primftpd.ui;

import android.content.res.Configuration;
import android.content.res.Resources;

public class UiModeUtil {
    public static boolean isDarkMode(Resources resources) {
        int uiMode = resources.getConfiguration().uiMode;
        int nightModeFlags = uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
}
