package org.primftpd.services;

import org.primftpd.PrefsBean;
import org.primftpd.PrimitiveFtpdActivity;
import org.primftpd.R;
import org.primftpd.util.NotificationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.widget.Toast;

/**
 * Abstract base class for {@link Service}s wrapping servers.
 * <div>
 * Implements:
 * <ul>
 * 		<li>android lifecycle</li>
 * 		<li>statusbar notifications</li>
 * 		<li>bonjour/zeroconf announcements</li>
 * </ul>
 * </div>
 *
 */
public abstract class AbstractServerService
	extends Service
{
	public static final String BROADCAST_ACTION_COULD_NOT_START =
		"org.primftpd.CouldNotStartServer";

	protected static final int MSG_START = 1;
	protected static final int MSG_STOP = 2;

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private Looper serviceLooper;
	private ServerServiceHandler serviceHandler;
	PrefsBean prefsBean;
	private NsdManager.RegistrationListener nsdRegistrationListener;

	protected abstract ServerServiceHandler createServiceHandler(
		Looper serviceLooper,
		AbstractServerService service);

	protected abstract Object getServer();
	protected abstract void launchServer();
	protected abstract void stopServer();
	protected abstract int getPort();
	protected abstract String getServiceName();

	protected void handleServerStartError(Exception e)
	{
		logger.error("could not start server", e);

		String msg = getText(R.string.serverCouldNotBeStarted).toString();
		msg += e.getLocalizedMessage();
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
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
	    serviceHandler = createServiceHandler(serviceLooper, this);
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
		int icon = R.drawable.ic_notification;
		CharSequence tickerText = getText(R.string.serverRunning);
		CharSequence contentTitle = getText(R.string.notificationTitle);
		CharSequence contentText = tickerText;

		// use main icon as large one
		Bitmap largeIcon = BitmapFactory.decodeResource(
			getResources(),
			R.drawable.ic_launcher);

		long when = System.currentTimeMillis();

		Notification notification = new Notification.Builder(getApplicationContext())
			.setTicker(tickerText)
			.setContentTitle(contentTitle)
			.setContentText(contentText)
			.setSmallIcon(icon)
			.setLargeIcon(largeIcon)
			.setContentIntent(contentIntent)
			.setWhen(when)
			.build();

		notification.flags |= Notification.FLAG_NO_CLEAR;

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
     * Register a DNS-SD service (to be discoverable through Bonjour/Avahi).
     */
    protected void announceService () {
		nsdRegistrationListener = new NsdManager.RegistrationListener() {
			@Override
			public void onServiceRegistered(NsdServiceInfo serviceInfo) {
				logger.debug("onServiceRegistered()");
			}
			@Override
			public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
				logger.debug("onRegistrationFailed()");
			}
			@Override
			public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
				logger.debug("onServiceUnregistered()");
			}
			@Override
			public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
				logger.debug("onUnregistrationFailed()");
			}
		};

		NsdServiceInfo serviceInfo  = new NsdServiceInfo();
		serviceInfo.setServiceName("primitive ftpd");
		serviceInfo.setServiceType("_" + getServiceName() + "._tcp.");
		serviceInfo.setPort(getPort());

		NsdManager nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);

		nsdManager.registerService(
			serviceInfo,
			NsdManager.PROTOCOL_DNS_SD,
			nsdRegistrationListener);
	}

	protected void unannounceService () {
		NsdManager nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
		nsdManager.unregisterService(nsdRegistrationListener);
	}
}
