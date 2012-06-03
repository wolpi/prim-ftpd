package org.primftpd;

import java.security.KeyStore;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfiguration;
import org.primftpd.filesystem.AndroidFileSystemFactory;
import org.primftpd.util.KeyStoreUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.widget.Toast;

/**
 * Implements an FTP server.
 */
public class FtpServerService extends Service
{
	public static final String BROADCAST_ACTION_COULD_NOT_START = "org.primftpd.CouldNotStartServer";

	protected static final int MSG_START = 1;
	protected static final int MSG_STOP = 2;

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private FtpServer ftpServer;
	private Looper serviceLooper;
	private ServiceHandler serviceHandler;
	private PrefsBean prefsBean;

	/**
	 * Handles starting and stopping of FtpServer.
	 *
	 */
	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}
		@Override
		public void handleMessage(Message msg) {
			logger.debug("handleMessage()");

			int toDo = msg.arg1;
			if (toDo == MSG_START) {
				if (ftpServer == null) {
					logger.debug("starting ftp server");

					// XXX set properties to prefer IPv4 to run in simulator
					System.setProperty("java.net.preferIPv4Stack", "true");
		    		System.setProperty("java.net.preferIPv6Addresses", "false");

		    		launchFtpServer();

		    		if (ftpServer != null) {
						createStatusbarNotification();
		    		} else {
		    			stopSelf();

		    			// tell activity to update button states
		    			Intent intent = new Intent(BROADCAST_ACTION_COULD_NOT_START);
		    			sendBroadcast(intent);
		    		}
				}

			} else if (toDo == MSG_STOP) {
				if (ftpServer != null) {
					logger.debug("stopping ftp server");
					ftpServer.stop();
					ftpServer = null;
				}
				if (ftpServer == null) {
					removeStatusbarNotification();
				}
				logger.debug("stopSelf");
				stopSelf();
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
			"ServiceStartArguments",
            Process.THREAD_PRIORITY_BACKGROUND);
	    thread.start();

	    serviceLooper = thread.getLooper();
	    serviceHandler = new ServiceHandler(serviceLooper);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		logger.debug("onStartCommand()");

		// get parameters
		Bundle extras = intent.getExtras();
		String userName = extras.getString(
			PrimitiveFtpdActivity.EXTRA_USERNAME);
		String password = extras.getString(
			PrimitiveFtpdActivity.EXTRA_PASSWORD);
		int port = extras.getInt(
			PrimitiveFtpdActivity.EXTRA_PORT);
		int sslPort = extras.getInt(
			PrimitiveFtpdActivity.EXTRA_SSL_PORT);
		prefsBean = new PrefsBean(userName, password, port, sslPort);

		// send start message
		Message msg = serviceHandler.obtainMessage();
		msg.arg1 = MSG_START;
		serviceHandler.sendMessage(msg);

		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		logger.debug("onDestroy()");

		Message msg = serviceHandler.obtainMessage();
		msg.arg1 = MSG_STOP;
		serviceHandler.sendMessage(msg);
	}

	/**
	 * Creates Statusbar Notification.
	 */
	protected void createStatusbarNotification() {
		// create notification
		int icon = R.drawable.ic_launcher;
		CharSequence tickerText = getText(R.string.serverRunning);
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);
		notification.flags = Notification.FLAG_NO_CLEAR;

		// create pending intent
		Context context = getApplicationContext();
		CharSequence contentTitle = getText(R.string.notificationTitle);
		CharSequence contentText = tickerText;
		Intent notificationIntent = new Intent(this, PrimitiveFtpdActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

		// notification manager
		NotificationUtil.createStatusbarNotification(this, notification);
	}

	/**
	 * Removes Statusbar Notification.
	 */
	protected void removeStatusbarNotification() {
		NotificationUtil.removeStatusbarNotification(this);
	}

	/**
     * Launches FTP server.
     */
    protected void launchFtpServer() {
    	ListenerFactory listenerFactory = new ListenerFactory();
    	listenerFactory.setPort(prefsBean.getPort());

    	FtpServerFactory serverFactory = new FtpServerFactory();
    	serverFactory.addListener("default", listenerFactory.createListener());

    	// user manager & file system
    	serverFactory.setUserManager(new AndroidPrefsUserManager(prefsBean));
    	serverFactory.setFileSystem(new AndroidFileSystemFactory());

    	// ssl listener
    	KeyStore keyStore = KeyStoreUtil.loadKeyStore(getResources());
    	if (keyStore != null) {
	    	SslConfiguration sslConfig = KeyStoreUtil.createSslConfiguration(keyStore);
	    	if (sslConfig != null) {
		    	ListenerFactory sslListenerFactory = new ListenerFactory();
		    	sslListenerFactory.setPort(prefsBean.getSslPort());
		    	sslListenerFactory.setImplicitSsl(true);
		    	sslListenerFactory.setSslConfiguration(sslConfig);
		    	serverFactory.addListener("ssl", sslListenerFactory.createListener());
	    	}
    	}

    	// do start server
    	ftpServer = serverFactory.createServer();
    	try {
    		ftpServer.start();
    	} catch (Exception e) {
    		// note: createServer() throws RuntimeExceptions, too
			logger.error("could not start server", e);

			ftpServer = null;

			String msg = getText(R.string.serverCouldNotBeStarted).toString();
			msg += e.getLocalizedMessage();
			Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
		}
    }
}
