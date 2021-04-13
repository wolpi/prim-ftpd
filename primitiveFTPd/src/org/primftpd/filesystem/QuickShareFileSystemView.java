package org.primftpd.filesystem;

import org.primftpd.services.PftpdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public abstract class QuickShareFileSystemView<T extends QuickShareFile<X>, X> {

    protected final static String ROOT_PATH = "/";
    protected final static String CURRENT_PATH = ".";
    protected final static String CURRENT_ROOT_PATH = "/.";

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final File quickShareFile;
    protected final PftpdService pftpdService;

    QuickShareFileSystemView(File quickShareFile, PftpdService pftpdService) {
        this.quickShareFile = quickShareFile;
        this.pftpdService = pftpdService;
    }

    abstract protected T createFile(File quickShareFile, String dir, PftpdService pftpdService);
    abstract protected T createFile(File quickShareFile, PftpdService pftpdService);

    public T getFile(String file) {
        logger.trace("getFile({})", file);

        T result;
        if (ROOT_PATH.equals(file) || CURRENT_PATH.equals(file) || CURRENT_ROOT_PATH.equals(file)) {
            result = createFile(quickShareFile, ROOT_PATH, pftpdService);
        } else {
            result = createFile(quickShareFile, pftpdService);
        }

        return result;
    }
}
