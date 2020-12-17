package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.SshFile;
import org.primftpd.events.ClientActionPoster;

public class SafSshFileSystemView extends SafFileSystemView<SafSshFile, SshFile> implements FileSystemView {

    private final Session session;

    public SafSshFileSystemView(Context context, Uri startUrl, ContentResolver contentResolver, ClientActionPoster clientActionPoster, Session session) {
        super(context, startUrl, contentResolver, clientActionPoster);
        this.session = session;
    }

    @Override
    protected SafSshFile createFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            String absPath,
            ClientActionPoster clientActionPoster) {
        return new SafSshFile(contentResolver, parentDocumentFile, documentFile, absPath, clientActionPoster, session, this);
    }

    @Override
    protected SafSshFile createFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            String name,
            String absPath,
            ClientActionPoster clientActionPoster) {
        return new SafSshFile(contentResolver, parentDocumentFile, name, absPath, clientActionPoster, session, this);
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
