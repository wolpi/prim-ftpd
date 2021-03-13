package org.primftpd.services;

import org.primftpd.events.ClientActionEvent;
import org.primftpd.prefs.PrefsBean;

public interface PftpdService {

    public void postClientAction(
            ClientActionEvent.Storage storage,
            ClientActionEvent.ClientAction clientAction,
            String clientIp,
            String path);

    public PrefsBean getPrefsBean();
}
