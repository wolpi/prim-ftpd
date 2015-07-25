package org.primftpd.prefs;

import org.primftpd.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

public class AboutActivity extends Activity
{
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// set layout
		setContentView(R.layout.about);

		// show action bar to allow user to navigate back
		// -> the same as for PreferencesActivity
        getActionBar().setDisplayHomeAsUpEnabled(true);

		// show version num
        TextView versionLabel = (TextView)findViewById(R.id.versionLabel);
        versionLabel.setText("Version");

        TextView versionView = (TextView)findViewById(R.id.versionTextView);
        String pkgName = getPackageName();
        PackageManager pkgMgr = getPackageManager();
        PackageInfo packageInfo = pkgMgr.getPackageArchiveInfo(
        	pkgName,
        	0);
        String version = packageInfo != null
        	? packageInfo.versionName
        	: "unknown";
        if (packageInfo != null) {
        	version += " (" + packageInfo.versionCode + ")";
        }

        logger.debug("pkgName: '{}'", pkgName);
        logger.debug("versionName: '{}'", version);

        versionView.setText(version);
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        // navigate back -> the same as for PreferencesActivity
        switch (item.getItemId()) {
            case android.R.id.home:
            	finish();
                break;
        }
        return true;
    }
}
