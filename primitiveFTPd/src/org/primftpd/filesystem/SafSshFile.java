package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.support.v4.provider.DocumentFile;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.SshFile;

import java.util.List;

public class SafSshFile extends SafFile<SshFile> implements SshFile {

    private final Session session;

    public SafSshFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            String absPath,
            Session session) {
        super(contentResolver, parentDocumentFile, documentFile, absPath);
        this.session = session;
    }

    public SafSshFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            String name,
            String absPath,
            Session session) {
        super(contentResolver, parentDocumentFile, name, absPath);
        this.session = session;
    }

    @Override
    protected SshFile createFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            String absPath) {
        return new SafSshFile(contentResolver, parentDocumentFile, documentFile, absPath, session);
    }

    @Override
    public boolean move(SshFile target) {
        logger.trace("move()");
        return super.move((SafFile)target);
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
