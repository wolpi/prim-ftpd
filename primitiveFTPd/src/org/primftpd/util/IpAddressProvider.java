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
        String ip = remoteAddress;
        if (remoteAddress.charAt(0) == '/') {
            ip = remoteAddress.substring(1);
        }
        int indexOfColon = ip.indexOf(':');
        if (indexOfColon > 0) {
            ip = ip.substring(0, indexOfColon);
        }
        return ip;
    }

    public List<String> ipAddressTexts(Context ctxt, boolean verbose, boolean isLeftToRight) {
        List<String> result = new ArrayList<>();
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

                    logger.debug("addr: '{}', iface name: '{}', disp name: '{}', loopback: '{}'",
                            new Object[]{
                                    inetAddr,
                                    ifaceName,
                                    ifaceDispName,
                                    inetAddr.isLoopbackAddress()});

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
                    if (verbose) {
                        String verboseText =  "(" + ifaceDispName + ")";
                        if (isLeftToRight) {
                            displayText += " " + verboseText;
                        } else {
                            displayText = verboseText + " " + displayText;
                        }
                    }

                    result.add(displayText);
                }
            }
        } catch (SocketException e) {
            logger.info("exception while iterating network interfaces", e);

            String msg = ctxt.getText(R.string.ifacesError) + e.getLocalizedMessage();
            Toast.makeText(ctxt, msg, Toast.LENGTH_SHORT).show();
        }
        return result;
    }

    public boolean isIpv6(String address) {
        return address.contains(":");
    }
}
