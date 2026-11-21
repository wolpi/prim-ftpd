package org.primftpd.services;

import android.app.Notification;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import org.primftpd.filesystem.Utils;
import org.primftpd.prefs.StorageType;
import org.primftpd.util.KeyFingerprintProvider;
import org.primftpd.util.NotificationUtil;
import org.primftpd.util.ServicesStartStopUtil;
import org.primftpd.util.WakelockUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

	// root shell and wake lock are shared by ftp and sftp service, each of them has its own handler (thread)
	private static final Object SHARED_STATE_LOCK = new Object();
	private static Shell.Interactive shell;
	private static int shellUsers = 0;
	private static int wakeLockUsers = 0;
	private static int serversRunning = 0;

	// what the service of this handler has contributed to shared state
	private boolean usesShell = false;
	private boolean usesWakeLock = false;
	private boolean serverRunning = false;


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
		boolean running = service.getServer() != null;
		if (!running) {
			logger.debug("starting {} server", logName);

			// XXX set properties to prefer IPv4 to run in simulator
			System.setProperty("java.net.preferIPv4Stack", "true");
			System.setProperty("java.net.preferIPv6Addresses", "false");

			boolean started = false;
			try {
				StorageType storageType = service.prefsBean.getStorageType();
				if (storageType == StorageType.ROOT || storageType == StorageType.VIRTUAL) {
					shellOpen();
				}

				if (storageType == StorageType.ROOT && !isShellRunning()) {
					// device is not rooted (or root access has been denied): no file could be accessed
					service.handleServerStartError(new IOException(
							"root access is not available, choose another storage type"));
				} else {
					started = service.launchServer(shell);
				}
			} catch (RuntimeException e) {
				// must not kill the handler thread, service would be left in undefined state
				service.handleServerStartError(e);
			}
			running = started && service.getServer() != null;

			if (running) {
				synchronized (SHARED_STATE_LOCK) {
					if (!serverRunning) {
						serverRunning = true;
						serversRunning++;
					}
				}
				PowerManager powerMgr =
					(PowerManager) service.getSystemService(
						AbstractServerService.POWER_SERVICE);
				obtainWakeLock(powerMgr, service.prefsBean.isWakelock());

				if (service.prefsBean.isAnnounce()) {
					service.announceService();
				}
			}
		}

		// Service is started by startForegroundService(): android requires a call of startForeground()
		// for every such start, otherwise the app is crashed. This includes starts which fail
		// (e.g. port in use, no host key) and requests to start a server which is running already.
		if (running) {
			// make service high priority
			Notification notification = ServicesStartStopUtil.updateNonActivityUI(
					service,
					true,
					service.prefsBean,
					service.keyFingerprintProvider,
					service.quickShareBean,
					service.chosenIp);
			service.startForeground(NotificationUtil.NOTIFICATION_ID, notification);
		} else {
			try {
				Notification notification = NotificationUtil.createStatusbarNotification(
						service,
						service.prefsBean,
						service.keyFingerprintProvider != null
								? service.keyFingerprintProvider
								: new KeyFingerprintProvider(),
						service.quickShareBean,
						service.chosenIp);
				service.startForeground(NotificationUtil.NOTIFICATION_ID, notification);
				service.stopForeground(android.app.Service.STOP_FOREGROUND_REMOVE);
			} catch (Exception e) {
				logger.warn("could not fulfill foreground contract for failed start ({}): {}", logName, e.toString());
			}
			service.stopSelf();
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
		boolean otherServerRunning;
		synchronized (SHARED_STATE_LOCK) {
			if (serverRunning) {
				serverRunning = false;
				serversRunning--;
			}
			otherServerRunning = serversRunning > 0;
		}
		releaseWakeLock();
		shellClose();
		logger.debug("stopSelf ({})", logName);
		service.stopSelf();
		if (otherServerRunning) {
			// e.g. this server could not be started while the other one could:
			// widget and notification must keep showing a running server
			logger.debug("other server is still running, not updating UI to stopped ({})", logName);
		} else {
			ServicesStartStopUtil.updateNonActivityUI(
					service,
					false,
					service.prefsBean,
					null,
					null,
					null);
		}
	}

	private void obtainWakeLock(
		PowerManager powerMgr,
		boolean takeWakeLock)
	{
		// acquire wake lock for CPU to still handle requests
		if (takeWakeLock) {
			synchronized (SHARED_STATE_LOCK) {
				if (!usesWakeLock) {
					usesWakeLock = true;
					wakeLockUsers++;
				}
				WakelockUtil.obtainWakeLock(powerMgr);
			}
		} else {
			logger.debug("wake lock disabled ({})", logName);
		}
	}

	private void releaseWakeLock() {
		synchronized (SHARED_STATE_LOCK) {
			if (usesWakeLock) {
				usesWakeLock = false;
				wakeLockUsers--;
			}
			// the other server may still need it
			if (wakeLockUsers <= 0) {
				wakeLockUsers = 0;
				WakelockUtil.releaseWakeLock();
			} else {
				logger.debug("wake lock still in use by other server ({})", logName);
			}
		}
	}

	private void shellOpen() {
		synchronized (SHARED_STATE_LOCK) {
			if (!usesShell) {
				usesShell = true;
				shellUsers++;
			}
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
	}

	private boolean isShellRunning() {
		synchronized (SHARED_STATE_LOCK) {
			try {
				return shell != null && shell.isRunning();
			} catch (Exception e) {
				return false;
			}
		}
	}

	private void shellClose() {
		synchronized (SHARED_STATE_LOCK) {
			if (usesShell) {
				usesShell = false;
				shellUsers--;
			}
			if (shellUsers > 0) {
				// the other server still runs commands with it
				logger.debug("root shell still in use by other server ({})", logName);
				return;
			}
			shellUsers = 0;
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
}
