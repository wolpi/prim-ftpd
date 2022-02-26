package org.primftpd.crypto;

import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.Signature;
import org.apache.sshd.common.signature.AbstractSignature;
import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPublicKey;
import org.primftpd.pojo.KeyParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
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

    private Ed25519PublicKeyParameters extractPubKeyParas(PublicKey pubkey)
            throws NoSuchFieldException, IllegalAccessException {
        if (!(pubkey instanceof BCEdDSAPublicKey)) {
            throw new RuntimeException("can not extract pub key paras from key of type: " + pubkey.getClass().getName());
        }
        Class<? extends PublicKey> pubkeyClass = pubkey.getClass();
        Field eddsaPublicKeyFiled = pubkeyClass.getDeclaredField("eddsaPublicKey");
        eddsaPublicKeyFiled.setAccessible(true);
        Object eddsaPublicKey = eddsaPublicKeyFiled.get(pubkey);
        return (Ed25519PublicKeyParameters)eddsaPublicKey;
    }

    private Ed25519PrivateKeyParameters extractPrvKeyParas(PrivateKey prvkey)
            throws NoSuchFieldException, IllegalAccessException {
        if (!(prvkey instanceof BCEdDSAPrivateKey)) {
            throw new RuntimeException("can not extract prv key paras from key of type: " + prvkey.getClass().getName());
        }
        Class<? extends PrivateKey> prvkeyClass = prvkey.getClass();
        Field eddsaPrivateKeyFiled = prvkeyClass.getDeclaredField("eddsaPrivateKey");
        eddsaPrivateKeyFiled.setAccessible(true);
        Object eddsaPrivateKey = eddsaPrivateKeyFiled.get(prvkey);
        return (Ed25519PrivateKeyParameters)eddsaPrivateKey;
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
