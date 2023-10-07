package org.primftpd.ui;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.primftpd.R;
import org.primftpd.util.Defaults;
import org.primftpd.util.FileSizeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import androidx.fragment.app.Fragment;

public class CleanSpaceFragment extends Fragment {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private TextView quickShareSpaceTextView;
    private TextView logsSpaceTextView;
    private TextView rootTmpSpaceTextView;

    private static class DialogHandler extends Handler {
        DialogHandler() {
            super();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.clean_space, container, false);
        quickShareSpaceTextView = view.findViewById(R.id.quickShareFilesSize);
        logsSpaceTextView = view.findViewById(R.id.logFilesSize);
        rootTmpSpaceTextView = view.findViewById(R.id.rootTmpFilesSize);

        // register listeners
        final CleanSpaceFragment fragment = this;
        view.findViewById(R.id.quickShareFilesDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onButtonClick(fragment, quickShareDir(), true);
            }
        });
        view.findViewById(R.id.logFilesDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onButtonClick(fragment, logsDir(), false);
            }
        });
        view.findViewById(R.id.rootTmpFilesDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onButtonClick(fragment, rootTmpDir(), true);
            }
        });

        updateView();
        return view;
    }

    private void onButtonClick(CleanSpaceFragment fragment, File dir, boolean includeChildren) {
        int numberOfFiles = collectNumberOfFiles(dir, includeChildren);
        if (numberOfFiles > 0) {
            final ProgressDialog progressDialog = createProgressDialog(numberOfFiles);
            DeleteTask deleteTask = new DeleteTask(fragment, progressDialog, dir, includeChildren);
            deleteTask.execute();

            new DialogHandler();
        }
    }

    protected File quickShareDir() {
        return Defaults.quickShareTmpDir(this.getContext());
    }

    protected File logsDir() {
        return Defaults.homeDirScoped(this.getContext());
    }

    protected File rootTmpDir() {
        return Defaults.rootCopyTmpDir(this.getContext());
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
                    number += collectNumberOfFiles(child, true);
                }
            }
        }
        return  number;
    }

    protected ProgressDialog createProgressDialog(int maxProgress) {
        final ProgressDialog progressDialog = new ProgressDialog(this.getContext());
        progressDialog.setMax(maxProgress);
        progressDialog.setMessage("delete ...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

        progressDialog.show();

        return progressDialog;
    }

    private static class DeleteTask extends AsyncTask<Void, Void, Void> {

        protected Logger logger = LoggerFactory.getLogger(getClass());

        private final CleanSpaceFragment fragment;
        private final ProgressDialog progressDiag;
        private final File dir;
        private final boolean includeChildren;

        DeleteTask(CleanSpaceFragment fragment,
                 ProgressDialog progressDiag,
                 File dir,
                 boolean includeChildren) {
            super();
            this.fragment = fragment;
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
            fragment.updateView();
        }
    }
}
