package org.primftpd.util;

public class KeyFingerprintBean {
    private final String fingerprintMd5;
    private final String fingerprintSha1;
    private final String fingerprintSha256;
    private final String base64Md5;
    private final String base64Sha1;
    private final String base64Sha256;
    private final String bytesMd5;
    private final String bytesSha1;
    private final String bytesSha256;

    public KeyFingerprintBean(
            String fingerprintMd5,
            String fingerprintSha1,
            String fingerprintSha256,
            String base64Md5,
            String base64Sha1,
            String base64Sha256,
            String bytesMd5,
            String bytesSha1,
            String bytesSha256
    ) {
        this.fingerprintMd5 = fingerprintMd5;
        this.fingerprintSha1 = fingerprintSha1;
        this.fingerprintSha256 = fingerprintSha256;
        this.base64Md5 = base64Md5;
        this.base64Sha1 = base64Sha1;
        this.base64Sha256 = base64Sha256;
        this.bytesMd5 = bytesMd5;
        this.bytesSha1 = bytesSha1;
        this.bytesSha256 = bytesSha256;
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
