package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.provider.DocumentFile;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class SafFileSystemView<T extends SafFile<X>, X> {

    protected final static String ROOT_PATH = "/";

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final Context context;
    protected final Uri startUrl;
    protected final ContentResolver contentResolver;

    public SafFileSystemView(Context context, Uri startUrl, ContentResolver contentResolver) {
        this.context = context;
        this.startUrl = startUrl;
        this.contentResolver = contentResolver;
    }

    protected abstract T createFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            String absPath);
    protected abstract T createFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            String name,
            String absPath);

    public T getFile(String file) {
        logger.trace("getFile({})", file);

        try {
            List<String> parts = normalizePath(file);
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
                    String absPath = toPath(parts);
                    T child = createFile(contentResolver, parentDocFile, docFile, absPath);
                    if (found) {
                        return child;
                    }
                } else if (i == parts.size() - 1) {
                    // if just last part is not found -> probably upload -> create object just with name
                    String absPath = toPath(parts);
                    return createFile(contentResolver, parentDocFile, currentPart, absPath);

                } else {
                    break;
                }
            }

            return createFile(contentResolver, rootDocFile, rootDocFile, ROOT_PATH);
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

    private List<String> normalizePath(String path) {
        String[] parts = path.split("/");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            if (".".equals(part) || "".equals(part)) {
                continue;
            } else if ("..".equals(part)) {
                if (!result.isEmpty()) {
                    result.remove(result.size() - 1);
                }
                continue;
            } else {
                result.add(part);
            }
        }
        return result;
    }

    private String toPath(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        sb.append(ROOT_PATH);
        int i=0;
        for (String part : parts) {
            sb.append(part);
            if (i < parts.size() - 1) {
                sb.append("/");
            }
            i++;
        }
        return sb.toString();
    }
}
