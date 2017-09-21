package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.SshFile;

public class SafSshFileSystemView extends SafFileSystemView<SafSshFile, SshFile> implements FileSystemView {

    private final Session session;

    public SafSshFileSystemView(Context context, ContentResolver contentResolver, Uri startUrl, Session session) {
        super(context, contentResolver, startUrl);
        this.session = session;
    }

    @Override
    protected SafSshFile createFile() {
        logger.trace("createFile()");
        return new SafSshFile(context, contentResolver, startUrl, session);
    }

    @Override
    public SshFile getFile(SshFile baseDir, String file) {
        logger.trace("getFile()");
        return null;
    }

    @Override
    public FileSystemView getNormalizedView() {
        logger.trace("getNormalizedView()");
        return this;
    }
}
