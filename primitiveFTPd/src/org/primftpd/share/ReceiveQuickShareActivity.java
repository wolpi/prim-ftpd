package org.primftpd.share;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.Toast;

import org.primftpd.R;
import org.primftpd.util.Defaults;
import org.primftpd.util.ServicesStartStopUtil;
import org.primftpd.util.TmpDirType;

import java.io.File;
import java.util.ArrayList;

public class ReceiveQuickShareActivity extends AbstractReceiveShareActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logger.debug("onCreate()");

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        String content = intent.getStringExtra(Intent.EXTRA_TEXT);

        // when sharing URL and trying to resolve it in server we get:
        //   java.lang.SecurityException: Permission Denial: opening provider
        //   com.android.providers.media.MediaDocumentsProvider from
        //   ProcessRecord{xxx xxx:org.primftpd/xxx} (pid=xxx, uid=xxx)
        //   requires that you obtain access using ACTION_OPEN_DOCUMENT or related APIs
        // sharing in memory is not possible, as data needs to be parceled and that is
        // limited to 1 MB shared by all processes
        // -> create temp file

        File targetPath = Defaults.buildTmpDir(this, TmpDirType.QUICK_SHARE);
        logger.debug("quick share tmp path: {}", targetPath);

        if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            ArrayList<Parcelable> parcelables = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            for (Parcelable parcelable : parcelables) {
                Uri uri = (Uri) parcelable;
                logger.debug("got uri: '{}'", uri);
                saveUri(targetPath, uri, content, type);
            }
        }
        if (Intent.ACTION_SEND.equals(action)) {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            logger.debug("got uri: '{}'", uri);
            saveUri(targetPath, uri, content, type);
        }

        QuickShareBean quickShareBean = new QuickShareBean(targetPath);
        ServicesStartStopUtil.stopServers(this);
        ServicesStartStopUtil.startServers(this, quickShareBean);
        Toast.makeText(this, R.string.quickShareServerStarted, Toast.LENGTH_SHORT).show();

        finish();
    }
}
