package org.primftpd.filesystem;

import org.primftpd.services.PftpdService;

import java.io.File;

public abstract class QuickShareFileSystemView<TFile extends QuickShareFile<TMina, ? extends QuickShareFileSystemView>, TMina> extends AbstractFileSystemView {

    protected final static String ROOT_PATH = "/";
    protected final static String CURRENT_PATH = ".";
    protected final static String CURRENT_ROOT_PATH = "/.";

    protected final File tmpDir;

    public QuickShareFileSystemView(PftpdService pftpdService, File tmpDir) {
		super(pftpdService);
        this.tmpDir = tmpDir;
    }

    public final File getTmpDir() {
        return tmpDir;
    }

    abstract protected TFile createFile();
    abstract protected TFile createFile(File realFile);

    public TFile getFile(String file) {
        logger.trace("getFile({})", file);

        TFile result;
        if (ROOT_PATH.equals(file) || CURRENT_PATH.equals(file) || CURRENT_ROOT_PATH.equals(file)) {
            result = createFile();
        } else {
            String filename = file.substring(file.lastIndexOf('/')+1);
            result = createFile(new File(tmpDir, filename));
        }

        return result;
    }
}
