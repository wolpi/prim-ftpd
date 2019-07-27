package org.primftpd.filesystem;

import org.slf4j.Logger;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TracingBufferedOutputStream extends BufferedOutputStream {

    public static final int BUFFER_SIZE = 1024 * 1024;

    protected final Logger logger;

    public TracingBufferedOutputStream(OutputStream os, Logger logger) {
        super(os ,BUFFER_SIZE);
        this.logger = logger;
    }

    @Override
    public void close() throws IOException {
        super.close();
        logger.trace("sizes in close(), count: '{}', buf len: '{}'", count, buf.length);
    }

    @Override
    public synchronized void flush() throws IOException {
        super.flush();
        logger.trace("flush()");
    }

    @Override
    public synchronized void write(int b) throws IOException {
        super.write(b);
        logger.trace("write(single byte)");
    }

    @Override
    public void write(byte[] b) throws IOException {
        super.write(b);
        logger.trace("write(arr len: {})", b.length);
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
        logger.trace("write(len: {})", len);
    }
}
