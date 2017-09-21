package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.SshFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SafSshFile extends SafFile<SshFile> implements SshFile {

    private final Session session;

    public SafSshFile(Context context, ContentResolver contentResolver, Uri startUrl, Session session) {
        super(context, contentResolver, startUrl);
        this.session = session;
    }

    public SafSshFile(Context context, ContentResolver contentResolver, Uri startUrl, Cursor cursor, Session session) {
        super(context, contentResolver, startUrl, cursor);
        this.session = session;
    }

    @Override
    protected SshFile createFile(Context context, ContentResolver contentResolver, Uri startUrl, Cursor cursor) {
        return new SafSshFile(context, contentResolver, startUrl, cursor, session);
    }

    @Override
    public boolean create() throws IOException {
        logger.trace("create()");
        return false;
    }

    @Override
    public void createSymbolicLink(org.apache.sshd.common.file.SshFile arg0)
            throws IOException
    {
        // TODO ssh createSymbolicLink
        logger.trace("createSymbolicLink()");
    }

    @Override
    public Object getAttribute(Attribute attribute, boolean followLinks)
            throws IOException
    {
        logger.trace("getAttribute({})", attribute);
        return SshUtils.getAttribute(this, attribute, followLinks);
    }

    @Override
    public Map<Attribute, Object> getAttributes(boolean followLinks)
            throws IOException
    {
        logger.trace("getAttributes()");

        Map<SshFile.Attribute, Object> attributes = new HashMap<>();
        for (SshFile.Attribute attr : Attribute.values()) {
            attributes.put(attr, getAttribute(attr, followLinks));
        }

        return attributes;
    }

    @Override
    public String getOwner() {
        logger.trace("getOwner()");
        return session.getUsername();
    }

    @Override
    public SshFile getParentFile() {
        logger.trace("getParentFile()");
        return null;
    }

    @Override
    public void handleClose() throws IOException {
        // TODO ssh handleClose
        logger.trace("handleClose()");
    }

    @Override
    public boolean isExecutable() {
        logger.trace("isExecutable()");
        return false;
    }

    @Override
    public List<SshFile> listSshFiles() {
        return listFiles();
    }

    @Override
    public boolean move(SshFile target) {
        logger.trace("move()");
        return false;
    }

    @Override
    public String readSymbolicLink() throws IOException {
        logger.trace("readSymbolicLink()");
        return null;
    }

    @Override
    public void setAttribute(Attribute attribute, Object value) throws IOException {
        // TODO ssh saf setAttribute
        logger.trace("setAttribute()");
    }

    @Override
    public void setAttributes(Map<Attribute, Object> attributes) throws IOException {
        // TODO ssh saf setAttributes
        logger.trace("setAttributes()");
    }

    @Override
    public void truncate() throws IOException {
        // TODO ssh saf truncate
        logger.trace("truncate()");
    }
}
