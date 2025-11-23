package org.primftpd.services;

import android.app.Notification;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import org.primftpd.filesystem.Utils;
import org.primftpd.prefs.StorageType;
import org.primftpd.util.NotificationUtil;
import org.primftpd.util.ServicesStartStopUtil;
import org.primftpd.util.WakelockUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;

import eu.chainfire.libsuperuser.Shell;

/**
 * Handles starting and stopping of Servers, including {@link WakeLock}.
 *
 */
public class ServerServiceHandler extends Handler
{
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private final WeakReference<AbstractServerService> serviceRef;
	private final String logName;

	private static Shell.Interactive shell;


	protected ServerServiceHandler(
		Looper looper,
		AbstractServerService service,
		String logName)
	{
		super(looper);
		this.serviceRef = new WeakReference<>(service);
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

			StorageType storageType = service.prefsBean.getStorageType();
			if (storageType == StorageType.ROOT || storageType == StorageType.VIRTUAL) {
				shellOpen();
			}

			boolean started = service.launchServer(shell);

			if (started && service.getServer() != null) {
				PowerManager powerMgr =
					(PowerManager) service.getSystemService(
						AbstractServerService.POWER_SERVICE);
				obtainWakeLock(powerMgr, service.prefsBean.isWakelock());

				if (service.prefsBean.isAnnounce()) {
					service.announceService();
				}

				// make service high priority
				Notification notification = ServicesStartStopUtil.updateNonActivityUI(
						service,
						true,
						service.prefsBean,
						service.keyFingerprintProvider,
						service.quickShareBean);
				service.startForeground(NotificationUtil.NOTIFICATION_ID, notification);
			} else {
				service.stopSelf();
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

			service.cleanQuickShareTmpDir();
		}
		releaseWakeLock();
		shellClose();
		logger.debug("stopSelf ({})", logName);
		service.stopSelf();
		ServicesStartStopUtil.updateNonActivityUI(
				service,
				false,
				service.prefsBean,
				null,
				null);
	}

	private synchronized void obtainWakeLock(
		PowerManager powerMgr,
		boolean takeWakeLock)
	{
		// acquire wake lock for CPU to still handle requests
		if (takeWakeLock) {
			WakelockUtil.obtainWakeLock(powerMgr);
		} else {
			logger.debug("wake lock disabled ({})", logName);
		}
	}

	private synchronized void releaseWakeLock() {
		WakelockUtil.releaseWakeLock();
	}

	private synchronized void shellOpen() {
		if (shell == null) {
			logger.debug("opening root shell ({})", logName);
			// TODO test .setShell()
			Shell.Builder builder = new Shell.Builder();
			if (!Utils.RUN_TESTS) {
				builder.useSU();
			}
			shell = builder.open();
		} else {
			logger.debug("root shell already open ({})", logName);
		}
	}

	private synchronized void shellClose() {
		if (shell != null) {
			logger.debug("closing root shell ({})", logName);
			try {
				shell.close();
			} catch (Exception e) {
				logger.warn("error on closing shell", e);
			}
			shell = null;
		}
	}
}
