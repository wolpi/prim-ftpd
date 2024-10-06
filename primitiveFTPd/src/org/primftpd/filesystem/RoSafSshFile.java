package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.SshFile;
import org.primftpd.services.PftpdService;

import java.util.List;

public class RoSafSshFile extends RoSafFile<SshFile, RoSafSshFileSystemView> implements SshFile {

    private final Session session;

    public RoSafSshFile(
            RoSafSshFileSystemView fileSystemView,
            String absPath,
            Session session) {
        super(fileSystemView, absPath);
        this.session = session;
    }

    public RoSafSshFile(
            RoSafSshFileSystemView fileSystemView,
            String absPath,
            String docId,
            boolean exists,
            Session session) {
        super(fileSystemView, absPath, docId, exists);
        this.session = session;
    }

    protected RoSafSshFile(
            RoSafSshFileSystemView fileSystemView,
            String absPath,
            Cursor cursor,
            Session session) {
        super(fileSystemView, absPath, cursor);
        this.session = session;
    }

    @Override
    protected SshFile createFile(String absPath, Cursor cursor) {
        return new RoSafSshFile(getFileSystemView(), absPath, cursor, session);
    }

    @Override
    public String getClientIp() {
        return SshUtils.getClientIp(session);
    }

    @Override
    public boolean move(SshFile target) {
        return super.move((AbstractFile)target);
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
