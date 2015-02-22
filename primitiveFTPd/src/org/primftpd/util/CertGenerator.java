package org.primftpd.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.x509.X509V3CertificateGenerator;

public class CertGenerator
{
	public static final String KEY_ALGO = "RSA";
	public static final String SIG_ALGO = "SHA256WithRSAEncryption";
	public static final int KEY_SIZE = 2048;
	public static final long YEARS_3 = 1000L * 60 * 60 * 24 * 365 * 3;

	public void generate(FileOutputStream certFos, FileOutputStream privKeyFos)
		throws IOException, InvalidKeyException,
		SecurityException, SignatureException, NoSuchAlgorithmException,
		DataLengthException, CryptoException, KeyStoreException,
		NoSuchProviderException, CertificateException,
		InvalidKeySpecException
	{
		SecureRandom sr = new SecureRandom();

		KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_ALGO);
		keyGen.initialize(KEY_SIZE, sr);
		KeyPair keypair = keyGen.generateKeyPair();
		PrivateKey privKey = keypair.getPrivate();
		PublicKey pubKey = keypair.getPublic();

		// TODO use X509v3CertificateBuilder
		X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();

    	Date startDate = new Date();
    	Date expiryDate = new Date(startDate.getTime() + YEARS_3);

		certGen.setSerialNumber(BigInteger.ONE);
        certGen.setIssuerDN(new X509Principal("CN=pFTPd, OU=None, O=None L=None, C=None"));
        certGen.setNotBefore(startDate);
        certGen.setNotAfter(expiryDate);
        certGen.setSubjectDN(new X509Principal("CN=pFTPd, OU=None, O=None L=None, C=None"));

        certGen.setPublicKey(pubKey);
        certGen.setSignatureAlgorithm(SIG_ALGO);

        X509Certificate cert = certGen.generateX509Certificate(privKey);

        byte[] encoded = cert.getEncoded();
        certFos.write(encoded);

    	PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privKey.getEncoded());
		privKeyFos.write(keySpec.getEncoded());
	}
}
