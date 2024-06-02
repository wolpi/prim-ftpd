package org.primftpd.crypto;

import org.apache.ftpserver.util.IoUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.util.OpenSSHPrivateKeyUtil;
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jcajce.provider.asymmetric.edec.KeyFactorySpi;
import org.bouncycastle.jcajce.provider.asymmetric.edec.KeyPairGeneratorSpi;
import org.bouncycastle.util.encoders.Base64;
import org.primftpd.pojo.Base64Decoder;
import org.primftpd.pojo.KeyParser;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

public enum HostKeyAlgorithm {

    ED_25519 {
        @Override
        public String getAlgorithmName() {
            return "ed25519";
        }

        @Override
        public String getDisplayName() {
            return "ed25519";
        }

        @Override
        public String getFilenamePrivateKey() {
            return "id_ed25519";
        }

        @Override
        public String getFilenamePublicKey() {
            return "id_ed25519.pub";
        }

        @Override
        public String getPreferenceValue() {
            return "ed25519";
        }

        @Override
        public void generateKey(FileOutputStream pubKeyFos, FileOutputStream privKeyFos)
                throws IOException, NoSuchAlgorithmException {
            generateEd25519(pubKeyFos, privKeyFos);
        }

        @Override
        public PublicKey readPublicKey(FileInputStream fis)
                throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
            return readPublicKeyEd25519(fis);
        }

