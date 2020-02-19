package org.primftpd.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;

import org.primftpd.PrimitiveFtpdActivity;
import org.primftpd.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenKeysAskDialogFragment extends DialogFragment {
    public static final String KEY_START_SERVER = "START_SERVER";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private boolean startServerOnFinish;

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        startServerOnFinish = args.getBoolean(KEY_START_SERVER);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        logger.debug("showing gen key dialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.generateKeysMessage);
        builder.setPositiveButton(R.string.generate, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                PrimitiveFtpdActivity activity = (PrimitiveFtpdActivity)getActivity();
                activity.genKeysAndShowProgressDiag(startServerOnFinish);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // nothing
            }
        });
        return builder.create();
    }
}
