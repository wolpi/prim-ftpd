package org.primftpd.io;

import org.apache.ftpserver.ftplet.DataConnection;
import org.apache.ftpserver.ftplet.DataType;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.impl.DefaultFtpSession;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.ServerDataConnectionFactory;
import org.greenrobot.eventbus.EventBus;
import org.primftpd.events.DataTransferredEvent;
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
                //LOG.trace("buffer stats, position: {}, limit: {}, capacity: {}, remaining: {}",
                //        new Object[]{src.position(), src.limit(), src.capacity(), src.remaining()});
                byte[] buf = src.array();
                int length = src.position();
                if (length < buf.length) {
                    LOG.trace("writing less than buffer length, len: {}, diff: {}", length, (buf.length - length));
                }
                out.write(buf, 0, length);
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
            return transfer(session, true, dataSocketChannel, outStreamBacked);
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

            private int lastRead = 0;

            @Override
            public int read(ByteBuffer dst) throws IOException {
                byte[] buf = dst.array();
                lastRead = in.read(buf);
                if (lastRead < 0) {
                    dst.position(0);
                    dst.limit(0);
                } else if (lastRead < buf.length) {
                    LOG.trace("setting buffer position: 0 & limit: {}", lastRead);
                    dst.position(0);
                    dst.limit(lastRead);
                }
                return lastRead;
            }

            @Override
            public boolean isOpen() {
                return lastRead >= 0;
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
            dataSocketChannel.write(buffer);

            // update session
            if (session instanceof DefaultFtpSession) {
                ((DefaultFtpSession) session).increaseWrittenDataBytes(str
                        .getBytes("UTF-8").length);
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
        byte[] buff = new byte[4096];

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

            long loopcnt = 0;
            ByteBuffer buffer = ByteBuffer.wrap(buff);
            while (true) {

                // read data
                int count = inCounting.read(buffer);
                if (count == -1) {
                    break;
                }
                if (count < buff.length) {
                    LOG.trace("read less than buffer size in loop '{}', read: {}, diff: {}",
                            new Object[]{loopcnt, count, (buff.length - count)});
                    LOG.trace("    buffer stats, position: {}, limit: {}, capacity: {}, remaining: {}",
                            new Object[]{buffer.position(), buffer.limit(), buffer.capacity(), buffer.remaining()});
                }

                // update MINA session
                if (defaultFtpSession != null) {
                    if (isWrite) {
                        defaultFtpSession.increaseWrittenDataBytes(count);
                    } else {
                        defaultFtpSession.increaseReadDataBytes(count);
                    }
                }

                // write data
                outCounting.write(buffer);

                transferredSize += count;

                notifyObserver();

                buffer.clear();

                long read = inCounting.getCount();
                long written = outCounting.getCount();
                if (read != written) {
                    LOG.trace("difference of read/written in loop '{}', bytes: {}", loopcnt, (written - read));
                }
                loopcnt++;

                // post event
                //EventBus.getDefault().post(new DataTransferredEvent(System.currentTimeMillis(), transferredSize, isWrite));
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

        // TODO this has been moved from AbstractConnection, do we need to keep
        // it?
        // serverContext.getConnectionManager().updateConnection(this);
    }
}
