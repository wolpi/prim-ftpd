package org.primftpd.util;

import android.content.Context;
import android.widget.Toast;

import org.primftpd.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class IpAddressProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static String extractIp(String remoteAddress) {
        if (remoteAddress == null || remoteAddress.isEmpty()) {
            return "";
        }
        String ip = remoteAddress;

        // in some cases we get ip-address with leading / or even "hostname/ip", just cut it off
        int indexOfSlash = ip.indexOf('/');
        if (indexOfSlash >= 0) {
            ip = ip.substring(indexOfSlash + 1);
        }
        if (ip.isEmpty()) {
            return "";
        }

        // if square brackets are present we can reliably say it is IPv6
        if (ip.charAt(0) == '[') {
            int index = ip.indexOf(']');
            if (index > 0) {
                ip = ip.substring(0, index + 1);
            }
            return ip;
        }

        // more than one colon can only be IPv6 (without brackets there is no port)
        int indexOfColon = ip.indexOf(':');
        if (indexOfColon != ip.lastIndexOf(':')) {
            return ip;
        }

        // remove port for IPv4
        if (indexOfColon > 0) {
            ip = ip.substring(0, indexOfColon);
        }
        return ip;
    }

    public List<IpAddressBean> ipAddressTexts(Context ctxt, boolean verbose, boolean isLeftToRight) {
        List<IpAddressBean> result = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                String ifaceDispName = iface.getDisplayName();
                String ifaceName = iface.getName();
                Enumeration<InetAddress> inetAddrs = iface.getInetAddresses();

                while (inetAddrs.hasMoreElements()) {
                    InetAddress inetAddr = inetAddrs.nextElement();
                    String hostAddr = inetAddr.getHostAddress();
                    if (hostAddr == null) {
                        hostAddr = "";
                    }

                    logger.debug("addr: '{}', iface name: '{}', disp name: '{}', loopback: '{}'",
                        inetAddr,
                        ifaceName,
                        ifaceDispName,
                        inetAddr.isLoopbackAddress());

                    if (inetAddr.isLoopbackAddress()) {
                        continue;
                    }
                    if (inetAddr.isAnyLocalAddress()) {
                        continue;
                    }

                    if (hostAddr.contains("::")) {
                        // Don't include the raw encoded names. Just the raw IP addresses.
                        logger.debug("Skipping IPv6 address '{}'", hostAddr);
                        continue;
                    }

                    if (!verbose && !ifaceDispName.startsWith("wlan")) {
                        continue;
                    }

                    String displayText = hostAddr;
                    String verboseText =  "(" + ifaceDispName + ")";
                    if (isLeftToRight) {
                        displayText += " " + verboseText;
                    } else {
                        displayText = verboseText + " " + displayText;
                    }

                    result.add(new IpAddressBean(hostAddr, ifaceDispName, displayText));
                }
            }
        } catch (SocketException e) {
            logger.info("exception while iterating network interfaces", e);

            String msg = ctxt.getText(R.string.ifacesError) + e.getLocalizedMessage();
            Toast.makeText(ctxt, msg, Toast.LENGTH_SHORT).show();
        }
        return result;
    }

    public boolean isIpAvail(String ip) {
        if (ip == null) {
            return true;
        }
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                Enumeration<InetAddress> inetAddrs = iface.getInetAddresses();

                while (inetAddrs.hasMoreElements()) {
                    InetAddress inetAddr = inetAddrs.nextElement();
                    String hostAddr = inetAddr.getHostAddress();
                    if (ip.equals(hostAddr)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            logger.info("could not check ip", e);
        }
        return false;
    }

    public boolean isIpv6(String address) {
        return address.contains(":");
    }
}
