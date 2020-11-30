package org.primftpd.services;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.widget.Toast;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.Header;
import org.primftpd.util.Defaults;
import org.primftpd.util.FilenameUnique;
import org.primftpd.util.NotificationUtil;
import org.primftpd.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;

public class DownloadsService extends Service {

    public static final String ACTION_START = "org.primftpd.download.START";
    public static final String ACTION_STOP = "org.primftpd.download.STOP";
    protected static final int MSG_START = 1;
    protected static final int MSG_STOP = 2;
    public static final String URL = "url";

    private static final int BUF_SIZE = 4096;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected static Handler serviceHandler;

    private String localPath = "";
    private String filename = "";
    private boolean stopped = false;
    private boolean finished = false;

    static class DownloadsHandler extends Handler {

        protected final Logger logger = LoggerFactory.getLogger(getClass());

        private final WeakReference<DownloadsService> serviceRef;

        DownloadsHandler(Looper looper, DownloadsService service) {
            super(looper);
            this.serviceRef = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            logger.debug("handleMessage()");

            DownloadsService service = serviceRef.get();

            if (msg.what == MSG_START) {
                String urlStr = msg.getData().getString(URL);
                service.download(urlStr);

            } else if (msg.what == MSG_STOP) {
                // -> seems not to be delivered
                logger.info("signaling to stop download, filename: {}", service.filename);
                service.stopped = true;
                service.stopSelf();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread(
                "DownloadsService",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        Looper serviceLooper = thread.getLooper();
        serviceHandler = new DownloadsHandler(serviceLooper, this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logger.debug("onStartCommand()");

        if (intent == null) {
            logger.warn("intent is null in onStartCommand()");

            return START_REDELIVER_INTENT;
        }

        Message msg;
        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            logger.debug("  action start");
            msg = Message.obtain(serviceHandler, MSG_START);
            msg.setData(intent.getExtras());

            // build local path
            try {
                URL url = new URL(intent.getExtras().getString(URL));
                String urlPath = url.getPath();
                filename = "download";
                if (urlPath != null) {
                    String[] split = urlPath.split("/");
                    if (split.length > 0) {
                        filename = split[split.length - 1];
                    }
                }
                localPath = Defaults.DOWNLOADS_DIR + "/" + filename;
            } catch (Exception e) {
                logger.error("", e);
            }
        } else {
            logger.debug("  action stop");
            msg = Message.obtain(serviceHandler, MSG_STOP);
            // -> seems never to be entered
        }
        serviceHandler.sendMessage(msg);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        logger.debug("onDestroy()");

        // -> trying to send a message here seems not to work

        //serviceHandler.sendMessage(Message.obtain(serviceHandler, MSG_STOP));

        //Message msg = serviceHandler.obtainMessage();
        //msg.arg1 = MSG_STOP;
        //serviceHandler.sendMessage(msg);

        // reset notification
        if (!finished) {
            NotificationUtil.removeDownloadNotification(this);
            NotificationUtil.createDownloadNotification(
                    this,
                    filename,
                    localPath,
                    true,
                    false,
                    0,
                    0);

            // stop download
            stopped = true;
        }
        stopSelf();
    }

    private void download(String urlStr) {
        CloseableHttpClient httpclient = null;
        CloseableHttpResponse response = null;
        try {
            httpclient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(urlStr);
            response = httpclient.execute(httpGet);

            // log headers
            long filesize = 0;
            String contentType = null;
            logger.debug("headers of {}", urlStr);
            logger.debug("status code {}", response.getCode());
            Header[] headers = response.getHeaders();
            for (Header header : headers) {
                String headerName = header.getName();
                String val = header.getValue();
                if ("Content-Length".equals(headerName)) {
                    try {
                        filesize = Long.parseLong(val);
                    } catch (Exception e) {
                        logger.info("could not parse download size: {}", val);
                    }
                } else if ("Content-Type".equals(headerName)) {
                    contentType = val;
                }
                logger.debug("header: {} = {}", headerName, val);
            }

            Header locationHeader = response.getHeader("Location");
            String location = locationHeader != null ? locationHeader.getValue() : null;
            if (StringUtils.isNotEmpty(location)) {
                logger.debug("url {} redirected", location);
                download(location);
            } else {

                // build local path
                Uri uri = Uri.parse(urlStr);
                filename = FilenameUnique.filename(uri, null, contentType, Defaults.DOWNLOADS_DIR, this);
                localPath = Defaults.DOWNLOADS_DIR + "/" + this.filename;

                // open streams
                InputStream inputStream = new BufferedInputStream(response.getEntity().getContent());
                FileOutputStream fos = new FileOutputStream(localPath);

                logger.info("downloading {} to file {}", urlStr, localPath);

                // copy
                byte[] buf = new byte[BUF_SIZE];
                long bytes = 0;
                while (!stopped) {
                    int r = inputStream.read(buf);
                    bytes += r;
                    if (r == -1) {
                        logger.info("download finished");
                        finished = true;
                        break;
                    }
                    //logger.trace("download: transferring num bytes: {}", r);
                    NotificationUtil.createDownloadNotification(
                            this,
                            this.filename,
                            localPath,
                            false,
                            false,
                            bytes,
                            filesize);
                    fos.write(buf, 0, r);
                }
                if (stopped && !finished) {
                    logger.info("download stopped");
                } else if (finished) {
                    NotificationUtil.removeDownloadNotification(this);
                    NotificationUtil.createDownloadNotification(
                            this,
                            this.filename,
                            localPath,
                            false,
                            true,
                            0,
                            0);
                }
            }
        } catch (final Exception e) {
            logger.error("could not download", e);
            Toast.makeText(this, "could not download: " + e.getMessage(),Toast.LENGTH_LONG).show();
            NotificationUtil.removeDownloadNotification(this);
            NotificationUtil.createDownloadNotification(
                    this,
                    filename,
                    localPath,
                    true,
                    false,
                    0,
                    0);
        } finally {
            close(response);
            close(httpclient);
        }
    }

    private void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            logger.warn("exception while closing", e);
        }
    }
}
