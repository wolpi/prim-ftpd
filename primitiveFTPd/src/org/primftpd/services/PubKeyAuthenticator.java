package org.primftpd.services;

import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        // serverKey is an object from bouncy castle, see org.primftpd.pojo.KeyParser
        // clientKey is an object from conscrypt (at least since api-level 28)
        // fortunately conscrypt's equals() method supports comparision with bouncy castle objects
        return clientKey.equals(serverKey);
    }
}
