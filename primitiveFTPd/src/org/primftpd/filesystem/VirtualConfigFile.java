package org.primftpd.filesystem;

import org.primftpd.events.ClientActionEvent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
public abstract class VirtualConfigFile<TFileSystemView extends VirtualFileSystemView> extends AbstractFile<TFileSystemView> {

    public static final String NAME = "primftpd.config";
    public static final String ABS_PATH = "/" + NAME;

    private final String content;

    public VirtualConfigFile(
            TFileSystemView fileSystemView) {
        super(
            fileSystemView,
            ABS_PATH,
            NAME);
        this.content = getContent();
    }

    private String getContent() {
        return
            "{" +
                "\"announceName\":\"" + getPftpdService().getPrefsBean().getAnnounceName().replace("\"", "\\\"") + "\"" +
            "}";
    }

    public ClientActionEvent.Storage getClientActionStorage() {
        return ClientActionEvent.Storage.CONFIG;
    }

    public boolean isDirectory() {
        return false;
    }

    public boolean doesExist() {
        return true;
    }

    public boolean isReadable() {
        return true;
    }

    public long getLastModified() {
        return 0;
    }

    public long getSize() {
        return content.length();
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

    public boolean move(AbstractFile<TFileSystemView> target) {
        return false;
    }

    public OutputStream createOutputStream(long offset) throws IOException{
        throw new IOException(String.format("Can not write file '%s'", absPath));
    }

    public InputStream createInputStream(long offset) {
        logger.trace("[{}] createInputStream(offset: {})", name, offset);
        postClientAction(ClientActionEvent.ClientAction.DOWNLOAD);

        ByteArrayInputStream bais = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        bais.skip(offset);
        return bais;
    }
}
