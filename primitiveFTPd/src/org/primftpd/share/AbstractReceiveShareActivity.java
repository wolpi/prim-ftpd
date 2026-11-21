package org.primftpd.share;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import org.primftpd.services.AbstractServerService;
import org.primftpd.util.FilenameUnique;
import org.primftpd.util.WakelockUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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
        private boolean wakeLockWasHeld = false;
        private volatile int failedCopies = 0;

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
            // wake lock is shared with running servers, it must only be released if it has been taken here
            wakeLockWasHeld = WakelockUtil.isHeld();
            WakelockUtil.obtainWakeLock(powerMgr);

            for (int i = 0; i < uris.size(); i++) {
                final Uri uri = uris.get(i);

                String content = null;
                if (contents != null && i < contents.size()) {
                    content = contents.get(i);
                }

                logger.trace("handling uri async: {}", uri);

                // copy in this background thread, copying large files on main thread freezes the UI
                // and makes android show 'app not responding'
                try {
                    if (!activity.saveUri(targetDir, uri, content, type)) {
                        failedCopies++;
                    }
                } catch (Exception e) {
                    logger.warn("could not copy shared data", e);
                    failedCopies++;
                }
                progressDiag.incrementProgressBy(1);
            }

            return null;
        }

        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            progressDiag.dismiss();

            try {
                if (!activity.keepWakelock() && !wakeLockWasHeld) {
                    WakelockUtil.releaseWakeLock();
                }
            } catch (Exception e) {
                logger.warn("error while releasing wake lock", e);
            }

            if (failedCopies > 0) {
                // e.g. no space left for a large file, user must not think data has been saved
                try {
                    Toast.makeText(
                            activity,
                            "could not copy " + failedCopies + " of " + uris.size() + " file(s)",
                            Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    logger.warn("could not show toast", e);
                }
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

    /**
     * @return false if data could not be copied (completely)
     */
    protected boolean saveUri(TargetDir targetDir, Uri uri, String content, String type) {
        if (uri == null) {
            return true;
        }
        if (!isUriAllowed(uri)) {
            logger.warn("refusing to copy shared data, uri points to private data of this app: {}", uri);
            return false;
        }
        boolean success = false;
        OutputStream os = null;
        InputStream is = null;
        try {
            String filename = FilenameUnique.filename(uri, content, type, targetDir, this);
            logger.debug("saving with filename: {}", filename);
            os = targetDir.createOutStream(filename);
            is = getContentResolver().openInputStream(uri);
            if (os == null || is == null) {
                logger.warn("could not open streams to copy shared data, uri: {}", uri);
                return false;
            }
            copyStream(is, os);
            success = true;

        } catch (Exception e) {
            logger.warn("could not copy shared data", e);
        } finally {
            try {
                if (os != null) os.close();
            } catch (IOException e) {
                // data may be written when stream is closed
                logger.warn("could not copy shared data", e);
                success = false;
            }
            try {
                if (is != null) is.close();
            } catch (IOException e) {
                logger.warn("could not copy shared data", e);
            }
        }
        return success;
    }

    // small buffers slow down copying of large files
    /**
     * Share intents can be sent by any app. They must not be able to make this app copy its own private files
     * (e.g. host keys or preferences) to a place where others can read them or where they are served to clients.
     */
    protected boolean isUriAllowed(Uri uri) {
        try {
            String scheme = uri.getScheme();
            if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
                String authority = uri.getAuthority();
                return authority == null || !authority.startsWith(getPackageName());
            }
            if (scheme == null || ContentResolver.SCHEME_FILE.equals(scheme)) {
                String path = uri.getPath();
                if (path == null) {
                    return false;
                }
                String canonicalPath = new File(path).getCanonicalPath();
                String dataDir = new File(getApplicationInfo().dataDir).getCanonicalPath();
                if (canonicalPath.equals(dataDir) || canonicalPath.startsWith(dataDir + File.separator)) {
                    return false;
                }
                // data dir might be reachable by different paths, e.g. /data/data and /data/user/0
                return !canonicalPath.contains(File.separator + getPackageName() + File.separator)
                        || !canonicalPath.startsWith("/data/");
            }
            return true;
        } catch (Exception e) {
            logger.warn("could not check uri", e);
            return false;
        }
    }

    private static final int BUFFER_SIZE = 64 * 1024;

    private void copyStream(InputStream is, OutputStream os) throws IOException {
        // errors must be reported to caller, otherwise a truncated file looks like a successful copy
        byte[] bytes = new byte[BUFFER_SIZE];
        for (;;) {
            int count = is.read(bytes, 0, BUFFER_SIZE);
            if (count == -1) {
                break;
            }
            os.write(bytes, 0, count);
        }
    }
}
