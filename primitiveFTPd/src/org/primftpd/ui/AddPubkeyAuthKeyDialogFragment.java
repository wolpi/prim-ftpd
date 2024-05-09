package org.primftpd.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.primftpd.R;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class AddPubkeyAuthKeyDialogFragment extends DialogFragment {

    protected final PubKeyAuthKeysFragment fragment;

    public AddPubkeyAuthKeyDialogFragment(PubKeyAuthKeysFragment fragment) {
        this.fragment = fragment;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.addPubkeyForAuth);

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_key, null);
        builder.setView(view);

        builder.setPositiveButton(R.string.add, (dialog, id) -> {
            TextView textbox = view.findViewById(R.id.addPubkeyTextbox);
            CharSequence key = textbox.getText();
            fragment.addKeyToFile(key);
        });
        builder.setNegativeButton(R.string.cancel, (dialog, id) -> {
            // nothing
        });

        return builder.create();
    }
}
