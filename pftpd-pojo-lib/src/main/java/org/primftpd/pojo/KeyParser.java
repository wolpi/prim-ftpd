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
import java.util.Arrays;

public class KeyParser {

    // http://blog.oddbit.com/2011/05/08/converting-openssh-public-keys/

    public static final String NAME_RSA = "ssh-rsa";
    public static final String NAME_DSA = "ssh-dss";
    public static final String NAME_ECDSA = "ecdsa-sha2-nistp256";
    public static final int LENGTH_LENGTH = 4;

    public static PublicKey parsePublicKey(InputStream is, Base64Decoder base64Decoder)
            throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        if (is == null) {
            throw new IllegalArgumentException("input stream cannot be null");
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
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

            if (NAME_RSA.equals(name)) {
                return parsePublicKeyRsa(keyBytes);
            } else if (NAME_DSA.equals(name)) {
                return parsePublicKeyDsa(keyBytes);
            }
        }

        return null;
    }

    protected static PublicKey parsePublicKeyRsa(byte[] keyBytes)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        // name is also included in bytes
        ByteBuffer byteBuffer = ByteBuffer.wrap(keyBytes);
        int nameLength = byteBuffer.getInt();

        // read exponent
        int exponentLengthPos = nameLength + LENGTH_LENGTH;
        byteBuffer.position(exponentLengthPos);
        int exponentLength = byteBuffer.getInt();
        byte[] exponentBytes = Arrays.copyOfRange(
                keyBytes,
                byteBuffer.position(),
                byteBuffer.position() + exponentLength);
        BigInteger exponent = new BigInteger(exponentBytes);

        // read modulus
        int modulusLengthPos = exponentLengthPos + exponentLength + LENGTH_LENGTH;
        byteBuffer.position(modulusLengthPos);
        int modulusLength = byteBuffer.getInt();
        byte[] modulusBytes = Arrays.copyOfRange(
                keyBytes,
                byteBuffer.position(),
                byteBuffer.position() + modulusLength);
        BigInteger modulus = new BigInteger(modulusBytes);

        return createPubKeyRsa(exponent, modulus);
    }

    protected static PublicKey createPubKeyRsa(BigInteger exponent, BigInteger modulus)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(keySpec);
    }

    protected static PublicKey parsePublicKeyDsa(byte[] keyBytes)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        // name is also included in bytes
        ByteBuffer byteBuffer = ByteBuffer.wrap(keyBytes);
        int nameLength = byteBuffer.getInt();

        // read p
        int pLengthPos = nameLength + LENGTH_LENGTH;
        byteBuffer.position(pLengthPos);
        int pLength = byteBuffer.getInt();
        byte[] pBytes = Arrays.copyOfRange(
                keyBytes,
                byteBuffer.position(),
                byteBuffer.position() + pLength);
        BigInteger p = new BigInteger(pBytes);

        // read q
        int qLengthPos = pLengthPos + pLength + LENGTH_LENGTH;
        byteBuffer.position(qLengthPos);
        int qLength = byteBuffer.getInt();
        byte[] qBytes = Arrays.copyOfRange(
                keyBytes,
                byteBuffer.position(),
                byteBuffer.position() + qLength);
        BigInteger q = new BigInteger(qBytes);

        // read g
        int gLengthPos = qLengthPos + qLength + LENGTH_LENGTH;
        byteBuffer.position(gLengthPos);
        int gLength = byteBuffer.getInt();
        byte[] gBytes = Arrays.copyOfRange(
                keyBytes,
                byteBuffer.position(),
                byteBuffer.position() + gLength);
        BigInteger g = new BigInteger(gBytes);

        // read y
        int yLengthPos = gLengthPos + gLength + LENGTH_LENGTH;
        byteBuffer.position(yLengthPos);
        int yLength = byteBuffer.getInt();
        byte[] yBytes = Arrays.copyOfRange(
                keyBytes,
                byteBuffer.position(),
                byteBuffer.position() + yLength);
        BigInteger y = new BigInteger(yBytes);

        return createPubKeyDsa(y, p, q, g);
    }

    protected static PublicKey createPubKeyDsa(
            BigInteger y, BigInteger p, BigInteger q, BigInteger g)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        DSAPublicKeySpec keySpec = new DSAPublicKeySpec(y, p, q, g);
        KeyFactory kf = KeyFactory.getInstance("DSA");
        return kf.generatePublic(keySpec);
    }
}
