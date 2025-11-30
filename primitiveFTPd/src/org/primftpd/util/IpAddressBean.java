package org.primftpd.util;

public class IpAddressBean {
    private final String ipAddress;
    private final String interfaceName;
    private final String displayName;

    public IpAddressBean(String ipAddress, String interfaceName, String displayName) {
        this.ipAddress = ipAddress;
        this.interfaceName = interfaceName;
        this.displayName = displayName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
