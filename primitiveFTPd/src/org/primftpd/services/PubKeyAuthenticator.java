package org.primftpd.services;

import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.util.List;

public class PubKeyAuthenticator implements PublickeyAuthenticator {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final List<PublicKey> pubKeys;

    public PubKeyAuthenticator(List<PublicKey> pubKeys) {
        this.pubKeys = pubKeys;
    }

    @Override
    public boolean authenticate(String username, PublicKey clientKey, ServerSession session) {
        logger.debug("attempting pub key auth, user: {}, client key class: '{}'",
                username, clientKey.getClass().getName());
        // never mind username
        for (PublicKey serverKey : pubKeys) {
            boolean keyEquals = keysEqual(serverKey, clientKey);
            logger.debug("pub key auth, success: {}, server key class '{}'",
                    keyEquals, serverKey.getClass().getName());
            if (keyEquals) {
                return true;
            }
        }
        return false;
    }

    private static boolean keysEqual(PublicKey serverKey, PublicKey clientKey) {
        // Quick sanity checks
        if (serverKey == clientKey) {
            return true;
        }
        if (serverKey == null || clientKey == null) {
            return false;
        }
        // serverKey is an object from bouncy castle, see org.primftpd.pojo.KeyParser
        // clientKey is often another vendor. So we can't do a simple equals (at least not always).

        // Sanity check: their algorithms must match
        if (!serverKey.getAlgorithm().equals(clientKey.getAlgorithm())) {
            return false;
        }

        try {
            // Translate BOTH keys and THAN compare
            KeyFactory factory = KeyFactory.getInstance(serverKey.getAlgorithm());
            return factory.translateKey(serverKey).equals(factory.translateKey(clientKey));

        } catch (Exception e) {
            return false;
        }
    }
}
