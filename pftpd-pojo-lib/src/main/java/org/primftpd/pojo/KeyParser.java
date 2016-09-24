package org.primftpd.pojo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.List;

public class KeyParser {

    // http://blog.oddbit.com/2011/05/08/converting-openssh-public-keys/

    public static final String NAME_RSA = "ssh-rsa";
    public static final String NAME_DSA = "ssh-dss";
    public static final String NAME_ECDSA = "ecdsa-sha2-nistp256";
    public static final int LENGTH_LENGTH = 4;

    public static List<PublicKey> parsePublicKeys(InputStream is, Base64Decoder base64Decoder)
            throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
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
}
