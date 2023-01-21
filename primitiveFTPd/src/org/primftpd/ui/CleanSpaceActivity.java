package org.primftpd.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.primftpd.R;
import org.primftpd.prefs.LoadPrefsUtil;
import org.primftpd.prefs.Theme;
import org.primftpd.util.Defaults;
import org.primftpd.util.FileSizeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class CleanSpaceActivity extends Activity {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private TextView quickShareSpaceTextView;
    private TextView logsSpaceTextView;
    private TextView rootTmpSpaceTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        logger.trace("onCreate()");

        SharedPreferences prefs = LoadPrefsUtil.getPrefs(getBaseContext());
        Theme theme = LoadPrefsUtil.theme(prefs);
        setTheme(theme.resourceId());
        setContentView(R.layout.clean_space);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        // store references
        quickShareSpaceTextView = findViewById(R.id.quickShareFilesSize);
        logsSpaceTextView = findViewById(R.id.logFilesSize);
        rootTmpSpaceTextView = findViewById(R.id.rootTmpFilesSize);

        // register listeners
        final CleanSpaceActivity activity = this;
        findViewById(R.id.quickShareFilesDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onButtonClick(activity, quickShareDir(), true);
            }
        });
        findViewById(R.id.logFilesDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onButtonClick(activity, logsDir(), false);
            }
        });
        findViewById(R.id.rootTmpFilesDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onButtonClick(activity, rootTmpDir(), true);
            }
        });
    }

    private void onButtonClick(CleanSpaceActivity activity, File dir, boolean includeChildren) {
        int numberOfFiles = collectNumberOfFiles(dir, includeChildren);
        if (numberOfFiles > 0) {
            createProgressDialog(numberOfFiles);
            ProgressDialog progressDialog = createProgressDialog(numberOfFiles);
            DeleteTask deleteTask = new DeleteTask(activity, progressDialog, dir, includeChildren);
            deleteTask.execute();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        logger.trace("onResume()");

        updateView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        // navigate back -> the same as for PreferencesActivity
        if (android.R.id.home == item.getItemId()) {
            finish();
        }
        return true;
    }

    protected File quickShareDir() {
        return Defaults.quickShareTmpDir(this);
    }

    protected File logsDir() {
        return Defaults.homeDirScoped(this);
    }

    protected File rootTmpDir() {
        return Defaults.rootCopyTmpDir(this);
    }

    protected void updateView() {
        quickShareSpaceTextView.setText(FileSizeUtils.humanReadableByteCountSI(calcSizeQuickShare()));
        logsSpaceTextView.setText(FileSizeUtils.humanReadableByteCountSI(calcSizeLogs()));
        rootTmpSpaceTextView.setText(FileSizeUtils.humanReadableByteCountSI(calcSizeRootTmp()));
    }

    private long calcSizeQuickShare() {
        return calcSizeDir(quickShareDir(), true);
    }

    private long calcSizeLogs() {
        return calcSizeDir(logsDir(), false);
    }

    private long calcSizeRootTmp() {
        return calcSizeDir(rootTmpDir(), true);
    }

    private long calcSizeDir(File dir, boolean includeChildren) {
        logger.trace("calcSizeDir({}, {})", dir, includeChildren);
        long size = 0;
        if (dir != null && dir.exists()) {
            for (File child : dir.listFiles()) {
                if (child.isFile()) {
                    size += child.length();
                } else if (child.isDirectory() && includeChildren) {
                    size += calcSizeDir(child, true);
                }
            }
        }
        return  size;
    }

    private int collectNumberOfFiles(File dir, boolean includeChildren) {
        int number = 0;
        if (dir != null && dir.exists()) {
            for (File child : dir.listFiles()) {
                if (child.isFile()) {
                    number ++;
                } else if (child.isDirectory() && includeChildren) {
                    number += calcSizeDir(child, true);
                }
            }
        }
        return  number;
    }

    protected ProgressDialog createProgressDialog(int maxProgress) {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMax(maxProgress);
        progressDialog.setMessage("delete ...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

        progressDialog.show();

        return progressDialog;
    }

    private static class DeleteTask extends AsyncTask<Void, Void, Void> {

        protected Logger logger = LoggerFactory.getLogger(getClass());

        private final CleanSpaceActivity activity;
        private final ProgressDialog progressDiag;
        private final File dir;
        private final boolean includeChildren;

        DeleteTask(CleanSpaceActivity activity,
                 ProgressDialog progressDiag,
                   File dir,
                   boolean includeChildren) {
            super();
            this.activity = activity;
            this.progressDiag = progressDiag;
            this.dir = dir;
            this.includeChildren = includeChildren;
        }

        @Override
        protected java.lang.Void doInBackground(java.lang.Void[] objects) {
            int counter = 0;
            delete(dir, includeChildren, counter);
            return null;
        }

        private int delete(File dir, boolean includeChildren, int counter) {
            if (dir != null && dir.exists()) {
                for (File child : dir.listFiles()) {
                    if (child.isFile()) {
                        child.delete();
                        counter ++;
                        progressDiag.setProgress(counter);
                    } else if (child.isDirectory() && includeChildren) {
                        counter = delete(child, true, counter);
                        child.delete();
                    }
                }
            }
            return counter;
        }

        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            progressDiag.dismiss();
            activity.updateView();
        }
    }
}
