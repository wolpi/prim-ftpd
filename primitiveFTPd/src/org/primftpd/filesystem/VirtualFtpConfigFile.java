package org.primftpd.filesystem;

import org.apache.ftpserver.ftplet.User;
import org.primftpd.services.PftpdService;

public class VirtualFtpConfigFile extends VirtualConfigFile<VirtualFtpFileSystemView> {

    private final User user;

    public VirtualFtpConfigFile(VirtualFtpFileSystemView fileSystemView, User user) {
        super(fileSystemView);
        this.user = user;
    }

    @Override
    public String getClientIp() {
        return FtpUtils.getClientIp(user);
    }
}
