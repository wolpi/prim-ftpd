package org.primftpd.filesystem;

import android.content.ContentResolver;
import androidx.documentfile.provider.DocumentFile;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.SshFile;

import java.util.List;

public class SafSshFile extends SafFile<SshFile> implements SshFile {

    private final Session session;
    private final SafSshFileSystemView fileSystemView;

    public SafSshFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            String absPath,
            Session session,
            SafSshFileSystemView fileSystemView) {
        super(contentResolver, parentDocumentFile, documentFile, absPath);
        this.session = session;
        this.fileSystemView = fileSystemView;
    }

    public SafSshFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            String name,
            String absPath,
            Session session,
            SafSshFileSystemView fileSystemView) {
        super(contentResolver, parentDocumentFile, name, absPath);
        this.session = session;
        this.fileSystemView = fileSystemView;
    }

    @Override
    protected SshFile createFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            String absPath) {
        return new SafSshFile(contentResolver, parentDocumentFile, documentFile, absPath, session, fileSystemView);
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
        String parentPath = Utils.parent(absPath);
        if (parentPath.length() == 0) {
            // in SAF we don't keep track of home dir
            parentPath = "/";
        }
        logger.trace("[{}]   getParentFile() -> {}", name, parentPath);
        return fileSystemView.getFile(parentPath);
    }

    @Override
    public List<SshFile> listSshFiles() {
        return listFiles();
    }
}
