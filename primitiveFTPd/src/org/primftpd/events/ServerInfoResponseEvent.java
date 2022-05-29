package org.primftpd.events;

/**
 * Eventbus event to respond to {@link ServerInfoRequestEvent}.
 */
public class ServerInfoResponseEvent {
    private final int quickShareNumberOfFiles;

    public ServerInfoResponseEvent(int quickShareNumberOfFiles) {
        this.quickShareNumberOfFiles = quickShareNumberOfFiles;
    }

    public int getQuickShareNumberOfFiles() {
        return quickShareNumberOfFiles;
    }
}
