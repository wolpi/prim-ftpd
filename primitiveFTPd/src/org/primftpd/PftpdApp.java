package org.primftpd;

import android.app.Application;

import org.primftpd.log.LogController;
import java.security.Security;

public class PftpdApp extends Application {

    static {
        Security.removeProvider("BC");
        Security.insertProviderAt(new org.bouncycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        LogController.init(getApplicationContext());
    }
}
