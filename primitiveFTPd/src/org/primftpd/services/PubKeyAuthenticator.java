package org.primftpd.services;

import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
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

    private boolean keysEqual(PublicKey serverKey, PublicKey clientKey) {
        // Quick sanity checks
        if (serverKey == clientKey) {
            return true;
        }
        if (serverKey == null || clientKey == null) {
            return false;
        }
        // Sanity check: their algorithms must match
        if (!serverKey.getAlgorithm().equalsIgnoreCase(clientKey.getAlgorithm())) {
            return false;
        }

        // use constant-time compare
        return MessageDigest.isEqual(serverKey.getEncoded(), clientKey.getEncoded());
    }
}
