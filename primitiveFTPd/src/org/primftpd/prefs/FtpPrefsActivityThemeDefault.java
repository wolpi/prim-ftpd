package org.primftpd.prefs;

import android.content.Intent;
import android.os.Bundle;

public class FtpPrefsActivityThemeDefault extends FtpPrefsActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // check theme and launch the other activity if necessary
        Theme theme = LoadPrefsUtil.theme(LoadPrefsUtil.getPrefs(getBaseContext()));
        if (Theme.DARK == theme) {
            this.finish();
            startActivity(new Intent(this, FtpPrefsActivityThemeDark.class));
        } else if (Theme.LIGHT == theme) {
            this.finish();
            startActivity(new Intent(this, FtpPrefsActivityThemeLight.class));
        }
    }
}
