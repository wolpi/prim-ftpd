package org.primftpd.util;

import android.content.Context;

import org.apache.ftpserver.util.IoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

public class KeyFingerprintProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Context ctxt;

    private boolean fingerprintsGenerated = false;
    private boolean keyPresent = false;
    private String fingerprintMd5 = " - ";
    private String fingerprintSha1 = " - ";
    private String fingerprintSha256 = " - ";

    public KeyFingerprintProvider(Context ctxt) {
        this.ctxt = ctxt;
    }

    public FileInputStream buildPublickeyInStream() throws IOException {
        FileInputStream fis = ctxt.openFileInput(Defaults.PUBLICKEY_FILENAME);
        return fis;
    }

    public FileOutputStream buildPublickeyOutStream() throws IOException {
        FileOutputStream fos = ctxt.openFileOutput(Defaults.PUBLICKEY_FILENAME, Context.MODE_PRIVATE);
        return fos;
    }

    public FileInputStream buildPrivatekeyInStream() throws IOException {
        FileInputStream fis = ctxt.openFileInput(Defaults.PRIVATEKEY_FILENAME);
        return fis;
    }

    public FileOutputStream buildPrivatekeyOutStream() throws IOException {
        FileOutputStream fos = ctxt.openFileOutput(Defaults.PRIVATEKEY_FILENAME, Context.MODE_PRIVATE);
        return fos;
    }

    /**
     * Creates figerprints of public key.
     */
    public void calcPubkeyFingerprints() {
        fingerprintsGenerated = true;
        FileInputStream fis = null;
        try {
            fis = buildPublickeyInStream();

            // check if key is present
            if (fis.available() <= 0) {
                keyPresent = false;
                throw new Exception("key seems not to be present");
            }

            KeyInfoProvider keyInfoprovider = new KeyInfoProvider();
            PublicKey pubKey = keyInfoprovider.readPublicKey(fis);
            RSAPublicKey rsaPubKey = (RSAPublicKey) pubKey;
            byte[] encodedKey = keyInfoprovider.encodeAsSsh(rsaPubKey);

            // fingerprints
            String fp = keyInfoprovider.fingerprint(encodedKey, "MD5");
            if (fp != null) {
                fingerprintMd5 = fp;
            }

            fp = keyInfoprovider.fingerprint(encodedKey, "SHA-1");
            if (fp != null) {
                fingerprintSha1 = fp;
            }

            fp = keyInfoprovider.fingerprint(encodedKey, "SHA-256");
            if (fp != null) {
                fingerprintSha256 = fp;
            }

            keyPresent = true;

        } catch (Exception e) {
            logger.debug("key does probably not exist");
        } finally {
            if (fis != null) {
                IoUtils.close(fis);
            }
        }
    }

    public boolean areFingerprintsGenerated() {
        return fingerprintsGenerated;
    }

    public boolean isKeyPresent() {
        return keyPresent;
    }

    public String getFingerprintMd5() {
        return fingerprintMd5;
    }

    public String getFingerprintSha1() {
        return fingerprintSha1;
    }

    public String getFingerprintSha256() {
        return fingerprintSha256;
    }
}
