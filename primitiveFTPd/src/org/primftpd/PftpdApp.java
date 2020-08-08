package org.primftpd;

import android.content.Context;

import org.primftpd.log.CsvLoggerFactory;

import androidx.multidex.MultiDexApplication;

public class PftpdApp extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();

        // init static context to be able to create log file in scoped dir
        Context context = getApplicationContext();
        CsvLoggerFactory.CONTEXT = context;
    }
}
