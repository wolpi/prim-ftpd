package org.primftpd.ui;

import android.view.View;

import org.primftpd.PrimitiveFtpdActivity;
import org.primftpd.R;

public class LeanbackActivity extends PrimitiveFtpdActivity {

    protected int getLayoutId() {
        return R.layout.leanback;
    }

    public void handleStart(View view) {
        super.handleStart();
    }

    public void handleStop(View view) {
        super.handleStop();
    }

    public void handlePrefs(View view) {
        super.handlePrefs();
    }
}
