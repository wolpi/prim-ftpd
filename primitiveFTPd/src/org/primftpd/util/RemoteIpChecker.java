package org.primftpd.util;

import org.primftpd.prefs.PrefsBean;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class RemoteIpChecker {

    public static boolean ipAllowed(SocketAddress remoteAddress, PrefsBean prefs, Logger logger) {
        String pattern = prefs.getAllowedIpsPattern();
        if (StringUtils.isNotBlank(pattern)) {
            if (remoteAddress instanceof InetSocketAddress) {
                InetSocketAddress inetSock = (InetSocketAddress) remoteAddress;
                String ip = inetSock.getAddress().toString();
                ip = IpAddressProvider.extractIp(ip);
                boolean allowed = doCheck(ip, pattern);
                logger.info("[checking whether remote ip is allowed] remote ip: '{}', pattern: '{}', allowed? '{}'",
                        new Object[]{ip, pattern, allowed});
                return allowed;
            } else {
                logger.warn("cannot check remote IP, bad class. remote addr: {}, class: {}",
                        remoteAddress.toString(),
                        remoteAddress.getClass().getSimpleName());
            }
        } else {
            logger.debug("not checking ip pattern: '{}'", pattern);
        }
        return true;
    }

    private static boolean doCheck(String ip, String pattern) {
        if (pattern.endsWith("*")) {
            return ip.startsWith(pattern.substring(0, pattern.length()-1));
        }
        return ip.equals(pattern);
    }
}
