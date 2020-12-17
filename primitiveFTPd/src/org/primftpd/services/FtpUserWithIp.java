package org.primftpd.services;

import org.apache.ftpserver.usermanager.impl.BaseUser;

public class FtpUserWithIp extends BaseUser {
    private final String remoteIp;

    public FtpUserWithIp(String remoteIp) {
        super();
        this.remoteIp = remoteIp;
    }

    public String getRemoteIp() {
        return remoteIp;
    }
}
