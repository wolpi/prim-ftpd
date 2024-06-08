package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.SshFile;
import org.primftpd.services.PftpdService;

import java.util.List;

public class RoSafSshFile extends RoSafFile<SshFile> implements SshFile {

    private final Session session;

    public RoSafSshFile(
            ContentResolver contentResolver,
            Uri startUrl,
            String absPath,
            PftpdService pftpdService,
            Session session) {
        super(contentResolver, startUrl, absPath, pftpdService);
        this.session = session;
    }

    public RoSafSshFile(
            ContentResolver contentResolver,
            Uri startUrl,
            String docId,
            String absPath,
            boolean exists,
            PftpdService pftpdService,
            Session session) {
        super(contentResolver, startUrl, docId, absPath, exists, pftpdService);
        this.session = session;
    }

    public RoSafSshFile(
            ContentResolver contentResolver,
            Uri startUrl,
            Cursor cursor,
            String absPath,
            PftpdService pftpdService,
            Session session) {
        super(contentResolver, startUrl, cursor, absPath, pftpdService);
        this.session = session;
    }

    @Override
    protected SshFile createFile(
            ContentResolver contentResolver,
            Uri startUrl,
            Cursor cursor,
            String absPath,
            PftpdService pftpdService) {
        return new RoSafSshFile(contentResolver, startUrl, cursor, absPath, pftpdService, session);
    }

    @Override
    public String getClientIp() {
        return SshUtils.getClientIp(session);
    }

    @Override
    public boolean move(SshFile target) {
        logger.trace("move()");
        return super.move((RoSafFile)target);
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
    public List<SshFile> listSshFiles() {
        return listFiles();
    }
}
