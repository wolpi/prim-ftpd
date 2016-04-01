package org.primftpd.services;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;

/**
 * Handles starting and stopping of Servers, including {@link WakeLock}.
 *
 */
public class ServerServiceHandler extends Handler
{
	private static final String APP_NAME = "pFTPd";

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private final WeakReference<AbstractServerService> serviceRef;
	private final String logName;

	private static WakeLock wakeLock;


	protected ServerServiceHandler(
		Looper looper,
		AbstractServerService service,
		String logName)
	{
		super(looper);
		this.serviceRef = new WeakReference<AbstractServerService>(service);
		this.logName = logName;
	}

	@Override
	public void handleMessage(Message msg) {
		logger.debug("handleMessage()");

		AbstractServerService service = serviceRef.get();
		if (service == null) {
			logger.warn("serviceRef is null ({})", logName);
			return;
		}

		int toDo = msg.arg1;
		if (toDo == AbstractServerService.MSG_START) {
			handleStart(service);

		} else if (toDo == AbstractServerService.MSG_STOP) {
			handleStop(service);
		}
	}

	protected void handleStart(AbstractServerService service)
	{
		if (service.getServer() == null) {
			logger.debug("starting {} server", logName);

			// XXX set properties to prefer IPv4 to run in simulator
			System.setProperty("java.net.preferIPv4Stack", "true");
			System.setProperty("java.net.preferIPv6Addresses", "false");

			boolean started = service.launchServer();

			if (started && service.getServer() != null) {
				service.createStatusbarNotification();

				PowerManager powerMgr =
					(PowerManager) service.getSystemService(
						AbstractServerService.POWER_SERVICE);
				obtainWakeLock(powerMgr, service.prefsBean.isWakelock());

				if (service.prefsBean.isAnnounce()) {
					service.announceService();
				}
			} else {
				service.stopSelf();

				// tell activity to update button states
				Intent intent = new Intent(
					AbstractServerService.BROADCAST_ACTION_COULD_NOT_START);
				service.sendBroadcast(intent);
			}
		}
	}

	protected void handleStop(AbstractServerService service)
	{
		if (service.getServer() != null) {
			logger.debug("stopping {} server", logName);
			service.stopServer();

			if (service.prefsBean.isAnnounce()) {
				service.unannounceService();
			}
		}
		if (service.getServer() == null) {
			service.removeStatusbarNotification();
		}
		releaseWakeLock();
		logger.debug("stopSelf ({})", logName);
		service.stopSelf();
	}

	private synchronized void obtainWakeLock(
		PowerManager powerMgr,
		boolean takeWakeLock)
	{
		// acquire wake lock for CPU to still handle requests
		// note: PARTIAL_WAKE_LOCK is not enough
		if (wakeLock == null && takeWakeLock) {
			logger.debug("acquiring wake lock ({})", logName);
			wakeLock = powerMgr.newWakeLock(
				PowerManager.SCREEN_DIM_WAKE_LOCK,
				APP_NAME);
			wakeLock.acquire();
		} else {
			if (takeWakeLock) {
				logger.debug("wake lock already taken ({})", logName);
			} else {
				logger.debug("wake lock disabled ({})", logName);
			}
		}
	}

	private synchronized void releaseWakeLock() {
		if (wakeLock != null) {
			logger.debug("releasing wake lock ({})", logName);
			wakeLock.release();
			wakeLock = null;
		} else {
			logger.debug("wake lock already released ({})", logName);
		}
	}
}
