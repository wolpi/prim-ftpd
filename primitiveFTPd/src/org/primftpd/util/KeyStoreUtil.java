package org.primftpd.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.ftpserver.ssl.ClientAuth;
import org.apache.ftpserver.ssl.SslConfiguration;
import org.apache.ftpserver.ssl.impl.DefaultSslConfiguration;
import org.primftpd.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.res.Resources;

public class KeyStoreUtil
{
	private static final Logger log = LoggerFactory.getLogger(KeyStoreUtil.class);

	private static final String KEY_STORE_PASS = "primftpd";
	private static final String KEY_ALIAS = "self-signed";

	private static final String KEY_STORE_TYPE = "BKS";
	private static final String KEY_ALGORITHM = "X509";

	// see
	// http://mina.apache.org/ftpserver/listeners.html

	//private static final String SSL_PROTOCOL_NAME = "TLS";
	private static final String SSL_PROTOCOL_NAME = "SSL";

	private static final String[] CIPHER_SUITES = null; /*{
		"TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
		"TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
		"TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
		"TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
		"TLS_DH_anon_WITH_AES_128_CBC_SHA",
		"TLS_DH_anon_WITH_AES_256_CBC_SHA",
//		"TLS_KRB5_EXPORT_WITH_DES_CBC_40_MD5",
//		"TLS_KRB5_EXPORT_WITH_DES_CBC_40_SHA",
//		"TLS_KRB5_EXPORT_WITH_RC4_40_MD5",
//		"TLS_KRB5_EXPORT_WITH_RC4_40_SHA",
//		"TLS_KRB5_WITH_3DES_EDE_CBC_MD5",
//		"TLS_KRB5_WITH_3DES_EDE_CBC_SHA",
//		"TLS_KRB5_WITH_DES_CBC_MD5",
//		"TLS_KRB5_WITH_DES_CBC_SHA",
//		"TLS_KRB5_WITH_RC4_128_MD5",
//		"TLS_KRB5_WITH_RC4_128_SHA",
		"TLS_RSA_WITH_AES_128_CBC_SHA",
	};*/

	public static KeyStore loadKeyStore(Resources resources)
	{
		InputStream inputStream = resources.openRawResource(R.raw.ssl_keystore);
		try {
			KeyStore ks = KeyStore.getInstance(KEY_STORE_TYPE);
	        ks.load(inputStream, KEY_STORE_PASS.toCharArray());
	        return ks;

		} catch (KeyStoreException e) {
			log.error("could not load key store", e);
		} catch (NoSuchAlgorithmException e) {
			log.error("could not load key store", e);
		} catch (CertificateException e) {
			log.error("could not load key store", e);
		} catch (IOException e) {
			log.error("could not load key store", e);
		}
        return null;
	}

	public static SslConfiguration createSslConfiguration(KeyStore ks)
	{
		try {
			KeyManagerFactory keyManagerFactory =
					KeyManagerFactory.getInstance(KEY_ALGORITHM);
	        keyManagerFactory.init(ks, KEY_STORE_PASS.toCharArray());

	        String trustMgmDefAlgo = TrustManagerFactory.getDefaultAlgorithm();
	        TrustManagerFactory trustMgrFactory =
	        		TrustManagerFactory.getInstance(trustMgmDefAlgo);
	        trustMgrFactory.init(ks);

	        return new DefaultSslConfiguration(
					keyManagerFactory,
					trustMgrFactory,
					ClientAuth.NONE,
					SSL_PROTOCOL_NAME,
					CIPHER_SUITES,
					KEY_ALIAS);
		} catch (NoSuchAlgorithmException e) {
			log.error("could not create ssl config", e);
		} catch (KeyStoreException e) {
			log.error("could not create ssl config", e);
		} catch (UnrecoverableKeyException e) {
			log.error("could not create ssl config", e);
		}
		return null;
	}

	public static String calcKeyFingerprint(KeyStore ks, String hashAlgo)
	{
		try {
			Certificate cert = ks.getCertificate(KEY_ALIAS);
			MessageDigest md = MessageDigest.getInstance(hashAlgo);
			md.update(cert.getEncoded());
			byte[] fingerPrintBytes = md.digest();

			StringBuilder fingerPrint = new StringBuilder();
			for (int i=0; i<fingerPrintBytes.length; i++) {
				byte b = fingerPrintBytes[i];
				String hexString = Integer.toHexString(b);

				// beautify
				if (hexString.length() > 2) {
					hexString = hexString.substring(
							hexString.length() - 2,
							hexString.length());
				} else if (hexString.length() < 2) {
					hexString = "0" + hexString;
				}

				fingerPrint.append(hexString.toUpperCase());

				if (i != fingerPrintBytes.length -1) {
					fingerPrint.append(":");
				}

				if (i > 0 && i % 10 == 0) {
					// force line breaks in UI
					fingerPrint.append("<br/>");
				}
			}
			return fingerPrint.toString();

		} catch (KeyStoreException e) {
			log.error("could not calc finger print", e);
		} catch (NoSuchAlgorithmException e) {
			log.error("could not calc finger print", e);
		} catch (CertificateEncodingException e) {
			log.error("could not calc finger print", e);
		}
		return null;
	}
}
