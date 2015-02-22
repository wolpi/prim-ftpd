package org.primftpd.util;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.Locale;

import org.apache.ftpserver.util.IoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CertInfoProvider
{
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public Date validUntil(X509Certificate cert) {
		return cert.getNotAfter();
	}

	public String fingerprint(X509Certificate cert, String hashAlgo) {
		try {
			MessageDigest md = MessageDigest.getInstance(hashAlgo);
			PublicKey publicKey = cert.getPublicKey();
			md.update(publicKey.getEncoded());
			byte[] fingerPrintBytes = md.digest();
			return beautify(fingerPrintBytes);
		} catch (Exception e) {
			logger.error("could not read cert: " + e.getMessage(), e);
		}
		return null;
	}

	public X509Certificate readCert(FileInputStream fis)
		throws CertificateException, IOException
	{
		CertificateFactory x509CertFact = CertificateFactory.getInstance("X.509");
		X509Certificate cert = (X509Certificate)x509CertFact.generateCertificate(fis);
		if (logger.isDebugEnabled()) {
			PublicKey publicKey = cert.getPublicKey();
			byte[] pubkeyBytes = publicKey.getEncoded();
			logger.debug(
				"public key num bytes: {}",
				Integer.valueOf(pubkeyBytes.length));
		}
		return cert;
	}

	private static final int BUFFER_SIZE = 4096;

	public PrivateKey readPrivatekey(FileInputStream fis)
		throws NoSuchAlgorithmException, InvalidKeySpecException,
		IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		IoUtils.copy(fis, baos, BUFFER_SIZE);
		byte[] privKeyBytes = baos.toByteArray();
		logger.debug(
			"private key num bytes: {}",
			Integer.valueOf(privKeyBytes.length));
		PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privKeyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance(CertGenerator.KEY_ALGO);
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
			}
			if (i > 0 && i % 10 == 0) {
				// force line breaks in UI
				fingerPrint.append("<br/>");
			}
		}
		return fingerPrint.toString();
	}
}
