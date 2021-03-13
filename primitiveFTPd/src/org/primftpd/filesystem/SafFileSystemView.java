package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import androidx.documentfile.provider.DocumentFile;
import android.widget.Toast;

import org.primftpd.services.PftpdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class SafFileSystemView<T extends SafFile<X>, X> {

    protected final static String ROOT_PATH = "/";

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final Context context;
    protected final Uri startUrl;
    protected final ContentResolver contentResolver;
    protected final PftpdService pftpdService;

    public SafFileSystemView(Context context, Uri startUrl, ContentResolver contentResolver, PftpdService pftpdService) {
        this.context = context;
        this.startUrl = startUrl;
        this.contentResolver = contentResolver;
        this.pftpdService = pftpdService;
    }

    protected abstract T createFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            String absPath,
            PftpdService pftpdService);
    protected abstract T createFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            String name,
            String absPath,
            PftpdService pftpdService);

    protected abstract String absolute(String file);

    public T getFile(String file) {
        logger.trace("getFile({})", file);

        file = absolute(file);
        logger.trace("  getFile(abs: {})", file);

        try {
            List<String> parts = Utils.normalizePath(file);
            logger.trace("  getFile(): normalized path parts: '{}'", parts);
            DocumentFile rootDocFile = DocumentFile.fromTreeUri(context, startUrl);
            DocumentFile docFile = rootDocFile;
            for (int i=0; i<parts.size(); i++) {
                String currentPart = parts.get(i);
                logger.trace("  getFile(): current docFile '{}', current part: '{}'", docFile.getName(), currentPart);
                DocumentFile parentDocFile = docFile;
                docFile = docFile.findFile(currentPart);

                if (docFile != null) {
                    boolean found = i == parts.size() - 1;
                    String absPath = Utils.toPath(parts);
                    T child = createFile(contentResolver, parentDocFile, docFile, absPath, pftpdService);
                    if (found) {
                        return child;
                    }
                } else {
                    // probably upload -> create object just with name
                    String absPath = Utils.toPath(parts);
                    return createFile(contentResolver, parentDocFile, currentPart, absPath, pftpdService);
                }
            }

            return createFile(contentResolver, rootDocFile, rootDocFile, ROOT_PATH, pftpdService);
        } catch (Exception e) {
            final String msg = "[(s)ftpd] Error getting data from SAF: " + e.toString();
            logger.error(msg);
            Handler handler = new Handler(context.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                }
            });
            throw e;
        }
    }
}
