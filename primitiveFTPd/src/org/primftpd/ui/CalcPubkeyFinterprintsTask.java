package org.primftpd.ui;

import android.os.AsyncTask;

import org.primftpd.PrimitiveFtpdActivity;
import org.primftpd.util.KeyFingerprintProvider;

public class CalcPubkeyFinterprintsTask extends AsyncTask<Void, Void, Void> {

    private final KeyFingerprintProvider keyFingerprintProvider;
    private final PrimitiveFtpdActivity activity;

    public CalcPubkeyFinterprintsTask(KeyFingerprintProvider keyFingerprintProvider, PrimitiveFtpdActivity activity) {
        this.keyFingerprintProvider = keyFingerprintProvider;
        this.activity = activity;
    }

    @Override
    protected Void doInBackground(Void... params) {
        keyFingerprintProvider.calcPubkeyFingerprints(activity);
        return null;
    }
    @Override
    protected void onPostExecute(Void result){
        super.onPostExecute(result);
        activity.showKeyFingerprints();
    }
}
