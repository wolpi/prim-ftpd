package org.primftpd.filesystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public abstract class QuickShareFileSystemView<T extends QuickShareFile<X>, X> {

    protected final static String ROOT_PATH = "/";
    protected final static String CURRENT_PATH = ".";
    protected final static String CURRENT_ROOT_PATH = "/.";

    final Logger logger = LoggerFactory.getLogger(getClass());
    final File quickShareFile;

    QuickShareFileSystemView(File quickShareFile) {
        this.quickShareFile = quickShareFile;
    }

    abstract protected T createFile(File quickShareFile, String dir);
    abstract protected T createFile(File quickShareFile);

    public T getFile(String file) {
        logger.trace("getFile({})", file);

        T result;
        if (ROOT_PATH.equals(file) || CURRENT_PATH.equals(file) || CURRENT_ROOT_PATH.equals(file)) {
            result = createFile(quickShareFile, ROOT_PATH);
        } else {
            result = createFile(quickShareFile);
        }

        return result;
    }
}
