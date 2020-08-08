package org.primftpd.util;

import android.util.Base64;

import org.apache.ftpserver.util.IoUtils;
import org.primftpd.pojo.Base64Decoder;
import org.primftpd.pojo.KeyParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class KeyInfoProvider
{
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public FingerprintBean fingerprint(byte[] pubKeyEnc, String hashAlgo) {
		try {
			MessageDigest md = MessageDigest.getInstance(hashAlgo);
			md.update(pubKeyEnc);
			byte[] fingerPrintBytes = md.digest();
			String base64 = Base64.encodeToString(fingerPrintBytes, Base64.NO_PADDING);
			String beautified = beautify(fingerPrintBytes);
			return new FingerprintBean(beautified, base64);
		} catch (Exception e) {
			logger.error("could not read key: " + e.getMessage(), e);
		}
		return null;
	}

	private static final int BUFFER_SIZE = 4096;

	public PublicKey readPublicKey(FileInputStream fis)
		throws NoSuchAlgorithmException, InvalidKeySpecException,
		IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		IoUtils.copy(fis, baos, BUFFER_SIZE);
		byte[] pubKeyBytes = baos.toByteArray();
		X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(pubKeyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance(KeyGenerator.KEY_ALGO);
		PublicKey publicKey = keyFactory.generatePublic(pubKeySpec);
		return publicKey;
	}

	public PrivateKey readPrivatekey(FileInputStream fis)
		throws NoSuchAlgorithmException, InvalidKeySpecException,
		IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		IoUtils.copy(fis, baos, BUFFER_SIZE);
		byte[] privKeyBytes = baos.toByteArray();
		PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privKeyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance(KeyGenerator.KEY_ALGO);
		PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
		return privateKey;
	}

	protected String beautify(byte[] fingerPrintBytes)
	{
		StringBuilder fingerPrint = new StringBuilder();
		for (int i=0; i<fingerPrintBytes.length; i++) {
			byte b = fingerPrintBytes[i];
			String hexString = Integer.toHexString(b);
			if (hexString.length() > 2) {
				hexString = hexString.substring(
				hexString.length() - 2,
				hexString.length());
			} else if (hexString.length() < 2) {
				hexString = "0" + hexString;
			}
			fingerPrint.append(hexString.toUpperCase(Locale.ENGLISH));
			if (i != fingerPrintBytes.length -1) {
				fingerPrint.append(":");

				if ((i + 1) % 10 == 0) {
					// force line breaks in UI
					fingerPrint.append("\n");
				}
			}
		}
		return fingerPrint.toString();
	}

	public byte[] encodeAsSsh(RSAPublicKey pubKey)
		throws IOException
	{
		ByteArrayOutputStream buf = new ByteArrayOutputStream();

		byte[] name = "ssh-rsa".getBytes("US-ASCII");
		writeKeyPart(name, buf);

		writeKeyPart(pubKey.getPublicExponent().toByteArray(), buf);
		writeKeyPart(pubKey.getModulus().toByteArray(), buf);

		return buf.toByteArray();
	}

	private void writeKeyPart(byte[] bytes, OutputStream os)
		throws IOException
	{
		for (int shift = 24; shift >= 0; shift -= 8) {
			os.write((bytes.length >>> shift) & 0xFF);
		}
		os.write(bytes);
	}

	public List<PublicKey> readKeyAuthKeys(String path, boolean ignoreErrors)
	{
		List<PublicKey> keys = null;
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(path);
			keys = KeyParser.parsePublicKeys(
					fis,
					new Base64Decoder() {
						@Override
						public byte[] decode(String str) {
							return Base64.decode(str, Base64.DEFAULT);
						}
					});

		} catch (Exception e) {
			if (!ignoreErrors) {
				logger.error("could not read key auth keys", e);
			}
		} finally {
			try {
				if (fis != null) {
					fis.close();
				}
			} catch (IOException e) {
				if (!ignoreErrors) {
					logger.error("could not close key auth keys file", e);
				}
			}
		}
		return keys != null ? keys : Collections.<PublicKey>emptyList();
	}
}
