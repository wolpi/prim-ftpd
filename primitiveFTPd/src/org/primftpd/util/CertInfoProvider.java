package org.primftpd.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CertInfoProvider
{
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public Date validUntil(FileInputStream fis) {
		try {
			X509Certificate cert = readCert(fis);
			return cert.getNotAfter();
		} catch (Exception e) {
			logger.error("could not read cert", e);
		}
		return null;
	}

	public String fingerprint(FileInputStream fis, String hashAlgo) {
		try {
			// TODO figure out what clients show as fingerprint
			X509Certificate cert = readCert(fis);
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
		return cert;
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
