package org.primftpd.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;

public class KeyGenerator
{
	public static final String KEY_ALGO = "RSA";
	public static final int KEY_SIZE = 2048;

	public void generate(FileOutputStream pubKeyFos, FileOutputStream privKeyFos)
		throws IOException, NoSuchAlgorithmException
	{
		SecureRandom sr = new SecureRandom();

		KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_ALGO);
		keyGen.initialize(KEY_SIZE, sr);
		KeyPair keypair = keyGen.generateKeyPair();
		PrivateKey privKey = keypair.getPrivate();
		PublicKey pubKey = keypair.getPublic();

		pubKeyFos.write(pubKey.getEncoded());

    	PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(privKey.getEncoded());
		privKeyFos.write(privKeySpec.getEncoded());
	}
}
