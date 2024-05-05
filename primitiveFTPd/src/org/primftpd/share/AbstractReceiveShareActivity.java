package org.primftpd.share;

import android.app.ProgressDialog;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import org.primftpd.services.AbstractServerService;
import org.primftpd.util.FilenameUnique;
import org.primftpd.util.WakelockUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

public abstract class AbstractReceiveShareActivity extends FragmentActivity {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // if screen orientation changes and copy-dialog is currently shown
        // the dialog will be destroyed
        // to avoid that we request a specific orientation to prevent it being changed
        // check first if we have a landscape or portrait like screen
        // re-set after copy is finished
        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        boolean isWideScreen = metrics.heightPixels < metrics.widthPixels;
        int requestedOrientation = isWideScreen
                ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        setRequestedOrientation(requestedOrientation);
    }

    protected void saveUris(
            ProgressDialog progressDialog,
            final TargetDir targetDir,
            final List<Uri> uris,
            final List<String> contents,
            final String type) {
        if (uris == null) {
            return;
        }

        final Handler mainThreadHandler = new Handler();

        AsyncTask<Void, Void, Void> task = new CopyTask(
                this,
                progressDialog,
                mainThreadHandler,
                targetDir,
                uris,
                contents,
                type);
        task.execute();
    }

    protected ProgressDialog createProgressDialog(int maxProgress) {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMax(maxProgress);
        progressDialog.setMessage("copy ...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

        progressDialog.show();

        return progressDialog;
    }

    protected abstract boolean keepWakelock();

    private static class CopyTask extends AsyncTask<Void, Void, Void> {

        protected Logger logger = LoggerFactory.getLogger(getClass());

        private final AbstractReceiveShareActivity activity;
        private final ProgressDialog progressDiag;
        private final Handler mainThreadHandler;
        private final TargetDir targetDir;
        private final List<Uri> uris;
        private final List<String> contents;
        private final String type;

        CopyTask(AbstractReceiveShareActivity activity,
                 ProgressDialog progressDiag,
                 Handler mainThreadHandler,
                 TargetDir targetDir,
                 List<Uri> uris,
                 List<String> contents,
                 String type) {
            super();
            this.activity = activity;
            this.progressDiag = progressDiag;
            this.mainThreadHandler = mainThreadHandler;
            this.targetDir = targetDir;
            this.uris = uris;
            this.contents = contents;
            this.type = type;
        }

        @Override
        protected java.lang.Void doInBackground(java.lang.Void[] objects) {

            // acquire wake lock to make sure copy finishes before going to sleep
            logger.debug("acquiring wake lock");
            PowerManager powerMgr =
                    (PowerManager) activity.getSystemService(
                            AbstractServerService.POWER_SERVICE);
            WakelockUtil.obtainWakeLock(powerMgr);

            for (int i = 0; i < uris.size(); i++) {
                final Uri uri = uris.get(i);

                String content = null;
                if (contents != null && i < contents.size()) {
                    content = contents.get(i);
                }

                logger.trace("handling uri async: {}", uri);

                boolean[] finished = new boolean[]{false};
                final String finalContent = content;
                mainThreadHandler.post(() -> {
                    logger.trace("saving uri in main thread");
                    activity.saveUri(targetDir, uri, finalContent, type);
                    finished[0] = true;
                });

                while (!finished[0]) {
                    try {
                        // wait
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        logger.error("", e);
                    }
                }

                // progress mus be updated in background task, not in main thread
                logger.trace("incrementing progress in background task");
                progressDiag.incrementProgressBy(1);
            }

            return null;
        }

        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            progressDiag.dismiss();

            try {
                if (!activity.keepWakelock()) {
                    WakelockUtil.releaseWakeLock();
                }
            } catch (Exception e) {
                logger.warn("error while releasing wake lock", e);
            }

            mainThreadHandler.post(() -> {
                activity.onCopyFinished(targetDir);
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            });
        }
    }

    protected void onCopyFinished(TargetDir targetDir) {
        this.finish();
    }

    protected void saveUri(TargetDir targetDir, Uri uri, String content, String type) {
        if (uri == null) {
            return;
        }
        OutputStream os = null;
        InputStream is = null;
        try {
            String filename = FilenameUnique.filename(uri, content, type, targetDir, this);
            logger.debug("saving with filename: {}", filename);
            os = targetDir.createOutStream(filename);
            is = getContentResolver().openInputStream(uri);
            copyStream(is, os);

        } catch (Exception e) {
            logger.warn("could not copy shared data", e);
        } finally {
            try {
                if (os != null) os.close();
                if (is != null) is.close();
            } catch (IOException e) {
                logger.warn("could not copy shared data", e);
            }
        }
    }

    private static final int BUFFER_SIZE = 4096;

    private void copyStream(InputStream is, OutputStream os) {
        try {
            byte[] bytes = new byte[BUFFER_SIZE];
            for (;;) {
                int count = is.read(bytes, 0, BUFFER_SIZE);
                if (count == -1) {
                    break;
                }
                os.write(bytes, 0, count);
            }
        } catch (Exception e) {
            logger.warn("could not copy stream", e);
        }
    }
}
