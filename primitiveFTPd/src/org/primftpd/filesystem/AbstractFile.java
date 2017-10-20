package org.primftpd.filesystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFile {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected boolean isDirectory;
    protected String absPath;

    protected String name;
    protected long lastModified;
    protected long size;
    protected boolean readable;
    protected boolean exists;

    public AbstractFile(String absPath, String name, long lastModified, long size, boolean readable, boolean exists, boolean isDirectory) {
        this.absPath = absPath;
        this.name = name;
        this.lastModified = lastModified;
        this.size = size;
        this.readable = readable;
        this.exists = exists;
        this.isDirectory = isDirectory;
    }

    public String getAbsolutePath() {
        logger.trace("[{}] getAbsolutePath() -> '{}'", name, absPath);
        return absPath;
    }

    public String getName() {
        logger.trace("[{}] getName()", name);
        return name;
    }

    public boolean isDirectory() {
        logger.trace("[{}] isDirectory() -> {}", name, isDirectory);
        return isDirectory;
    }

    public boolean doesExist() {
        logger.trace("[{}] doesExist() -> {}", name, exists);
        return exists;
    }

    public boolean isReadable() {
        logger.trace("[{}] isReadable() -> {}", name, readable);
        return readable;
    }

    public long getLastModified() {
        logger.trace("[{}] getLastModified() -> {}", name, lastModified);
        return lastModified;
    }

    public long getSize() {
        logger.trace("[{}] getSize() -> {}", name, size);
        return size;
    }


}
