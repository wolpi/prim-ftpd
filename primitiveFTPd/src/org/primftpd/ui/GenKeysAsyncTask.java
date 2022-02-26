package org.primftpd.ui;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import org.primftpd.PrimitiveFtpdActivity;
import org.primftpd.crypto.HostKeyAlgorithm;
import org.primftpd.util.KeyFingerprintProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;

public class GenKeysAsyncTask extends AsyncTask<Void, Void, Void> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final KeyFingerprintProvider keyFingerprintProvider;
    private final PrimitiveFtpdActivity activity;
    private final ProgressDialog progressDiag;
    private final boolean startServerOnFinish;

    public GenKeysAsyncTask(
            KeyFingerprintProvider keyFingerprintProvider,
            PrimitiveFtpdActivity activity,
            ProgressDialog progressDiag,
            boolean startServerOnFinish) {
        logger.trace("GenKeysAsyncTask()");
        this.keyFingerprintProvider = keyFingerprintProvider;
        this.activity = activity;
        this.progressDiag = progressDiag;
        this.startServerOnFinish = startServerOnFinish;
    }

    @Override
    protected Void doInBackground(Void... params) {
        logger.debug("generating keys");
        try {
            String[] fileList = activity.fileList();
            if (fileList != null) {
                logger.trace("num of existing files: '{}'", fileList.length);
                for (String file : fileList) {
                    logger.trace("existing file: '{}'", file);
                }
            } else {
                logger.trace("no existing files");
            }

            for (HostKeyAlgorithm hka : HostKeyAlgorithm.values()) {
                FileOutputStream publickeyFos = keyFingerprintProvider.buildPublickeyOutStream(activity, hka);
                FileOutputStream privatekeyFos = keyFingerprintProvider.buildPrivatekeyOutStream(activity, hka);
                try {
                    hka.generateKey(publickeyFos, privatekeyFos);
                } catch (Exception e) {
                    logger.error("could not generate key " + hka.getAlgorithmName(), e);
                } finally {
                    publickeyFos.close();
                    privatekeyFos.close();
                }
            }
        } catch (Exception e) {
            logger.error("could not generate keys", e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        logger.trace("onPostExecute()");
        keyFingerprintProvider.calcPubkeyFingerprints(activity);
        progressDiag.dismiss();
        activity.showKeyFingerprints();

        if (startServerOnFinish) {
            // icon members should be set at this time
            activity.handleStart();
        }
    }
}
