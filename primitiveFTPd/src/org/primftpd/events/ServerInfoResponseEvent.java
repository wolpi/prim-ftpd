package org.primftpd.events;

/**
 * Eventbus event to respond to {@link ServerInfoRequestEvent}.
 */
public class ServerInfoResponseEvent {
    private final String quickShareFilename;

    public ServerInfoResponseEvent(String quickShareFilename) {
        this.quickShareFilename = quickShareFilename;
    }

    public String getQuickShareFilename() {
        return quickShareFilename;
    }
}
