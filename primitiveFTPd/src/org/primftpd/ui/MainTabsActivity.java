package org.primftpd.ui;

import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;

import org.primftpd.R;
import org.primftpd.prefs.FtpPrefsFragment;
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
}
