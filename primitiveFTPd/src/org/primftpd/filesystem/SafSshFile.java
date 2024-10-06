package org.primftpd.filesystem;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.SshFile;

import java.io.IOException;
import java.util.List;

import androidx.documentfile.provider.DocumentFile;

public class SafSshFile extends SafFile<SshFile, SafSshFileSystemView> implements SshFile {

    private final Session session;

    public SafSshFile(
            SafSshFileSystemView fileSystemView,
            String absPath,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            Session session) {
        super(fileSystemView, absPath, parentDocumentFile, documentFile);
        this.session = session;
    }

    public SafSshFile(
            SafSshFileSystemView fileSystemView,
            String absPath,
            DocumentFile parentDocumentFile,
            List<String> parentNonexistentDirs,
            String name,
            Session session) {
        super(fileSystemView, absPath, parentDocumentFile, parentNonexistentDirs, name);
        this.session = session;
    }

    @Override
    protected SshFile createFile(
            String absPath,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile) {
        return new SafSshFile(getFileSystemView(), absPath, parentDocumentFile, documentFile, session);
    }

    @Override
    public String getClientIp() {
        return SshUtils.getClientIp(session);
    }

    @Override
    public boolean move(SshFile target) {
        return super.move((SafSshFile)target);
    }

    @Override
    public String getOwner() {
        logger.trace("[{}] getOwner()", name);
        return session.getUsername();
    }

    @Override
    public boolean create() throws IOException {
        // This call is required by SSHFS, because it calls STAT on created new files.
        // This call is not required by normal clients who simply open, write and close the file.
        boolean result = createNewFile();
        logger.trace("[{}] create() -> {}", name, result);
        return result;
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
        return getFileSystemView().getFile(parentPath);
    }

    @Override
    public List<SshFile> listSshFiles() {
        return listFiles();
    }
}
