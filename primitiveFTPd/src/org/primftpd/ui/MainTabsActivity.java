package org.primftpd.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.tabs.TabLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.primftpd.R;
import org.primftpd.events.ServerStateChangedEvent;
import org.primftpd.prefs.FtpPrefsFragment;
import org.primftpd.util.NotificationUtil;
import org.primftpd.util.ServicesStartStopUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class MainTabsActivity extends AppCompatActivity {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected MenuItem startIcon;
    protected MenuItem stopIcon;

    protected PftpdFragment pftpdFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logger.trace("onCreate()");
        setContentView(R.layout.tabs_activity);

        TabLayout tabLayout = findViewById(R.id.tabs);
        ViewPager viewPager = findViewById(R.id.view_pager);
        tabLayout.setupWithViewPager(viewPager);

        MainAdapter adapter = new MainAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);

        this.pftpdFragment = new PftpdFragment();
        adapter.addFragment(pftpdFragment, "pftpd");
        CleanSpaceFragment cleanSpaceFragment = new CleanSpaceFragment();
        adapter.addFragment(cleanSpaceFragment, "\uD83D\uDDD1️" + getText(R.string.iconCleanSpace));
        QrFragment qrFragment = new QrFragment();
        adapter.addFragment(qrFragment, "\uD83C\uDF10 " + getText(R.string.iconQrCode));
        ClientActionFragment clientActionFragment = new ClientActionFragment();
        adapter.addFragment(clientActionFragment, "\uD83D\uDDD2️" + getText(R.string.clientActionsLabel));
        KeysFingerprintsFragment keysFingerprintsFragment = new KeysFingerprintsFragment();
        adapter.addFragment(keysFingerprintsFragment, "\uD83D\uDD11 " + getText(R.string.iconKeysFingerprints));
        FtpPrefsFragment prefsFragment = new FtpPrefsFragment();
        adapter.addFragment(prefsFragment, "⚙ " + getText(R.string.prefs));
        AboutFragment aboutFragment = new AboutFragment();
        adapter.addFragment(aboutFragment, "\uD83D\uDE4F " + getText(R.string.iconAbout));
        adapter.notifyDataSetChanged();

        // listen for events
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private class MainAdapter extends FragmentPagerAdapter {
        ArrayList<Fragment> fragments = new ArrayList<>();
        ArrayList<CharSequence> titles = new ArrayList<>();

        public void addFragment(Fragment fragment, String s) {
            fragments.add(fragment);
            titles.add(s);
        }

        public MainAdapter(FragmentManager supportFragmentManager) {
            super(supportFragmentManager);
        }

        @Override
        @NonNull
        public Fragment getItem(int position) {
            logger.trace("getItem({})", position);
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            logger.trace("getCount()");
            return fragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            logger.trace("getPageTitle({})", position);
            return titles.get(position);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        logger.debug("onCreateOptionsMenu()");

        getMenuInflater().inflate(R.menu.pftpd, menu);

        startIcon = menu.findItem(R.id.menu_start);
        stopIcon = menu.findItem(R.id.menu_stop);

        // at least required on app start
        updateButtonStates();

        return true;
    }

    protected void updateButtonStates() {
        logger.debug("updateButtonStates()");

        boolean atLeastOneRunning = ServicesStartStopUtil.checkServicesRunning(this).atLeastOneRunning();

        // remove status bar notification if server not running
        if (!atLeastOneRunning) {
            NotificationUtil.removeStatusbarNotification(this);
        }

        // action bar icons
        if (startIcon == null || stopIcon == null) {
            return;
        }

        startIcon.setVisible(!atLeastOneRunning);
        stopIcon.setVisible(atLeastOneRunning);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        logger.debug("onOptionsItemSelected()");
        Intent intent;
        switch (item.getItemId()) {
            case R.id.menu_start:
                handleStart();
                break;
            case R.id.menu_stop:
                handleStop();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void handleStart() {
        logger.trace("handleStart()");

        ServicesStartStopUtil.startServers(pftpdFragment);
    }

    protected void handleStop() {
        logger.trace("handleStop()");
        ServicesStartStopUtil.stopServers(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEvent(ServerStateChangedEvent event) {
        logger.debug("got ServerStateChangedEvent");
        updateButtonStates();
    }
}
