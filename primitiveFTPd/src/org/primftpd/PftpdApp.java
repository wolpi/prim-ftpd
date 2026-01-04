package org.primftpd;

import android.app.Application;
import android.content.Context;

import org.primftpd.log.CsvLoggerFactory;

public class PftpdApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // init static context to be able to create log file in scoped dir
        Context context = getApplicationContext();
        CsvLoggerFactory.CONTEXT = context;
    }
}
