package org.primftpd.share;

import android.app.ProgressDialog;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;

import org.primftpd.util.FilenameUnique;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import androidx.fragment.app.FragmentActivity;

public abstract class AbstractReceiveShareActivity extends FragmentActivity {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    protected void saveUris(
            ProgressDialog progressDialog,
            final File targetPath,
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
                targetPath,
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
        private final File targetPath;
        private final List<Uri> uris;
        private final List<String> contents;
        private final String type;

        CopyTask(AbstractReceiveShareActivity activity,
                 ProgressDialog progressDiag,
                 Handler mainThreadHandler,
                 File targetPath,
                 List<Uri> uris,
                 List<String> contents,
                 String type) {
            this.activity = activity;
            this.progressDiag = progressDiag;
            this.mainThreadHandler = mainThreadHandler;
            this.targetPath = targetPath;
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

                        activity.saveUri(targetPath, uri, finalContent, type);

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
                    activity.onCopyFinished(targetPath);
                }
            });
        }
    };

    protected void onCopyFinished(File targetPath) {
        this.finish();
    }

    protected String saveUri(File targetPath, Uri uri, String content, String type) {
        if (uri == null) {
            return "";
        }
        FileOutputStream fos = null;
        InputStream is = null;
        try {
            File targetFile = targetFile(uri, content, type, targetPath);
            logger.debug("saving under: {}", targetFile);
            fos = new FileOutputStream(targetFile);
            is = getContentResolver().openInputStream(uri);
            copyStream(is, fos);
            return targetFile.getAbsolutePath();
        } catch (Exception e) {
            logger.warn("could not copy shared data", e);
        } finally {
            try {
                if (fos != null) fos.close();
                if (is != null) is.close();
            } catch (IOException e) {
                logger.warn("could not copy shared data", e);
            }
        }
        return "";
    }

    protected File targetFile(Uri uri, String content, String type, File targetPath) {
        if (!targetPath.isFile()) {
            String filename = FilenameUnique.filename(uri, content, type, targetPath, this);
            return new File(targetPath, filename);
        } else {
            return targetPath;
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
