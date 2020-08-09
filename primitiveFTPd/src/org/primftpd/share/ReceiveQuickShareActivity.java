package org.primftpd.share;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import org.primftpd.R;
import org.primftpd.util.Defaults;
import org.primftpd.util.ServicesStartStopUtil;

import java.io.File;
import java.util.UUID;

public class ReceiveQuickShareActivity extends AbstractReceiveShareActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logger.debug("onCreate()");

        boolean serverStarted = false;
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        String content = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (Intent.ACTION_SEND.equals(action)) {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            logger.debug("got uri: '{}'", uri);
            if (uri != null) {

                // when sharing URL and trying to resolve it in server we get:
                //   java.lang.SecurityException: Permission Denial: opening provider
                //   com.android.providers.media.MediaDocumentsProvider from
                //   ProcessRecord{xxx xxx:org.primftpd/xxx} (pid=xxx, uid=xxx)
                //   requires that you obtain access using ACTION_OPEN_DOCUMENT or related APIs
                // sharing in memory is not possible, as data needs to be parceled and that is
                // limited to 1 MB shared by all processes
                // -> create temp file

                String pathToFile = copyToTmpFile(uri, content, type);
                QuickShareBean quickShareBean = new QuickShareBean(pathToFile);
                ServicesStartStopUtil.stopServers(this);
                ServicesStartStopUtil.startServers(this, quickShareBean);
                serverStarted = true;
                Toast.makeText(this, R.string.quickShareServerStarted, Toast.LENGTH_SHORT).show();
            }
        }
        if (!serverStarted) {
            logger.info("could not start server to quick share. intent action: '{}', intent: '{}'", action, intent);
            Toast.makeText(this, R.string.quickShareCouldNotStartServer, Toast.LENGTH_LONG).show();
        }
        finish();
    }

    protected String copyToTmpFile(Uri uri, String content, String type) {
        File quickShareTmpDir = Defaults.quickShareTmpDir(this);
        quickShareTmpDir.mkdir();
        UUID uuid = UUID.randomUUID();
        File targetPath = new File(quickShareTmpDir, uuid.toString());
        targetPath.mkdir();
        logger.debug("quick share tmp path: {}", targetPath);
        return saveUri(targetPath, uri, content, type);
    }
}
