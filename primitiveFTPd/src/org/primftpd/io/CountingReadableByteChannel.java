package org.primftpd.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class CountingReadableByteChannel implements ReadableByteChannel {

    private final ReadableByteChannel delegate;
    private int count;

    public CountingReadableByteChannel(ReadableByteChannel delegate) {
        this.delegate = delegate;
        this.count = 0;
    }

    @Override
    public int read(ByteBuffer src) throws IOException {
        int read = delegate.read(src);
        if (read != -1) {
            count += read;
        }
        return read;
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    public int getCount() {
        return count;
    }
}
