package org.primftpd.filepicker;

import android.os.Environment;
import androidx.annotation.Nullable;


import org.primftpd.filepicker.nononsenseapps.AbstractFilePickerFragment;
import org.primftpd.filepicker.nononsenseapps.FilePickerActivity;

import java.io.File;


public class ResettingFilePickerActivity extends FilePickerActivity {
    public ResettingFilePickerActivity() {
        super();
    }

    @Override
    protected AbstractFilePickerFragment<File> getFragment(
            @Nullable final String startPath, final int mode, final boolean allowMultiple,
            final boolean allowCreateDir, final boolean allowExistingFile,
            final boolean singleClick) {
        AbstractFilePickerFragment<File> fragment = new ResettingFilePickerFragment();
        // startPath is allowed to be null. In that case, default folder should be SD-card and not "/"
        fragment.setArgs(startPath != null ? startPath : Environment.getExternalStorageDirectory().getPath(),
                mode, allowMultiple, allowCreateDir, allowExistingFile, singleClick);
        return fragment;
    }
}
