package org.primftpd.ui;

import android.app.AlertDialog;
import android.os.Bundle;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class CleanSpaceFragment extends Fragment implements RecreateLogger {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private TextView quickShareSpaceTextView;
    private TextView logsSpaceTextView;
    private TextView rootTmpSpaceTextView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.clean_space, container, false);
        quickShareSpaceTextView = view.findViewById(R.id.quickShareFilesSize);
        logsSpaceTextView = view.findViewById(R.id.logFilesSize);
        rootTmpSpaceTextView = view.findViewById(R.id.rootTmpFilesSize);

        // register listeners
        final CleanSpaceFragment fragment = this;
        view.findViewById(R.id.quickShareFilesDelete).setOnClickListener(
                viewQ -> onButtonClick(fragment, quickShareDir(), true));
        view.findViewById(R.id.logFilesDelete).setOnClickListener(
                viewL -> onButtonClick(fragment, logsDir(), false));
        view.findViewById(R.id.rootTmpFilesDelete).setOnClickListener(
                viewR -> onButtonClick(fragment, rootTmpDir(), true));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateView();
    }

    private void onButtonClick(CleanSpaceFragment fragment, File dir, boolean includeChildren) {
        View view = fragment.getView();
        if (view == null) {
            logger.warn("view is null");
            return;
        }
        int numberOfFiles = collectNumberOfFiles(dir, includeChildren);
        if (numberOfFiles > 0) {
            final AlertDialog progressDialog = createProgressDialog();
            try (ExecutorService executorService = Executors.newSingleThreadExecutor()) {
                executorService.execute(() -> {
                    int counter = 0;
                    delete(dir, includeChildren, counter);

                    view.post(() -> {
                        progressDialog.dismiss();
                        fragment.updateView();
                    });
                });
            }
        }
    }

    protected AlertDialog createProgressDialog() {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this.getContext());
        dialogBuilder.setMessage("deleting ...");

        return dialogBuilder.create();
    }

    protected File quickShareDir() {
        return Defaults.quickShareTmpDir(this.requireContext());
    }

    protected File logsDir() {
        return Defaults.homeDirScoped(this.requireContext());
    }

    protected File rootTmpDir() {
        return Defaults.rootCopyTmpDir(this.requireContext());
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
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (child.isFile()) {
                        size += child.length();
                    } else if (child.isDirectory() && includeChildren) {
                        size += calcSizeDir(child, true);
                    }
                }
            }
        }
        return  size;
    }

    private int collectNumberOfFiles(File dir, boolean includeChildren) {
        int number = 0;
        if (dir != null && dir.exists()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (child.isFile()) {
                        number++;
                    } else if (child.isDirectory() && includeChildren) {
                        number += collectNumberOfFiles(child, true);
                    }
                }
            }
        }
        return  number;
    }

    private int delete(File dir, boolean includeChildren, int counter) {
        if (dir != null && dir.exists()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (child.isFile()) {
                        boolean deleted = child.delete();
                        if (!deleted) {
                            logger.info("could not delete file: {}", child.getAbsolutePath());
                        }
                        counter++;

                        // in previous android versions we had a progress dialog
                        //progressDiag.setProgress(counter);
                    } else if (child.isDirectory() && includeChildren) {
                        counter = delete(child, true, counter);
                        boolean deleted = child.delete();
                        if (!deleted) {
                            logger.info("could not delete dir: {}", child.getAbsolutePath());
                        }
                    }
                }
            }
        }
        return counter;
    }

    @Override
    public void recreateLogger() {
        this.logger = LoggerFactory.getLogger(getClass());
    }
}
