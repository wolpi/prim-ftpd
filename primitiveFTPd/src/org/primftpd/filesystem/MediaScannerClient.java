package org.primftpd.filesystem;

import android.content.Context;
import android.net.Uri;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaScannerClient implements MediaScannerConnectionClient {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final MediaScannerConnection connection;

    public MediaScannerClient(Context context) {
        this.connection = new MediaScannerConnection(context, this);

        try {
            // Android 11 (API level 30) and higher: this makes the connection, and isConnected() is constant true from now on
            // Android 10 (API level 29) and lower: this initiates the connection, and we have to wait onMediaScannerConnected() before calling scanFile()
            connection.connect();
        } catch (Exception e) {
            logger.warn("media scanner connection error (reconnecting later on first use) '{}'", e.toString());
        }
    }

    private void ensureConnected() {
        synchronized (connection) {
            // Android 11 (API level 30) and higher: isConnected() is constant true (we called connect() in the ctor above)
            // Android 10 (API level 29) and lower: we have to wait onMediaScannerConnected() before calling scanFile()
            while (!connection.isConnected()) {
                try {
                    // calling connect() multiple times, even on an already connected connection, doesn't cause any problem
                    connection.connect();
                    // if we somehow miss a notifyAll(), better to use a timeout and retry
                    connection.wait(500);
                } catch (Exception e) {
                    logger.warn("  media scanner connection error (reconnecting) '{}'", e.toString());
                }
            }
        }
    }

    @Override
    public void onMediaScannerConnected() {
        synchronized (connection) {
            connection.notifyAll();
        }
    }

    public void scanFile(String path) {
        logger.info("media scanning started for file '{}'", path);
        try {
            ensureConnected();
            // Android 11 (API level 30) and higher: executes the scan task asynchronously
            // Android 10 (API level 29) and lower: just requests the scan
            connection.scanFile(path, null);
        } catch (Exception e) {
            logger.error("  media scanning start error for file '{}': '{}' ", e, path);
        }
    }

    @Override
    public void onScanCompleted(String path, Uri uri) {
        logger.info("media scanning completed for file '{}'", path);
    }
}
