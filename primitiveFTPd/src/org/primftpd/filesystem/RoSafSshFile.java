package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.SshFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoSafSshFile extends RoSafFile<SshFile> implements SshFile {

    private final Session session;

    public RoSafSshFile(
            ContentResolver contentResolver,
            Uri startUrl,
            String absPath,
            Session session) {
        super(contentResolver, startUrl, absPath);
        this.session = session;
    }

    public RoSafSshFile(
            ContentResolver contentResolver,
            Uri startUrl,
            String docId,
            String absPath,
            Session session) {
        super(contentResolver, startUrl, docId, absPath);
        this.session = session;
    }

    public RoSafSshFile(
            ContentResolver contentResolver,
            Uri startUrl,
            Cursor cursor,
            String absPath,
            Session session) {
        super(contentResolver, startUrl, cursor, absPath);
        this.session = session;
    }

    @Override
    protected SshFile createFile(
            ContentResolver contentResolver,
            Uri startUrl,
            Cursor cursor,
            String absPath) {
        return new RoSafSshFile(contentResolver, startUrl, cursor, absPath, session);
    }

    @Override
    public boolean move(SshFile target) {
        logger.trace("move()");
        return super.move((RoSafFile)target);
    }

    @Override
    public String readSymbolicLink() throws IOException {
        logger.trace("[{}] readSymbolicLink()", name);
        return null;
    }

    @Override
    public void createSymbolicLink(org.apache.sshd.common.file.SshFile arg0)
            throws IOException
    {
        // TODO ssh createSymbolicLink
        logger.trace("[{}] createSymbolicLink()", name);
    }

    @Override
    public String getOwner() {
        logger.trace("[{}] getOwner()", name);
        return session.getUsername();
    }

    @Override
    public Object getAttribute(Attribute attribute, boolean followLinks)
            throws IOException
    {
        logger.trace("[{}] getAttribute({})", name, attribute);
        return SshUtils.getAttribute(this, attribute, followLinks);
    }

    @Override
    public Map<Attribute, Object> getAttributes(boolean followLinks)
            throws IOException
    {
        logger.trace("[{}] getAttributes()", name);

        Map<SshFile.Attribute, Object> attributes = new HashMap<>();
        for (SshFile.Attribute attr : Attribute.values()) {
            attributes.put(attr, getAttribute(attr, followLinks));
        }

        return attributes;
    }

    @Override
    public boolean create() throws IOException {
        logger.trace("[{}] create()", name);
        // called e.g. when uploading a new file
        return true;
    }

    @Override
    public SshFile getParentFile() {
        logger.trace("[{}] getParentFile()", name);
        return null;
    }

    @Override
    public boolean isExecutable() {
        logger.trace("[{}] isExecutable()", name);
        return false;
    }

    @Override
    public void handleClose() throws IOException {
        // TODO ssh handleClose
        logger.trace("[{}] handleClose()", name);
    }

    @Override
    public List<SshFile> listSshFiles() {
        return listFiles();
    }

    @Override
    public void setAttribute(Attribute attribute, Object value) throws IOException {
        // TODO ssh setAttribute
        logger.trace("[{}] setAttribute()", name);
    }

    @Override
    public void setAttributes(Map<Attribute, Object> attributes) throws IOException {
        // TODO ssh setAttributes
        logger.trace("[{}] setAttributes()", name);
    }

    @Override
    public void truncate() throws IOException {
        // TODO ssh truncate
        logger.trace("[{}] truncate()", name);
    }
}
