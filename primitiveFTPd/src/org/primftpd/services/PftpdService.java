package org.primftpd.services;

import org.primftpd.events.ClientActionEvent;

public interface PftpdService {

    public void postClientAction(
            ClientActionEvent.Storage storage,
            ClientActionEvent.ClientAction clientAction,
            String clientIp,
            String path);
}
