package org.primftpd.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import org.primftpd.crypto.HostKeyAlgorithm;
import org.primftpd.util.KeyFingerprintProvider;
import org.primftpd.util.ServicesStartStopUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;

public class GenKeysAsyncTask extends AsyncTask<Void, Void, Void> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final KeyFingerprintProvider keyFingerprintProvider;
    private final PftpdFragment fragment;
    private final ProgressDialog progressDiag;
    private final boolean startServerOnFinish;

    public GenKeysAsyncTask(
            KeyFingerprintProvider keyFingerprintProvider,
            PftpdFragment fragment,
            ProgressDialog progressDiag,
            boolean startServerOnFinish) {
        super();
        logger.trace("GenKeysAsyncTask()");
        this.keyFingerprintProvider = keyFingerprintProvider;
        this.fragment = fragment;
        this.progressDiag = progressDiag;
        this.startServerOnFinish = startServerOnFinish;
    }

    @Override
    protected Void doInBackground(Void... params) {
        logger.debug("generating keys");
        try {
            String[] fileList = fragment.requireActivity().fileList();
            if (fileList != null) {
                logger.trace("num of existing files: '{}'", fileList.length);
                for (String file : fileList) {
                    logger.trace("existing file: '{}'", file);
                }
            } else {
                logger.trace("no existing files");
            }

            Context context = fragment.getContext();
            if (context == null) {
                logger.trace("context is null");
                return null;
            }
            progressDiag.setMax(HostKeyAlgorithm.values().length);
            int i=0;
            for (HostKeyAlgorithm hka : HostKeyAlgorithm.values()) {
                try (
                    FileOutputStream publickeyFos = keyFingerprintProvider.buildPublickeyOutStream(context, hka);
                    FileOutputStream privatekeyFos = keyFingerprintProvider.buildPrivatekeyOutStream(context, hka)
                ) {
                    hka.generateKey(publickeyFos, privatekeyFos);
                } catch (Exception e) {
                    logger.error("could not generate key " + hka.getAlgorithmName(), e);
                }
                i++;
                progressDiag.setProgress(i);
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
        keyFingerprintProvider.calcPubkeyFingerprints(fragment.getContext());
        progressDiag.dismiss();
        fragment.showKeyFingerprints();

        if (startServerOnFinish) {
            // icon members should be set at this time
            ServicesStartStopUtil.startServers(fragment.getContext(), null, keyFingerprintProvider, fragment);
        }
    }
}
