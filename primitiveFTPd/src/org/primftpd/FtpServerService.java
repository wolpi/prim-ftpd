package org.primftpd;

import java.lang.ref.WeakReference;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.listener.ListenerFactory;
import org.primftpd.filesystem.AndroidFileSystemFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
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
	private WakeLock wakeLock;
	private NsdManager.RegistrationListener registrationListener;

	/**
	 * Handles starting and stopping of FtpServer.
	 *
	 */
	private final static class ServiceHandler extends Handler {

		protected final Logger logger = LoggerFactory.getLogger(getClass());

		private final WeakReference<FtpServerService> ftpServiceRef;

		public ServiceHandler(Looper looper, FtpServerService ftpService) {
			super(looper);
			this.ftpServiceRef = new WeakReference<FtpServerService>(ftpService);
		}
		@Override
		public void handleMessage(Message msg) {
			logger.debug("handleMessage()");

			FtpServerService ftpService = ftpServiceRef.get();
			if (ftpService == null) {
				logger.warn("ftpServiceRef is null");
				return;
			}

			int toDo = msg.arg1;
			if (toDo == MSG_START) {
				if (ftpService.ftpServer == null) {
					logger.debug("starting ftp server");

					// XXX set properties to prefer IPv4 to run in simulator
					System.setProperty("java.net.preferIPv4Stack", "true");
		    		System.setProperty("java.net.preferIPv6Addresses", "false");

		    		ftpService.launchFtpServer();

		    		if (ftpService.ftpServer != null) {
		    			ftpService.createStatusbarNotification();

						// acquire wake lock for CPU to still handle requests
						// note: PARTIAL_WAKE_LOCK is not enough
						logger.debug("acquiring wake lock");
						PowerManager powerMgr =
								(PowerManager) ftpService.getSystemService(POWER_SERVICE);
						ftpService.wakeLock = powerMgr.newWakeLock(
								PowerManager.SCREEN_DIM_WAKE_LOCK,
								"pFTPd");
						ftpService.wakeLock.acquire();

						ftpService.registerService();
		    		} else {
		    			ftpService.stopSelf();

		    			// tell activity to update button states
		    			Intent intent = new Intent(BROADCAST_ACTION_COULD_NOT_START);
		    			ftpService.sendBroadcast(intent);
		    		}
				}

			} else if (toDo == MSG_STOP) {
				if (ftpService.ftpServer != null) {
					logger.debug("stopping ftp server");
					ftpService.ftpServer.stop();
					ftpService.ftpServer = null;
					ftpService.unregisterService();
				}
				if (ftpService.ftpServer == null) {
					ftpService.removeStatusbarNotification();
				}
				if (ftpService.wakeLock != null) {
					logger.debug("releasing wake lock");
					ftpService.wakeLock.release();
					ftpService.wakeLock = null;
				}
				logger.debug("stopSelf");
				ftpService.stopSelf();
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
	    serviceHandler = new ServiceHandler(serviceLooper, this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		logger.debug("onStartCommand()");

		if (intent == null) {
			logger.warn("intent is null in onStartCommand()");

			return START_REDELIVER_INTENT;
		}

		// get parameters
		Bundle extras = intent.getExtras();
		prefsBean = (PrefsBean)extras.get(PrimitiveFtpdActivity.EXTRA_PREFS_BEAN);

		// send start message
		Message msg = serviceHandler.obtainMessage();
		msg.arg1 = MSG_START;
		serviceHandler.sendMessage(msg);

		// we don't want the system to kill the ftp server
		//return START_NOT_STICKY;
		return START_STICKY;
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
		// create pending intent
		Intent notificationIntent = new Intent(this, PrimitiveFtpdActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		// create notification
		int icon = R.drawable.ic_launcher;
		CharSequence tickerText = getText(R.string.serverRunning);
		CharSequence contentTitle = getText(R.string.notificationTitle);
		CharSequence contentText = tickerText;

		long when = System.currentTimeMillis();

		Notification notification = new Notification.Builder(getApplicationContext())
			.setTicker(tickerText)
			.setContentTitle(contentTitle)
			.setContentText(contentText)
			.setSmallIcon(icon)
			.setContentIntent(contentIntent)
			.setWhen(when)
			.build();


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

    	// XXX SSL
    	// ssl listener
//    	KeyStore keyStore = KeyStoreUtil.loadKeyStore(getResources());
//    	if (keyStore != null) {
//	    	SslConfiguration sslConfig = KeyStoreUtil.createSslConfiguration(keyStore);
//	    	if (sslConfig != null) {
//		    	ListenerFactory sslListenerFactory = new ListenerFactory();
//		    	sslListenerFactory.setPort(prefsBean.getSslPort());
//		    	sslListenerFactory.setImplicitSsl(true);
//		    	sslListenerFactory.setSslConfiguration(sslConfig);
//		    	serverFactory.addListener("ssl", sslListenerFactory.createListener());
//	    	}
//    	}

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

    // Register a DNS-SD service (to be discoverable through Bonjour/Avahi)
	void registerService () {
		NsdServiceInfo serviceInfo  = new NsdServiceInfo();
		serviceInfo.setServiceName("primitive ftpd");
		serviceInfo.setServiceType("_ftp._tcp.");
		serviceInfo.setPort(prefsBean.getPort());

		NsdManager nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);

		registrationListener = new NsdManager.RegistrationListener() {
				@Override
				public void onServiceRegistered(NsdServiceInfo serviceInfo) {}
				@Override
				public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {}
				@Override
				public void onServiceUnregistered(NsdServiceInfo serviceInfo) {}
				@Override
				public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {}
			};
		nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
	}

	void unregisterService () {
		NsdManager nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
		nsdManager.unregisterService(registrationListener);
	}
}
