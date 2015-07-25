package org.primftpd.prefs;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

public class FtpPrefsActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // note: setting theme here does not work for PreferenceActivity
        // -> sub classes necessary

        // prefs fragment
        getFragmentManager().beginTransaction().replace(
            android.R.id.content,
            new FtpPrefsFragment()
        ).commit();

        // allow to navigate back with action bar
        // DO NOT, force user to use HW back button
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }
}
