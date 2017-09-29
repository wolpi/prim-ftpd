package org.primftpd.filesystem;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.provider.DocumentFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class SafFileSystemView<T extends SafFile<X>, X> {

    protected final static String ROOT_PATH = "/";

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final Context context;
    protected final ContentResolver contentResolver;
    protected final Uri startUrl;

    public SafFileSystemView(Context context, ContentResolver contentResolver, Uri startUrl) {
        this.context = context;
        this.contentResolver = contentResolver;
        this.startUrl = startUrl;
    }

    protected abstract T createFile();
    protected abstract T createFile(Cursor cursor, String absPath);
    protected abstract T createFile(DocumentFile parentDocumentFile, DocumentFile documentFile, String absPath);
    protected abstract T createFile(DocumentFile parentDocumentFile, String name, String absPath);

    public T getFile(String file) {
        logger.trace("getFile({})", file);

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
                T child = createFile(parentDocFile, docFile, absPath);
                if (found) {
                    return child;
                }
            } else if (i == parts.size() - 1) {
                // if just last part is not found -> probably upload -> create object just with name
                String absPath = toPath(parts);
                return createFile(parentDocFile, currentPart, absPath);

            } else {
                break;
            }
        }

        return createFile(null, rootDocFile, ROOT_PATH);
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
