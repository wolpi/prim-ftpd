package org.primftpd.ui;

import rikka.shizuku.Shizuku;

public class ShizukuListener implements
        Shizuku.OnRequestPermissionResultListener,
        Shizuku.OnBinderReceivedListener,
        Shizuku.OnBinderDeadListener{

    private boolean binderReceived;

    @Override
    public void onRequestPermissionResult(int requestCode, int grantResult) {
        // what to do here?
    }

    @Override
    public void onBinderReceived() {
        this.binderReceived = true;
    }

    @Override
    public void onBinderDead() {
        // what to do here?
    }

    public boolean isBinderReceived() {
        return binderReceived;
    }
}
