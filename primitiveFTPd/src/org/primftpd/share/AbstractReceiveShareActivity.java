package org.primftpd.share;

import android.app.ProgressDialog;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;

import org.primftpd.util.FilenameUnique;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import androidx.fragment.app.FragmentActivity;

public abstract class AbstractReceiveShareActivity extends FragmentActivity {

    protected Logger logger = LoggerFactory.getLogger(getClass());

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
            // showing progress dialog must not be done in main thread
            // accessing shared uris is possible in main thread only
            // thus we need to post messages between thready and synchronize progress

            final Integer[] progressSync = new Integer[1];
            progressSync[0] = 0;

            for (int i = 0; i < uris.size(); i++) {
                final Uri uri = uris.get(i);

                String content = null;
                if (contents != null && i < contents.size()) {
                    content = contents.get(i);
                }

                logger.trace("handling uri async: {}", uri);

                final String finalContent = content;
                final int progress = i;
                mainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        logger.trace("saving uri in main thread");

                        activity.saveUri(targetDir, uri, finalContent, type);

                        logger.trace("setting progressSync to: {}", progress);
                        progressSync[0] = progress;
                    }
                });
            }

            while (progressSync[0] < uris.size() -1) {
                if (progressSync[0] != progressDiag.getProgress()) {
                    logger.trace("setting progress to: {}", progressSync[0]);
                    progressDiag.setProgress(progressSync[0]);
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    logger.error("error while waiting for progress", e);
                }
            }

            return null;
        }

        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            progressDiag.dismiss();

            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    activity.onCopyFinished(targetDir);
                }
            });
        }
    };

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
