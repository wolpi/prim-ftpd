package org.primftpd.filesystem;

import org.primftpd.events.ClientActionEvent;
import org.primftpd.services.PftpdService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Provides a virtual file to allow clients to read some of server's config.
 *
 * To support "prim-sync", see <a href="https://github.com/lmagyar/prim-sync/">prim-sync</a>
 *
 * For more details see <a href="https://github.com/wolpi/prim-ftpd/pull/378">PR</a>
 */
public abstract class VirtualConfigFile extends AbstractFile {

    public static final String NAME = "primftpd.config";
    public static final String ABS_PATH = "/" + NAME;

    private final String content;

    public VirtualConfigFile(
            PftpdService pftpdService) {
        super(
                ABS_PATH,
                NAME,
                0,
                0,
                true,
                true,
                false,
                pftpdService);
        this.content = getContent();
        size = content.length();
    }

    private String getContent() {
        return
            "{" +
                "\"announceName\":\"" + pftpdService.getPrefsBean().getAnnounceName().replace("\"", "\\\"") + "\"" +
            "}";
    }

    public ClientActionEvent.Storage getClientActionStorage() {
        return ClientActionEvent.Storage.CONFIG;
    }

    public boolean isFile() {
        return true;
    }

    public boolean isWritable() {
        return false;
    }

    public boolean isRemovable() {
        return false;
    }

    public boolean setLastModified(long time) {
        return false;
    }

    public boolean mkdir() {
        return false;
    }

    public boolean delete() {
        return false;
    }

    public OutputStream createOutputStream(long offset) {
        return null;
    }

    public InputStream createInputStream(long offset) {
        logger.trace("[{}] createInputStream(offset: {})", name, offset);
        postClientAction(ClientActionEvent.ClientAction.DOWNLOAD);

        ByteArrayInputStream bais = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        bais.skip(offset);
        return bais;
    }

}
