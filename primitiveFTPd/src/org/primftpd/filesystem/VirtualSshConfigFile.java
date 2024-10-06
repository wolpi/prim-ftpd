package org.primftpd.filesystem;

import org.apache.sshd.common.Session;
import org.primftpd.services.PftpdService;

public class VirtualSshConfigFile extends VirtualConfigFile<VirtualSshFileSystemView> {

    private final Session session;

    public VirtualSshConfigFile(VirtualSshFileSystemView fileSystemView, Session session) {
        super(fileSystemView);
        this.session = session;
    }

    @Override
    public String getClientIp() {
        return SshUtils.getClientIp(session);
    }
}
