package org.primftpd.filesystem;

import org.apache.ftpserver.ftplet.User;
import org.primftpd.services.FtpUserWithIp;
import org.primftpd.util.IpAddressProvider;

public class FtpUtils {
    static String getClientIp(User user) {
        if (user instanceof FtpUserWithIp) {
            String remoteIp = ((FtpUserWithIp) user).getRemoteIp();
            if (remoteIp != null) {
                return IpAddressProvider.extractIp(remoteIp);
            }
        }
        return "unknown ip";
    }
}
