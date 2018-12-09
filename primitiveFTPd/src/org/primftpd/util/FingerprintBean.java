package org.primftpd.util;

public class FingerprintBean {
    public final String bytes;
    public final String base64;

    public FingerprintBean(String bytes, String base64) {
        this.bytes = bytes;
        this.base64 = base64;
    }

    public String fingerprint() {
        return bytes + "\nBase 64\n" + base64;
    }
}
