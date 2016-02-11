package org.primftpd.events;

import org.primftpd.services.AbstractServerService;

public class ServerStatusUpdateEvent {
    public static final ServerStatusUpdateEvent STARTING = new ServerStatusUpdateEvent(AbstractServerService.MSG_START);
    public static final ServerStatusUpdateEvent STOPPING = new ServerStatusUpdateEvent(AbstractServerService.MSG_STOP);

    private final int state;

    private ServerStatusUpdateEvent(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }
}
