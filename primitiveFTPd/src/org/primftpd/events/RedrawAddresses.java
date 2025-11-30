package org.primftpd.events;

/**
 * Eventbus event to indicate IP-Addresses shall be re-drawn.
 */
public class RedrawAddresses {
    private final boolean chooseBindIp;
    private final String chosenIp;

    public RedrawAddresses(boolean chooseBindIp, String chosenIp) {
        this.chooseBindIp = chooseBindIp;
        this.chosenIp = chosenIp;
    }

    public boolean isChooseBindIp() {
        return chooseBindIp;
    }

    public String getChosenIp() {
        return chosenIp;
    }
}
