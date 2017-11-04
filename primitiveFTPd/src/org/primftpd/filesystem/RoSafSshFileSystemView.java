package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.net.Uri;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.SshFile;

public class RoSafSshFileSystemView extends RoSafFileSystemView<RoSafSshFile, SshFile> implements FileSystemView {

    private final Session session;

    public RoSafSshFileSystemView(Uri startUrl, ContentResolver contentResolver, Session session) {
        super(startUrl, contentResolver);
        this.session = session;
    }

    @Override
    protected RoSafSshFile createFile(ContentResolver contentResolver, Uri startUrl, String absPath) {
        return new RoSafSshFile(contentResolver, startUrl, absPath, session);
    }

    @Override
    protected RoSafSshFile createFile(ContentResolver contentResolver, Uri startUrl, String docId, String absPath) {
        return new RoSafSshFile(contentResolver, startUrl, docId, absPath, true, session);
    }

    @Override
    protected RoSafSshFile createFileNonExistant(ContentResolver contentResolver, Uri startUrl, String name, String absPath) {
        return new RoSafSshFile(contentResolver, startUrl, name, absPath, false, session);
    }

    @Override
    protected String absolute(String file) {
        return Utils.absoluteOrHome(file, ROOT_PATH);
    }

    @Override
    public SshFile getFile(SshFile baseDir, String file) {
        logger.trace("getFile(baseDir: {}, file: {})", baseDir.getAbsolutePath(), file);
        // e.g. for scp
        return getFile(baseDir.getAbsolutePath() + "/" + file);
    }

    @Override
    public FileSystemView getNormalizedView() {
        logger.trace("getNormalizedView()");
        return this;
    }
}
