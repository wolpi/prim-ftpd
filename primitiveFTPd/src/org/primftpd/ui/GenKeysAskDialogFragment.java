package org.primftpd.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import org.primftpd.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenKeysAskDialogFragment extends DialogFragment {
    public static final String KEY_START_SERVER = "START_SERVER";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private boolean startServerOnFinish;

    private final PftpdFragment pftpdFragment;

    public GenKeysAskDialogFragment(PftpdFragment pftpdFragment) {
        this.pftpdFragment = pftpdFragment;
    }
    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        startServerOnFinish = args.getBoolean(KEY_START_SERVER);
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        logger.debug("showing gen key dialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.generateKeysMessage);
        builder.setPositiveButton(R.string.generate, (dialog, id) -> genKeysAndShowProgressDiag(startServerOnFinish));
        builder.setNegativeButton(R.string.cancel, (dialog, id) -> {
            // nothing
        });
        return builder.create();
    }

    public void genKeysAndShowProgressDiag(boolean startServerOnFinish) {
        logger.trace("genKeysAndShowProgressDiag()");
        // critical: do not pass getApplicationContext() to dialog
        final ProgressDialog progressDiag = new ProgressDialog(getContext());
        progressDiag.setCancelable(false);
        progressDiag.setMessage(getText(R.string.generatingKeysMessage));
        progressDiag.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

        AsyncTask<Void, Void, Void> task = new GenKeysAsyncTask(
                pftpdFragment,
                progressDiag,
                startServerOnFinish);
        task.execute();

        progressDiag.show();
    }
}
