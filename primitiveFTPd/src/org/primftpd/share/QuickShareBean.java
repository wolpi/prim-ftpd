package org.primftpd.share;

import java.io.File;
import java.io.Serializable;

public class QuickShareBean implements Serializable {
    private final File tmpDir;

    QuickShareBean(File tmpDir) {
        this.tmpDir = tmpDir;
    }

    public File getTmpDir() {
        return tmpDir;
    }

    public int numberOfFiles() {
        String[] list = tmpDir.list();
        if (list != null) {
            return list.length;
        }
        return -1;
    }
}
