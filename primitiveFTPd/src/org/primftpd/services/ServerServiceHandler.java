package org.primftpd.services;

import java.lang.ref.WeakReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

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

			service.launchServer();

			if (service.getServer() != null) {
				service.createStatusbarNotification();

				// acquire wake lock for CPU to still handle requests
				// note: PARTIAL_WAKE_LOCK is not enough
				logger.debug("acquiring wake lock ({})", logName);
				PowerManager powerMgr =
					(PowerManager) service.getSystemService(
						AbstractServerService.POWER_SERVICE);
				service.wakeLock = powerMgr.newWakeLock(
					PowerManager.SCREEN_DIM_WAKE_LOCK,
					APP_NAME);
				service.wakeLock.acquire();

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
		if (service.wakeLock != null) {
			logger.debug("releasing wake lock ({})", logName);
			service.wakeLock.release();
			service.wakeLock = null;
		}
		logger.debug("stopSelf ({})", logName);
		service.stopSelf();
	}
}
