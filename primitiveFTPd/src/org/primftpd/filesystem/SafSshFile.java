package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.provider.DocumentFile;

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

    public SafSshFile(Context context, ContentResolver contentResolver, Uri startUrl, Cursor cursor, String absPath, Session session) {
        super(context, contentResolver, startUrl, cursor, absPath);
        this.session = session;
    }

    public SafSshFile(
            Context context,
            ContentResolver contentResolver,
            Uri startUrl,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            String absPath,
            Session session) {
        super(context, contentResolver, startUrl, parentDocumentFile, documentFile, absPath);
        this.session = session;
    }

    public SafSshFile(
            Context context,
            ContentResolver contentResolver,
            Uri startUrl,
            DocumentFile parentDocumentFile,
            String name,
            String absPath,
            Session session) {
        super(context, contentResolver, startUrl, parentDocumentFile, name, absPath);
        this.session = session;
    }

    @Override
    protected SshFile createFile(Context context, ContentResolver contentResolver, Uri startUrl, Cursor cursor, String absPath) {
        return new SafSshFile(context, contentResolver, startUrl, cursor, absPath, session);
    }

    @Override
    protected SshFile createFile(
            Context context,
            ContentResolver contentResolver,
            Uri startUrl,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            String absPath) {
        return new SafSshFile(context, contentResolver, startUrl, parentDocumentFile, documentFile, absPath, session);
    }

    @Override
    public boolean create() throws IOException {
        logger.trace("[{}] create()", name);
        // called e.g. when uploading a new file
        return true;
    }

    @Override
    public void createSymbolicLink(org.apache.sshd.common.file.SshFile arg0)
            throws IOException
    {
        // TODO ssh createSymbolicLink
        logger.trace("[{}] createSymbolicLink()", name);
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
    public String getOwner() {
        logger.trace("[{}] getOwner()", name);
        return session.getUsername();
    }

    @Override
    public SshFile getParentFile() {
        logger.trace("[{}] getParentFile()", name);
        return null;
    }

    @Override
    public void handleClose() throws IOException {
        // TODO ssh handleClose
        logger.trace("[{}] handleClose()", name);
    }

    @Override
    public boolean isExecutable() {
        logger.trace("[{}] isExecutable()", name);
        return false;
    }

    @Override
    public List<SshFile> listSshFiles() {
        return listFiles();
    }

    @Override
    public boolean move(SshFile target) {
        logger.trace("move()");
        return super.move((SafFile)target);
    }

    @Override
    public String readSymbolicLink() throws IOException {
        logger.trace("[{}] readSymbolicLink()", name);
        return null;
    }

    @Override
    public void setAttribute(Attribute attribute, Object value) throws IOException {
        // TODO ssh saf setAttribute
        logger.trace("[{}] setAttribute()", name);
    }

    @Override
    public void setAttributes(Map<Attribute, Object> attributes) throws IOException {
        // TODO ssh saf setAttributes
        logger.trace("[{}] setAttributes()", name);
    }

    @Override
    public void truncate() throws IOException {
        // TODO ssh saf truncate
        logger.trace("[{}] truncate()", name);
    }
}
