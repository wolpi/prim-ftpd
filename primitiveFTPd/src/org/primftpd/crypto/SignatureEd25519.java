package org.primftpd.crypto;

import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.Signature;
import org.apache.sshd.common.signature.AbstractSignature;
import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.primftpd.pojo.KeyParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class SignatureEd25519 extends AbstractSignature {

    public static class Factory implements NamedFactory<Signature> {

        public String getName() {
            return KeyParser.NAME_ED25519;
        }

        public Signature create() {
            return new SignatureEd25519(getName());
        }

    }

    private SignatureEd25519(String name) {
        super(name);
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Signer signer = new Ed25519Signer();

    // Parameters are derived from the standardized encoding of the keys (X.509 / PKCS#8).
    // Reading private fields of the key classes of bouncy castle by reflection breaks as soon as
    // bouncy castle hands out a subclass, which it does for newer java versions (BC15EdDSAPublicKey).
    private Ed25519PublicKeyParameters extractPubKeyParas(PublicKey pubkey) throws IOException {
        AsymmetricKeyParameter keyParameter = PublicKeyFactory.createKey(pubkey.getEncoded());
        if (!(keyParameter instanceof Ed25519PublicKeyParameters)) {
            throw new IOException("can not extract pub key paras from key of type: " + pubkey.getClass().getName());
        }
        return (Ed25519PublicKeyParameters) keyParameter;
    }

    private Ed25519PrivateKeyParameters extractPrvKeyParas(PrivateKey prvkey) throws IOException {
        AsymmetricKeyParameter keyParameter = PrivateKeyFactory.createKey(prvkey.getEncoded());
        if (!(keyParameter instanceof Ed25519PrivateKeyParameters)) {
            throw new IOException("can not extract prv key paras from key of type: " + prvkey.getClass().getName());
        }
        return (Ed25519PrivateKeyParameters) keyParameter;
    }

    public void init(PublicKey pubkey, PrivateKey prvkey) {
        logger.trace("SignatureEd25519.init({}, {})",
                (pubkey != null ? pubkey.getClass().getName() : null),
                (prvkey != null ? prvkey.getClass().getName() : null));
        Ed25519PublicKeyParameters chipherParasPub = null;
        Ed25519PrivateKeyParameters chipherParasPrv = null;
        try {
            if (pubkey != null) {
                chipherParasPub = extractPubKeyParas(pubkey);
            }
            if (prvkey != null) {
                chipherParasPrv = extractPrvKeyParas(prvkey);
            }
        } catch (Exception e) {
            logger.error("could not transform ed25519 key", e);
        }
        if (chipherParasPrv != null) {
            signer.init(true, chipherParasPrv);
        } else {
            signer.init(false, chipherParasPub);
        }
    }

    public void update(byte[] H, int off, int len) {
        signer.update(H, off, len);
    }

    public boolean verify(byte[] sig) {
        sig = extractSig(sig);
        return signer.verifySignature(sig);
    }

    public byte[] sign() throws Exception {
        return signer.generateSignature();
    }
}
