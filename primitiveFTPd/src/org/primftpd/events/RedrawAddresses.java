package org.primftpd.events;

/**
 * Eventbus event to indicate IP-Addresses shall be re-drawn.
 */
public class RedrawAddresses {
    private final String chosenIp;

    public RedrawAddresses(String chosenIp) {
        this.chosenIp = chosenIp;
    }

    public String getChosenIp() {
        return chosenIp;
    }
}
