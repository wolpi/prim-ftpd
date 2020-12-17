package org.primftpd.events;

import java.util.Date;

public class ClientActionEvent {
    public static enum Storage {
        FS,
        ROOT,
        SAF,
        ROSAF,
        QUICKSHARE
    }

    public static enum Protocol {
        FTP,
        SFTP
    }

    public static enum ClientAction {
        LIST_DIR,
        CREATE_DIR,
        RENAME,
        DELETE,
        DOWNLOAD,
        UPLOAD
    }

    private final Storage storage;
    private final Protocol protocol;
    private final ClientAction clientAction;
    private final Date timestamp;
    private final String clientIp;
    private final String path;

    public ClientActionEvent(Storage storage, Protocol protocol, ClientAction clientAction, Date timestamp, String clientIp, String path) {
        this.storage = storage;
        this.protocol = protocol;
        this.clientAction = clientAction;
        this.timestamp = timestamp;
        this.clientIp = clientIp;
        this.path = path;
    }

    @Override
    public String toString() {
        return "ClientActionEvent{" +
                "storage=" + storage +
                ", protocol=" + protocol +
                ", timestamp=" + timestamp +
                ", clientIp='" + clientIp + '\'' +
                ", clientAction=" + clientAction +
                ", path='" + path + '\'' +
                '}';
    }

    public Storage getStorage() {
        return storage;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public ClientAction getClientAction() {
        return clientAction;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getPath() {
        return path;
    }
}
