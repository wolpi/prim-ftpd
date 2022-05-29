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
    protected final File tmpDir;
    protected final PftpdService pftpdService;

    QuickShareFileSystemView(File tmpDir, PftpdService pftpdService) {
        this.tmpDir = tmpDir;
        this.pftpdService = pftpdService;
    }

    abstract protected T createFile(File tmpDir, PftpdService pftpdService);
    abstract protected T createFile(File tmpDir, File realFile, PftpdService pftpdService);

    public T getFile(String file) {
        logger.trace("getFile({})", file);

        T result;
        if (ROOT_PATH.equals(file) || CURRENT_PATH.equals(file) || CURRENT_ROOT_PATH.equals(file)) {
            result = createFile(tmpDir, pftpdService);
        } else {
            String filename = file.substring(file.lastIndexOf('/')+1);
            result = createFile(tmpDir, new File(tmpDir, filename), pftpdService);
        }

        return result;
    }
}
