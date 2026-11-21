package org.primftpd.io;

import org.apache.ftpserver.ftplet.DataConnection;
import org.apache.ftpserver.ftplet.DataType;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.impl.DefaultFtpSession;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.ServerDataConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

public class AndroidIoDataConnection implements DataConnection {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private final FtpIoSession session;
    private final ServerDataConnectionFactory factory;
    private SocketChannel dataSocketChannel;

    // small buffers slow down transfer of large files significantly
    private static final int BUFFER_SIZE = 64 * 1024;

    private static volatile long lastTransferTimestamp = 0;

    /**
     * @return Timestamp of last moment data has been transferred on any data connection, 0 if there was no transfer yet.
     */
    public static long getLastTransferTimestamp() {
        return lastTransferTimestamp;
    }

    public AndroidIoDataConnection(final SocketChannel dataSocketChannel, final FtpIoSession session,
                            final ServerDataConnectionFactory factory) {
        LOG.trace("AndroidIoDataConnection()");
        this.session = session;
        this.dataSocketChannel = dataSocketChannel;
        this.factory = factory;
    }


    /*
     * (non-Javadoc)
     *
     * @seeorg.apache.ftpserver.FtpDataConnection2#transferFromClient(java.io.
     * OutputStream)
     */
    public final long transferFromClient(FtpSession session, final OutputStream out)
            throws IOException {
        LOG.trace("transferFromClient()");

        WritableByteChannel outStreamBacked = new WritableByteChannel() {
            @Override
            public int write(ByteBuffer src) throws IOException {
                // buffer has been flipped by caller, write everything between position and limit
                int length = src.remaining();
                out.write(src.array(), src.arrayOffset() + src.position(), length);
                src.position(src.limit());
                return length;
            }

            @Override
            public boolean isOpen() {
                return true;
            }

            @Override
            public void close() throws IOException {
                out.close();
            }
        };

        try {
            // data is read from client
            return transfer(session, false, dataSocketChannel, outStreamBacked);
        } finally {
            //IoUtils.close(out);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.ftpserver.FtpDataConnection2#transferToClient(java.io.InputStream
     * )
     */
    public final long transferToClient(FtpSession session, final InputStream in)
            throws IOException {
        LOG.trace("transferToClient()");

        ReadableByteChannel inStreamBacked = new ReadableByteChannel() {
            private boolean eof = false;
            @Override
            public int read(ByteBuffer dst) throws IOException {
                int read = in.read(dst.array(), dst.arrayOffset() + dst.position(), dst.remaining());
                if (read < 0) {
                    eof = true;
                } else {
                    dst.position(dst.position() + read);
                }
                return read;
            }

            @Override
            public boolean isOpen() {
                return !eof;
            }

            @Override
            public void close() throws IOException {
                in.close();
            }
        };

        try {
            return transfer(session, true, inStreamBacked, dataSocketChannel);
        } finally {
            //IoUtils.close(out);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.ftpserver.FtpDataConnection2#transferToClient(java.lang.String
     * )
     */
    public final void transferToClient(FtpSession session, final String str)
            throws IOException {
        LOG.trace("transferToClient()");
        //Writer writer = null;
        try {
            //writer = new OutputStreamWriter(out, "UTF-8");
            //writer.write(str);
            byte[] bytes = str.getBytes("UTF-8");
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            // a single call of write() is not guaranteed to write everything, e.g. listing of large dirs
            while (buffer.hasRemaining()) {
                dataSocketChannel.write(buffer);
            }

            // update session
            if (session instanceof DefaultFtpSession) {
                ((DefaultFtpSession) session).increaseWrittenDataBytes(bytes.length);
            }
        } finally {
//            if (writer != null) {
//                writer.flush();
//            }
//            IoUtils.close(writer);
        }

    }

    private final long transfer(FtpSession session, boolean isWrite, final ReadableByteChannel in, final WritableByteChannel out)
            throws IOException {
        long transferredSize = 0L;

        boolean isAscii = session.getDataType() == DataType.ASCII;
        byte[] buff = new byte[BUFFER_SIZE];

        LOG.trace("transfer(), ascii: {}", isAscii);
        if (isAscii) {
            LOG.info("ignoring request for ascii transfer, doing it binary");
        }
        try {

            DefaultFtpSession defaultFtpSession = null;
            if (session instanceof DefaultFtpSession) {
                defaultFtpSession = (DefaultFtpSession) session;
            }

            CountingReadableByteChannel inCounting = new CountingReadableByteChannel(in);
            CountingWritableByteChannel outCounting = new CountingWritableByteChannel(out);

            ByteBuffer buffer = ByteBuffer.wrap(buff);
            while (true) {

                // read data
                buffer.clear();
                int count = inCounting.read(buffer);
                if (count == -1) {
                    break;
                }
                if (count == 0) {
                    continue;
                }

                // update MINA session
                if (defaultFtpSession != null) {
                    if (isWrite) {
                        defaultFtpSession.increaseWrittenDataBytes(count);
                    } else {
                        defaultFtpSession.increaseReadDataBytes(count);
                    }
                }

                // write data, a single call of write() is not guaranteed to write everything
                buffer.flip();
                while (buffer.hasRemaining()) {
                    outCounting.write(buffer);
                }

                transferredSize += count;

                notifyObserver();
            }

            LOG.trace("bytes read: {}", inCounting.getCount());
            LOG.trace("bytes written: {}", outCounting.getCount());

        } catch(IOException e) {
            LOG.warn("Exception during data transfer, closing data connection socket", e);
            factory.closeDataConnection();
            throw e;
        } catch(RuntimeException e) {
            LOG.warn("Exception during data transfer, closing data connection socket", e);
            factory.closeDataConnection();
            throw e;
        } finally {
//            if (out != null) {
//                out.flush();
//            }
        }

        return transferredSize;
    }

    /**
     * Notify connection manager observer.
     */
    protected void notifyObserver() {
        //LOG.trace("notifyObserver()");
        session.updateLastAccessTime();
        lastTransferTimestamp = System.currentTimeMillis();

        // TODO this has been moved from AbstractConnection, do we need to keep
        // it?
        // serverContext.getConnectionManager().updateConnection(this);
    }
}
