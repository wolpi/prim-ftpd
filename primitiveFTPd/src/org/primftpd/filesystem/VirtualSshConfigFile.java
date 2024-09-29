package org.primftpd.filesystem;

import org.apache.sshd.common.Session;
import org.primftpd.services.PftpdService;

public class VirtualSshConfigFile extends VirtualConfigFile {

    private final Session session;

    public VirtualSshConfigFile(PftpdService pftpdService, Session session) {
        super(pftpdService);
        this.session = session;
    }

    @Override
    public String getClientIp() {
        return SshUtils.getClientIp(session);
    }
}
