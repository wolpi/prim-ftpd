package org.primftpd.filesystem;

import org.primftpd.events.ClientActionPoster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public abstract class QuickShareFileSystemView<T extends QuickShareFile<X>, X> {

    protected final static String ROOT_PATH = "/";
    protected final static String CURRENT_PATH = ".";
    protected final static String CURRENT_ROOT_PATH = "/.";

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final File quickShareFile;
    protected final ClientActionPoster clientActionPoster;

    QuickShareFileSystemView(File quickShareFile, ClientActionPoster clientActionPoster) {
        this.quickShareFile = quickShareFile;
        this.clientActionPoster = clientActionPoster;
    }

    abstract protected T createFile(File quickShareFile, String dir, ClientActionPoster clientActionPoster);
    abstract protected T createFile(File quickShareFile, ClientActionPoster clientActionPoster);

    public T getFile(String file) {
        logger.trace("getFile({})", file);

        T result;
        if (ROOT_PATH.equals(file) || CURRENT_PATH.equals(file) || CURRENT_ROOT_PATH.equals(file)) {
            result = createFile(quickShareFile, ROOT_PATH, clientActionPoster);
        } else {
            result = createFile(quickShareFile, clientActionPoster);
        }

        return result;
    }
}
