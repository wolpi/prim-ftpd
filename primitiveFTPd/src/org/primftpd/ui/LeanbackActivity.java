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

    public void handleQr(View view) {
        super.handleQr();
    }

    public void handleClientAction(View view) {
        super.handleClientAction();
    }

    public void handleKeysFingerprints(View view) {
        super.handleKeysFingerprints();
    }

    public void handleClean(View view) {
        super.handleClean();
    }

    public void handleAbout(View view) {
        super.handleAbout();
    }
}
