package org.primftpd.share;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.primftpd.R;
import org.primftpd.util.Defaults;
import org.primftpd.util.ServicesStartStopUtil;
import org.primftpd.util.TmpDirType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ReceiveQuickShareActivity extends AbstractReceiveShareActivity {

    private TargetDir targetDir;
    private List<Uri> uris = null;
    private String type;

    @Override
    protected boolean keepWakelock() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logger.debug("onCreate()");

        // EdgeToEdge on Android pre-15
        // There are some serious insets listener issues on API 28/29,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            EdgeToEdge.enable(this);
        }
        // set layout
        setContentView(R.layout.receive_share);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setNavigationBarContrastEnforced(false);
        }

        ListView listView = findViewById(R.id.list);
        ViewCompat.setOnApplyWindowInsetsListener(listView, (v, insetsCompat) -> {
            final Insets insets = insetsCompat.getInsets(WindowInsetsCompat.Type.systemBars()
                                                         | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return insetsCompat;
        });

        // read intent
        Intent intent = getIntent();
        String action = intent.getAction();
        this.type = intent.getType();
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
        this.targetDir = new TargetDir(targetPath);

        if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            ArrayList<Parcelable> parcelables = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            this.uris = new ArrayList<>(parcelables.size());
            for (Parcelable parcelable : parcelables) {
                Uri uri = (Uri) parcelable;
                logger.debug("got uri: '{}'", uri);
                uris.add(uri);
            }
            // more in onResume()
        }
        if (Intent.ACTION_SEND.equals(action)) {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            logger.debug("got uri: '{}'", uri);
            saveUri(targetDir, uri, content, type);
            onCopyFinished(targetDir);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        logger.trace("onStart()");

        if (this.uris != null) {
            // display uris
            ListView listView = findViewById(R.id.list);
            listView.setAdapter(new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_list_item_1,
                    uris
            ));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        logger.trace("onResume()");

        if (this.uris != null) {
            final ProgressDialog progressDialog = createProgressDialog(uris.size());

            // delay start of copy process to give the system time to draw progress dialog
            Handler handler = new Handler();
            handler.postDelayed(() -> {
                logger.trace("on delayed saveUris()");

                // this will show a progress dialog
                saveUris(progressDialog, targetDir, uris, null, type);
            }, 500);
        }
    }

    protected void onCopyFinished(TargetDir targetDir) {
        logger.trace("onCopyFinished()");

        QuickShareBean quickShareBean = new QuickShareBean(targetDir.getTargetPath());
        ServicesStartStopUtil.stopServers(this);
        ServicesStartStopUtil.startServers(this, quickShareBean);
        Toast.makeText(this, R.string.quickShareServerStarted, Toast.LENGTH_SHORT).show();

        this.finish();
    }
}
