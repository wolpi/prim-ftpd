package org.primftpd.share;

import java.io.Serializable;

public class QuickShareBean implements Serializable {
    private final String pathToFile;

    QuickShareBean(String pathToFile) {
        this.pathToFile = pathToFile;
    }

    public String getPathToFile() {
        return pathToFile;
    }
}
