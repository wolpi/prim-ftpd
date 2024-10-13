package org.primftpd.events;

import java.util.Date;

import androidx.annotation.NonNull;

public class ClientActionEvent {
    public enum Storage {
        FS,
        ROOT,
        SAF,
        ROSAF,
        QUICKSHARE
    }

    public enum Protocol {
        FTP,
        SFTP
    }

    public enum ClientAction {
        LIST_DIR,
        CREATE_DIR,
        RENAME,
        DELETE,
        DOWNLOAD,
        UPLOAD,

        ERROR,
    }

    private final Storage storage;
    private final Protocol protocol;
    private final ClientAction clientAction;
    private final Date timestamp;
    private final String clientIp;
    private final String path;

    private final String error;

    public ClientActionEvent(
            Storage storage,
            Protocol protocol,
            ClientAction clientAction,
            Date timestamp,
            String clientIp,
            String path,
            String error) {
        this.storage = storage;
        this.protocol = protocol;
        this.clientAction = clientAction;
        this.timestamp = timestamp;
        this.clientIp = clientIp;
        this.path = path;
        this.error = error;
    }

    @NonNull
    @Override
    public String toString() {
        String str = "ClientActionEvent{" +
                "storage=" + storage +
                ", protocol=" + protocol +
                ", timestamp=" + timestamp +
                ", clientIp='" + clientIp + '\'' +
                ", clientAction=" + clientAction +
                ", path='" + path + '\'';
        if (error != null) {
            str = str + ", error=" + error;
        }
        return str + '}';
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

    public String getError() {
        return error;
    }
}
