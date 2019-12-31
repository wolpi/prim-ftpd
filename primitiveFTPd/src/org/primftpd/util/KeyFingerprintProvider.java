package org.primftpd.util;

import android.content.Context;

import org.apache.ftpserver.util.IoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

public class KeyFingerprintProvider implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean fingerprintsGenerated = false;
    private boolean keyPresent = false;
    private String fingerprintMd5 = " - ";
    private String fingerprintSha1 = " - ";
    private String fingerprintSha256 = " - ";
    private String base64Md5 = "";
    private String base64Sha1 = "";
    private String base64Sha256 = "";
    private String bytesMd5 = "";
    private String bytesSha1 = "";
    private String bytesSha256 = "";

    public FileInputStream buildPublickeyInStream(Context ctxt) throws IOException {
        FileInputStream fis = ctxt.openFileInput(Defaults.PUBLICKEY_FILENAME);
        return fis;
    }

    public FileOutputStream buildPublickeyOutStream(Context ctxt) throws IOException {
        FileOutputStream fos = ctxt.openFileOutput(Defaults.PUBLICKEY_FILENAME, Context.MODE_PRIVATE);
        return fos;
    }

    public FileInputStream buildPrivatekeyInStream(Context ctxt) throws IOException {
        FileInputStream fis = ctxt.openFileInput(Defaults.PRIVATEKEY_FILENAME);
        return fis;
    }

    public FileOutputStream buildPrivatekeyOutStream(Context ctxt) throws IOException {
        FileOutputStream fos = ctxt.openFileOutput(Defaults.PRIVATEKEY_FILENAME, Context.MODE_PRIVATE);
        return fos;
    }

    /**
     * Creates figerprints of public key.
     */
    public void calcPubkeyFingerprints(Context ctxt) {
        Logger logger = LoggerFactory.getLogger(getClass());
        logger.trace("calcPubkeyFingerprints()");
        fingerprintsGenerated = true;
        FileInputStream fis = null;
        try {
            fis = buildPublickeyInStream(ctxt);

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
            FingerprintBean bean = keyInfoprovider.fingerprint(encodedKey, "MD5");
            if (bean != null) {
                fingerprintMd5 = bean.fingerprint();
                base64Md5 = bean.base64;
                bytesMd5 = bean.bytes;
            }

            bean = keyInfoprovider.fingerprint(encodedKey, "SHA-1");
            if (bean != null) {
                fingerprintSha1 = bean.fingerprint();
                base64Sha1 = bean.base64;
                bytesSha1 = bean.bytes;
            }

            bean = keyInfoprovider.fingerprint(encodedKey, "SHA-256");
            if (bean != null) {
                fingerprintSha256 = bean.fingerprint();
                base64Sha256 = bean.base64;
                bytesSha256 = bean.bytes;
            }

            keyPresent = true;

        } catch (Exception e) {
            logger.info("key does probably not exist");
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

    public String getBase64Md5() {
        return base64Md5;
    }

    public String getBase64Sha1() {
        return base64Sha1;
    }

    public String getBase64Sha256() {
        return base64Sha256;
    }

    public String getBytesMd5() {
        return bytesMd5;
    }

    public String getBytesSha1() {
        return bytesSha1;
    }

    public String getBytesSha256() {
        return bytesSha256;
    }
}
