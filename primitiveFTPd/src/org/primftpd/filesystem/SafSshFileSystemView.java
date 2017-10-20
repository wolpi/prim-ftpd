package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.support.v4.provider.DocumentFile;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.SshFile;

public class SafSshFileSystemView extends SafFileSystemView<SafSshFile, SshFile> implements FileSystemView {

    private final Session session;

    public SafSshFileSystemView(Context context, Uri startUrl, ContentResolver contentResolver, Session session) {
        super(context, startUrl, contentResolver);
        this.session = session;
    }

    @Override
    protected SafSshFile createFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            String absPath) {
        return new SafSshFile(contentResolver, parentDocumentFile, documentFile, absPath, session);
    }

    @Override
    protected SafSshFile createFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            String name,
            String absPath) {
        return new SafSshFile(contentResolver, parentDocumentFile, name, absPath, session);
    }

    @Override
    protected String absolute(String file) {
        return file;
    }

    @Override
    public SshFile getFile(SshFile baseDir, String file) {
        logger.trace("getFile(baseDir, {})", file);
        return null;
    }

    @Override
    public FileSystemView getNormalizedView() {
        logger.trace("getNormalizedView()");
        return this;
    }
}
