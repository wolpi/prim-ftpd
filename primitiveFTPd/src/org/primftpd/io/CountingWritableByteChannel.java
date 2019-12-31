package org.primftpd.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public class CountingWritableByteChannel implements WritableByteChannel {

    private final WritableByteChannel delegate;
    private int count;

    public CountingWritableByteChannel(WritableByteChannel delegate) {
        this.delegate = delegate;
        this.count = 0;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        int written = delegate.write(src);
        count += written;
        return written;
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
