package org.primftpd.filesystem;

import org.apache.ftpserver.ftplet.User;
import org.primftpd.services.PftpdService;

public class VirtualFtpConfigFile extends VirtualConfigFile {

    private final User user;

    public VirtualFtpConfigFile(PftpdService pftpdService, User user) {
        super(pftpdService);
        this.user = user;
    }

    @Override
    public String getClientIp() {
        return FtpUtils.getClientIp(user);
    }
}
