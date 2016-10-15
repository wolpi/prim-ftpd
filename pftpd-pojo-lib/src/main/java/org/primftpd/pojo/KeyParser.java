package org.primftpd.pojo;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.JCEECPublicKey;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class KeyParser {

    // http://blog.oddbit.com/2011/05/08/converting-openssh-public-keys/

    public static final String NAME_RSA = "ssh-rsa";
    public static final String NAME_DSA = "ssh-dss";
    public static final String NAME_ECDSA_256 = "ecdsa-sha2-nistp256";
    public static final String NAME_ECDSA_384 = "ecdsa-sha2-nistp384";
    public static final String NAME_ECDSA_521 = "ecdsa-sha2-nistp521";
    public static final int LENGTH_LENGTH = 4;

    public static final Map<String, Integer> EC_NAME_TO_COORD_SIZE;
    public static final Map<String, String> EC_NAME_TO_CURVE_NAME;

    static {
        Map<String, Integer> tmpCoordSize = new HashMap<>();
        tmpCoordSize.put(NAME_ECDSA_256, Integer.valueOf(32));
        tmpCoordSize.put(NAME_ECDSA_384, Integer.valueOf(48));
        tmpCoordSize.put(NAME_ECDSA_521, Integer.valueOf(66));
        EC_NAME_TO_COORD_SIZE = Collections.unmodifiableMap(tmpCoordSize);

        // see org.bouncycastle.asn1.nist.NISTNamedCurves
        Map<String, String> tmpCurveName = new HashMap<>();
        tmpCurveName.put(NAME_ECDSA_256, "P-256");
        tmpCurveName.put(NAME_ECDSA_384, "P-384");
        tmpCurveName.put(NAME_ECDSA_521, "P-521");
        EC_NAME_TO_CURVE_NAME = Collections.unmodifiableMap(tmpCurveName);
    }

    public static List<PublicKey> parsePublicKeys(InputStream is, Base64Decoder base64Decoder)
            throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
        if (is == null) {
            throw new IllegalArgumentException("input stream cannot be null");
        }
        List<PublicKey> keys = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        while (reader.ready()) {
            String keyLine = reader.readLine();
            String[] parts = keyLine.split(" ");

            String name = null;
            String keyEncoded = null;
            if (parts.length <= 3) {
                name = parts[0];
                keyEncoded = parts[1];
            }

            if (keyEncoded != null) {
                byte[] keyBytes = base64Decoder.decode(keyEncoded);

                PublicKey key = null;
                if (NAME_RSA.equals(name)) {
                    key = parsePublicKeyRsa(keyBytes);
                } else if (NAME_DSA.equals(name)) {
                    key = parsePublicKeyDsa(keyBytes);
                } else if (NAME_ECDSA_256.equals(name)) {
                    key = parsePublicKeyEcdsa(name, keyBytes);
                } else if (NAME_ECDSA_384.equals(name)) {
                    key = parsePublicKeyEcdsa(name, keyBytes);
                } else if (NAME_ECDSA_521.equals(name)) {
                    key = parsePublicKeyEcdsa(name, keyBytes);
                }
                if (key != null) {
                    keys.add(key);
                }
            }
        }

        return keys;
    }

    protected static PublicKey parsePublicKeyRsa(byte[] keyBytes)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        // name is also included in bytes
        ByteBuffer byteBuffer = ByteBuffer.wrap(keyBytes);
        int nameLength = byteBuffer.getInt();
        byteBuffer.position(nameLength + LENGTH_LENGTH);

        BigInteger exponent = readNext(byteBuffer);
        BigInteger modulus = readNext(byteBuffer);

        return createPubKeyRsa(exponent, modulus);
    }

    protected static PublicKey parsePublicKeyDsa(byte[] keyBytes)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        // name is also included in bytes
        ByteBuffer byteBuffer = ByteBuffer.wrap(keyBytes);
        int nameLength = byteBuffer.getInt();
        byteBuffer.position(nameLength + LENGTH_LENGTH);

        BigInteger p = readNext(byteBuffer);
        BigInteger q = readNext(byteBuffer);
        BigInteger g = readNext(byteBuffer);
        BigInteger y = readNext(byteBuffer);

        return createPubKeyDsa(y, p, q, g);
    }

    protected static BigInteger readNext(ByteBuffer byteBuffer) {
        int nextLength = byteBuffer.getInt();
        byte[] nextBytes = new byte[nextLength];
        byteBuffer.get(nextBytes);
        return new BigInteger(nextBytes);
    }

    protected static PublicKey createPubKeyRsa(BigInteger exponent, BigInteger modulus)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(keySpec);
    }

    protected static PublicKey createPubKeyDsa(
            BigInteger y, BigInteger p, BigInteger q, BigInteger g)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        DSAPublicKeySpec keySpec = new DSAPublicKeySpec(y, p, q, g);
        KeyFactory kf = KeyFactory.getInstance("DSA");
        return kf.generatePublic(keySpec);
    }

    protected static PublicKey parsePublicKeyEcdsa(String name, byte[] keyBytes)
            throws InvalidKeySpecException, NoSuchAlgorithmException, IOException, NoSuchProviderException {

        ByteBuffer byteBuffer = ByteBuffer.wrap(keyBytes);

        // https://security.stackexchange.com/questions/129910/ecdsa-why-do-ssh-keygen-and-java-generated-public-keys-have-different-sizes
        final int coordLength = EC_NAME_TO_COORD_SIZE.get(name);
        byteBuffer.position(keyBytes.length - 2*coordLength);
        byte[] xBytes = new byte[coordLength];
        byteBuffer.get(xBytes);
        BigInteger x = new BigInteger(1, xBytes);

        byteBuffer.position(keyBytes.length - coordLength);
        byte[] yBytes = new byte[coordLength];
        byteBuffer.get(yBytes);
        BigInteger y = new BigInteger(1, yBytes);

        return createPubKeyEcdsa(name, x, y);
    }

    public static PublicKey createPubKeyEcdsa(String name, BigInteger x, BigInteger y)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        final String curveName = EC_NAME_TO_CURVE_NAME.get(name);
        ECNamedCurveParameterSpec curveParaSpecBc = ECNamedCurveTable.getParameterSpec(curveName);
        ECPoint point = curveParaSpecBc.getCurve().createPoint(x, y);
        ECPublicKeySpec pubKeySpec = new ECPublicKeySpec(point, curveParaSpecBc);
        return new JCEECPublicKey("EC", pubKeySpec);
    }
}
