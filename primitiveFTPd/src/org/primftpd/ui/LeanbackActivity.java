package org.primftpd.ui;


import android.view.Menu;

public class LeanbackActivity extends MainTabsActivity {

    @Override
    protected PftpdFragment createPftpdFragment() {
        return new LeanbackFragment();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // no menu for leanback
        return true;
    }
}
