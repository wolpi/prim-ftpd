package org.primftpd.services;

import android.content.Context;

import org.primftpd.events.ClientActionEvent;
import org.primftpd.prefs.PrefsBean;

public interface PftpdService {

    public void postClientAction(
            ClientActionEvent.Storage storage,
            ClientActionEvent.ClientAction clientAction,
            String clientIp,
            String path,
            String error);

    public PrefsBean getPrefsBean();

    public Context getContext();
}
