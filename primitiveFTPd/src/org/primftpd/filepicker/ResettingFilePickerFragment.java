package org.primftpd.filepicker;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.primftpd.R;
import org.primftpd.filepicker.nononsenseapps.FilePickerFragment;
import org.primftpd.util.Defaults;

public class ResettingFilePickerFragment extends FilePickerFragment {

    protected View inflateRootView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.resetting_filepicker, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.button_reset).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToDir(Defaults.HOME_DIR);
            }
        });
    }
}
