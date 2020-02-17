package org.primftpd.filepicker.nononsenseapps;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;


public class NewFolderFragment extends NewItemFragment {

    private static final String TAG = "new_folder_fragment";

    public static void showDialog(@NonNull final FragmentManager fm,
                                  @Nullable final OnNewFolderListener listener) {
        NewItemFragment d = new NewFolderFragment();
        d.setListener(listener);
        d.show(fm, TAG);
    }

    @Override
    protected boolean validateName(@Nullable final String itemName) {
        return Utils.isValidFileName(itemName);
    }
}
