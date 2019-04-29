package org.primftpd.ui;

import android.os.AsyncTask;

import org.primftpd.PrimitiveFtpdActivity;
import org.primftpd.util.KeyFingerprintProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CalcPubkeyFinterprintsTask extends AsyncTask<Void, Void, Void> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final KeyFingerprintProvider keyFingerprintProvider;
    private final PrimitiveFtpdActivity activity;

    public CalcPubkeyFinterprintsTask(KeyFingerprintProvider keyFingerprintProvider, PrimitiveFtpdActivity activity) {
        logger.trace("CalcPubkeyFinterprintsTask()");
        this.keyFingerprintProvider = keyFingerprintProvider;
        this.activity = activity;
    }

    @Override
    protected Void doInBackground(Void... params) {
        logger.trace("onPostExecute()");
        keyFingerprintProvider.calcPubkeyFingerprints(activity);
        return null;
    }
    @Override
    protected void onPostExecute(Void result){
        super.onPostExecute(result);
        logger.trace("onPostExecute()");
        activity.showKeyFingerprints();
    }
}
