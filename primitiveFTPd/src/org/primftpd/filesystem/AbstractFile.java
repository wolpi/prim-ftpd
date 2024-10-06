package org.primftpd.filesystem;

import org.apache.sshd.common.file.SshFile;
import org.primftpd.events.ClientActionEvent;
import org.primftpd.services.PftpdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractFile<TFileSystemView extends AbstractFileSystemView> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final TFileSystemView fileSystemView;

    protected String absPath;
    protected String name;

    public AbstractFile(
            TFileSystemView fileSystemView,
            String absPath,
            String name) {
        this.fileSystemView = fileSystemView;
        this.absPath = absPath;
        this.name = name;
    }

    protected final TFileSystemView getFileSystemView() {
        return fileSystemView;
    }

    protected final PftpdService getPftpdService() {
        return fileSystemView.getPftpdService();
    }

    public abstract String getClientIp();
    public abstract ClientActionEvent.Storage getClientActionStorage();

    public void postClientAction(ClientActionEvent.ClientAction clientAction) {
        postClientAction(clientAction, null);
    }

    public void postClientActionError(String error) {
        postClientAction(ClientActionEvent.ClientAction.ERROR, error);
    }

    public void postClientAction(ClientActionEvent.ClientAction clientAction, String error) {
        getPftpdService().postClientAction(
                getClientActionStorage(),
                clientAction,
                getClientIp(),
                getAbsolutePath(),
                error);
    }

    public String getAbsolutePath() {
        logger.trace("[{}] getAbsolutePath() -> '{}'", name, absPath);
        return absPath;
    }

    public String getName() {
        logger.trace("[{}] getName()", name);
        return name != null ? name : "<unknown>";
    }

    public abstract boolean isDirectory();

    public abstract boolean doesExist();

    public abstract boolean isReadable();

    public abstract long getLastModified();

    public abstract long getSize();

    public abstract boolean isFile();

    public abstract boolean isWritable();

    public abstract boolean isRemovable();

    public abstract boolean setLastModified(long time);

    public abstract boolean mkdir();

    public abstract boolean delete();

    public abstract boolean move(AbstractFile<TFileSystemView> destination);

    public abstract OutputStream createOutputStream(long offset) throws IOException;

    public abstract InputStream createInputStream(long offset) throws IOException;

    ///////////////////////////////////////////////////////////////////////////
    // ftp
    ///////////////////////////////////////////////////////////////////////////

    public Object getPhysicalFile() {
        return this;
    }

    public int getLinkCount() {
        logger.trace("[{}] getLinkCount()", name);
        return 0;
    }

    public boolean isHidden() {
        //boolean result = name.charAt(0) == '.';
        boolean result = false;
        logger.trace("[{}] isHidden() -> {}", name, result);
        return result;
    }

    ///////////////////////////////////////////////////////////////////////////
    // ssh
    ///////////////////////////////////////////////////////////////////////////

    public void handleClose() throws IOException {
        // TODO ssh handleClose
        logger.trace("[{}] handleClose()", name);
    }

    public void truncate() {
        // TODO ssh truncate
        logger.trace("[{}] truncate()", name);
    }

    public String readSymbolicLink() throws IOException {
        logger.trace("[{}] readSymbolicLink()", name);
        //return null;
        // returning null causes issues with some clients, e.g. GH issue #121
        // let's try empty string and see what users report
        return "";
    }

    public void createSymbolicLink(SshFile arg0)
            throws IOException
    {
        // TODO ssh createSymbolicLink
        logger.trace("[{}] createSymbolicLink()", name);
    }

    public void setAttribute(SshFile.Attribute attribute, Object value) {
        logger.trace("[{}] setAttribute({})", name, attribute);
        SshUtils.setAttribute((SshFile)this, attribute, value);
    }

    public void setAttributes(Map<SshFile.Attribute, Object> attributes) throws IOException {
        logger.trace("[{}] setAttributes()", name);
        for (SshFile.Attribute attr : attributes.keySet()) {
            setAttribute(attr, attributes.get(attr));
        }
    }

    public Object getAttribute(SshFile.Attribute attribute, boolean followLinks)
            throws IOException
    {
        logger.trace("[{}] getAttribute({})", name, attribute);
        return SshUtils.getAttribute((SshFile)this, attribute);
    }

    public Map<SshFile.Attribute, Object> getAttributes(boolean followLinks)
            throws IOException
    {
        logger.trace("[{}] getAttributes()", name);

        Map<SshFile.Attribute, Object> attributes = new HashMap<>();
        for (SshFile.Attribute attr : SshFile.Attribute.values()) {
            attributes.put(attr, getAttribute(attr, followLinks));
        }

        return attributes;
    }

    public boolean isExecutable() {
        // return directories as executable in order to allow to enter them
        // at least we tell clients that they can try
        boolean result = isDirectory();
        logger.trace("[{}] isExecutable() -> {}", name, result);
        return result;
    }

    public boolean create() throws IOException {
        // called e.g. when uploading a new file
        boolean result = true;
        logger.trace("[{}] create() -> {}", name, result);
        return result;
    }

}