        @Override
        public PrivateKey readPrivateKey(FileInputStream fis)
                throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
            return readPrivateKeyEd25519(fis);
        }

        @Override
        public byte[] encodeAsSsh(PublicKey pubKey) throws IOException {
            return encodeAsSshEd25519(pubKey);
        }
    },

    ECDSA_256 {
        @Override
        public String getAlgorithmName() {
            return "EC";
        }

        @Override
        public String getDisplayName() {
            return "ECDSA 256";
        }

        @Override
        public String getFilenamePrivateKey() {
            return "id_ecdsa265";
        }

        @Override
        public String getFilenamePublicKey() {
            return "id_ecdsa265.pub";
        }

        @Override
        public String getPreferenceValue() {
            return "ecdsa256";
        }

        @Override
        public void generateKey(FileOutputStream pubKeyFos, FileOutputStream privKeyFos)
                throws IOException, NoSuchAlgorithmException {
            generateEcdsa256(pubKeyFos, privKeyFos);
        }

        @Override
        public byte[] encodeAsSsh(PublicKey pubKey) throws IOException {
            return encodeAsSshEcdsa256(pubKey);
        }
    },

    RSA_4096 {
        @Override
        public String getAlgorithmName() {
            return "RSA";
        }

        @Override
        public String getDisplayName() {
            return "RSA 4096";
        }

        @Override
        public String getFilenamePrivateKey() {
            return "id_rsa_4096";
        }

        @Override
        public String getFilenamePublicKey() {
            return "id_rsa_4096.pub";
        }

        @Override
        public String getPreferenceValue() {
            return "rsa4096";
        }

        @Override
        public void generateKey(FileOutputStream pubKeyFos, FileOutputStream privKeyFos)
                throws IOException, NoSuchAlgorithmException {
            generateRsa(4096, pubKeyFos, privKeyFos);
        }

        @Override
        public byte[] encodeAsSsh(PublicKey pubKey) throws IOException {
            return encodeAsSsh((RSAPublicKey)pubKey);
        }
    },

    RSA_2048 {
        @Override
        public String getAlgorithmName() {
            return "RSA";
        }

        @Override
        public String getDisplayName() {
            return "RSA 2048";
        }

        @Override
        public String getFilenamePrivateKey() {
            return "pftpd-priv.pk8";
        }

        @Override
        public String getFilenamePublicKey() {
            return "pftpd-pub.bin";
        }

        @Override
        public String getPreferenceValue() {
            return "rsa2048";
        }

        @Override
        public void generateKey(FileOutputStream pubKeyFos, FileOutputStream privKeyFos)
                throws IOException, NoSuchAlgorithmException {
            generateRsa(2048, pubKeyFos, privKeyFos);
        }

        @Override
        public byte[] encodeAsSsh(PublicKey pubKey) throws IOException {
            return encodeAsSsh((RSAPublicKey)pubKey);
        }
    };

    public abstract String getAlgorithmName();
    public abstract String getDisplayName();
    public abstract void generateKey(FileOutputStream pubKeyFos, FileOutputStream privKeyFos)
            throws IOException, NoSuchAlgorithmException;

    public abstract byte[] encodeAsSsh(PublicKey pubKey)
            throws IOException;

    public abstract String getFilenamePrivateKey();
    public abstract String getFilenamePublicKey();
    public abstract String getPreferenceValue();

    private static final int BUFFER_SIZE = 4096;

    public PublicKey readPublicKey(FileInputStream fis)
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IoUtils.copy(fis, baos, BUFFER_SIZE);
        byte[] pubKeyBytes = baos.toByteArray();
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(pubKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(getAlgorithmName());
        PublicKey publicKey = keyFactory.generatePublic(pubKeySpec);
        return publicKey;
    }

    public PrivateKey readPrivateKey(FileInputStream fis)
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IoUtils.copy(fis, baos, BUFFER_SIZE);
        byte[] privKeyBytes = baos.toByteArray();
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(getAlgorithmName());
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
        return privateKey;
    }

    void generateGeneric(FileOutputStream pubKeyFos, FileOutputStream privKeyFos)
            throws IOException, NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(getAlgorithmName());
        KeyPair keypair = keyGen.generateKeyPair();
        PrivateKey privKey = keypair.getPrivate();
        PublicKey pubKey = keypair.getPublic();

        pubKeyFos.write(pubKey.getEncoded());

        PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(privKey.getEncoded());
        privKeyFos.write(privKeySpec.getEncoded());
    }

    void generateRsa(int keySize, FileOutputStream pubKeyFos, FileOutputStream privKeyFos)
            throws IOException, NoSuchAlgorithmException
    {
        SecureRandom sr = new SecureRandom();

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(getAlgorithmName());
        keyGen.initialize(keySize, sr);
        KeyPair keypair = keyGen.generateKeyPair();
        PrivateKey privKey = keypair.getPrivate();
        PublicKey pubKey = keypair.getPublic();

        pubKeyFos.write(pubKey.getEncoded());

        PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(privKey.getEncoded());
        privKeyFos.write(privKeySpec.getEncoded());
    }

    void generateEcdsa256(FileOutputStream pubKeyFos, FileOutputStream privKeyFos)
            throws IOException, NoSuchAlgorithmException
    {
        SecureRandom sr = new SecureRandom();

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(getAlgorithmName());
        try {
            keyGen.initialize(new ECGenParameterSpec("secp256r1"), sr);
            KeyPair keypair = keyGen.generateKeyPair();
            PrivateKey privKey = keypair.getPrivate();
            PublicKey pubKey = keypair.getPublic();

            pubKeyFos.write(pubKey.getEncoded());

            PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(privKey.getEncoded());
            privKeyFos.write(privKeySpec.getEncoded());
        } catch (InvalidAlgorithmParameterException e) {
            throw new NoSuchAlgorithmException(e);
        }
    }

    void generateEd25519(FileOutputStream pubKeyFos, FileOutputStream privKeyFos) throws IOException {
        KeyPairGeneratorSpi.Ed25519 ed25519 = new KeyPairGeneratorSpi.Ed25519();
        ed25519.initialize(256, new SecureRandom());
        KeyPair keyPair = ed25519.generateKeyPair();

        PrivateKey privateKey = keyPair.getPrivate();

        Ed25519PrivateKeyParameters ed25519PrivateKeyParameters =
                (Ed25519PrivateKeyParameters) PrivateKeyFactory.createKey(privateKey.getEncoded());
        byte[] contentPriv = OpenSSHPrivateKeyUtil.encodePrivateKey(ed25519PrivateKeyParameters);
        byte[] privKeyBase64 = Base64.encode(contentPriv);
        privKeyFos.write(privKeyBase64);

        PublicKey publicKey = keyPair.getPublic();
        Ed25519PublicKeyParameters publicKeyParameters =
                (Ed25519PublicKeyParameters) PublicKeyFactory.createKey(publicKey.getEncoded());
        byte[] contentPub = OpenSSHPublicKeyUtil.encodePublicKey(publicKeyParameters);
        pubKeyFos.write("ssh-ed25519 ".getBytes(Charset.forName("utf8")));
        pubKeyFos.write(Base64.encode(contentPub));
    }

    PublicKey readPublicKeyEd25519(FileInputStream fis) throws IOException
    {
        List<String> parserErrors = new ArrayList<>();
        List<PublicKey> keys = KeyParser.parsePublicKeys(fis, new Base64Decoder() {
                @Override
                public byte[] decode(String str) {
                    return android.util.Base64.decode(str, android.util.Base64.DEFAULT);
                }
            },
            parserErrors);
        return !keys.isEmpty() ? keys.get(0) : null;
    }

    PrivateKey readPrivateKeyEd25519(FileInputStream fis) throws IOException
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        copyStream(fis, bos);
        byte[] keyBytesBase64 = bos.toByteArray();

        byte[] keyBytes = Base64.decode(keyBytesBase64);
        AsymmetricKeyParameter keyParameter = OpenSSHPrivateKeyUtil.parsePrivateKeyBlob(keyBytes);

        PrivateKeyInfo privateKeyInfo = PrivateKeyInfoFactory.createPrivateKeyInfo(keyParameter);
        KeyFactorySpi factory = new KeyFactorySpi.Ed25519();
        return factory.generatePrivate(privateKeyInfo);
    }

    private void copyStream(InputStream is, OutputStream os) throws IOException {
        byte[] bytes = new byte[BUFFER_SIZE];
        for (;;) {
            int count = is.read(bytes, 0, BUFFER_SIZE);
            if (count == -1) {
                break;
            }
            os.write(bytes, 0, count);
        }
    }

    byte[] encodeAsSsh(RSAPublicKey pubKey)
            throws IOException
    {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        byte[] name = "ssh-rsa".getBytes("US-ASCII");
        writeKeyPart(name, buf);

        writeKeyPart(pubKey.getPublicExponent().toByteArray(), buf);
        writeKeyPart(pubKey.getModulus().toByteArray(), buf);

        return buf.toByteArray();
    }

    byte[] encodeAsSshEcdsa256(PublicKey pubKey)
            throws IOException
    {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        byte[] name = "ecdsa-sha2-nistp256".getBytes("US-ASCII");
        writeKeyPart(name, buf);
        byte[] curveName = "nistp256".getBytes("US-ASCII");
        writeKeyPart(curveName, buf);

        AsymmetricKeyParameter asyncKeyParameter = PublicKeyFactory.createKey(pubKey.getEncoded());
        ECPublicKeyParameters ecPubKeyParas = (ECPublicKeyParameters)asyncKeyParameter;
        byte[] encoded = ecPubKeyParas.getQ().getEncoded(false);
        writeKeyPart(encoded, buf);

        return buf.toByteArray();
    }

    byte[] encodeAsSshEd25519(PublicKey pubKey)
            throws IOException
    {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        byte[] name = "ssh-ed25519".getBytes("US-ASCII");
        writeKeyPart(name, buf);

        Ed25519PublicKeyParameters publicKeyParameters =
                (Ed25519PublicKeyParameters) PublicKeyFactory.createKey(pubKey.getEncoded());
        byte[] contentPub = publicKeyParameters.getEncoded();
        writeKeyPart(contentPub, buf);

        return buf.toByteArray();
    }

    void writeKeyPart(byte[] bytes, OutputStream os)
            throws IOException
    {
        for (int shift = 24; shift >= 0; shift -= 8) {
            os.write((bytes.length >>> shift) & 0xFF);
        }
        os.write(bytes);
    }
}
