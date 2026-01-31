package org.primftpd.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.primftpd.events.ClientActionEvent;
import org.primftpd.events.ServerInfoRequestEvent;
import org.primftpd.events.ServerInfoResponseEvent;
import org.primftpd.events.ServerStateChangedEvent;
import org.primftpd.prefs.LoadPrefsUtil;
import org.primftpd.prefs.PrefsBean;
import org.primftpd.R;
import org.primftpd.share.QuickShareBean;
import org.primftpd.util.KeyFingerprintProvider;
import org.primftpd.util.ServicesStartStopUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;

import eu.chainfire.libsuperuser.Shell;

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
	extends Service implements PftpdService
{
	protected static final int MSG_START = 1;
	protected static final int MSG_STOP = 2;

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private ServerServiceHandler serviceHandler;
	PrefsBean prefsBean;
	KeyFingerprintProvider keyFingerprintProvider;
	QuickShareBean quickShareBean;
	String chosenIp;
	private NsdManager.RegistrationListener nsdRegistrationListener;

	private Handler timerHandler;
	private long timerTimeout;
	private final Runnable timerTask = () -> {
		logger.info("stopping server due to idle timeout");
		ServicesStartStopUtil.stopServers(AbstractServerService.this);
	};

	protected abstract ServerServiceHandler createServiceHandler(
		Looper serviceLooper,
		AbstractServerService service);

	protected abstract Object getServer();
	protected abstract boolean launchServer(final Shell.Interactive shell);
	protected abstract void stopServer();
	protected abstract int getPort();
	protected abstract String getServiceName();
	protected abstract ClientActionEvent.Protocol getProtocol();

	protected String getBindIp() {
		return chosenIp != null
				? chosenIp
				: prefsBean.getBindIp();
	}

	protected void handleServerStartError(Throwable e)
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

		// listen for events
		EventBus.getDefault().register(this);

		Looper serviceLooper = thread.getLooper();
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
		prefsBean = (PrefsBean)extras.get(ServicesStartStopUtil.EXTRA_PREFS_BEAN);
		keyFingerprintProvider = (KeyFingerprintProvider)extras.get(
				ServicesStartStopUtil.EXTRA_FINGERPRINT_PROVIDER);
		quickShareBean = (QuickShareBean)extras.get(ServicesStartStopUtil.EXTRA_QUICK_SHARE_BEAN);
		chosenIp = extras.getString(ServicesStartStopUtil.EXTRA_CHOSEN_IP);

		// send start message (to handler)
		Message msg = serviceHandler.obtainMessage();
		msg.arg1 = MSG_START;
		serviceHandler.sendMessage(msg);

		// post event
		EventBus.getDefault().post(new ServerStateChangedEvent());

		// handle server stop on idle timeout
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
		try {
			long timeout = Long.parseLong(sharedPreferences.getString(
					LoadPrefsUtil.PREF_KEY_IDLE_TIMEOUT_SERVER_STOP,
					LoadPrefsUtil.IDLE_TIMEOUT_SERVER_STOP_DEFAULT_VAL));
			if (timeout > 0) {
				this.timerTimeout = timeout * 60 * 1000;
				startTimer();
			}
		} catch (Exception e) {
			logger.error("could not start timer", e);
		}

		// we don't want the system to kill the ftp server
		//return START_NOT_STICKY;
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		logger.debug("onDestroy()");

		// send stop message (to handler)
		Message msg = serviceHandler.obtainMessage();
		msg.arg1 = MSG_STOP;
		serviceHandler.sendMessage(msg);

		// stop on idle timer not needed anymore
		if (this.timerHandler != null) {
			stopTimer();
		}

		// post event
		EventBus.getDefault().post(new ServerStateChangedEvent());

		// don't listen anymore
		EventBus.getDefault().unregister(this);
	}

	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(ServerInfoRequestEvent event) {
		logger.debug("got ServerInfoRequestEvent");
		int quickShareNumberOfFiles = quickShareBean != null ? quickShareBean.numberOfFiles() : -1;
		EventBus.getDefault().post(new ServerInfoResponseEvent(quickShareNumberOfFiles));
	}

	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(ClientActionEvent event) {
		if (timerHandler != null) {
			stopTimer();
			startTimer();
		}
	}

	private synchronized void stopTimer() {
		this.timerHandler.removeCallbacks(timerTask);
	}
	private synchronized void startTimer() {
		if (this.timerHandler == null) {
			this.timerHandler = new Handler();
		}
		this.timerHandler.postDelayed(timerTask, this.timerTimeout);
	}

	@Override
	public void postClientAction(
			ClientActionEvent.Storage storage,
			ClientActionEvent.ClientAction clientAction,
			String clientIp,
			String path,
			String error) {
		ClientActionEvent event = new ClientActionEvent(
				storage,
				getProtocol(),
				clientAction,
				new Date(),
				clientIp,
				path,
				error);
		logger.info("posting ClientActionEvent: {}", event);
		EventBus.getDefault().post(event);
	}

	@Override
	public PrefsBean getPrefsBean() {
		return prefsBean;
	}

	@Override
	public Context getContext() {
		return this;
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
            public void onRegistrationFailed(NsdServiceInfo serviceInfo,
                                             int errorCode) {
                logger.debug("onRegistrationFailed()");
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
                logger.debug("onServiceUnregistered()");
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo,
                                               int errorCode) {
                logger.debug("onUnregistrationFailed()");
            }
        };

        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        String servicename = "primitive ftpd";
        if (prefsBean != null) {
            servicename = prefsBean.getAnnounceName();
        } else {
            logger.error("prefsBean is null, falling back to default service name");
        }
        serviceInfo.setServiceName(servicename);
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

	protected void cleanQuickShareTmpDir() {
		if (quickShareBean != null) {
			File tmpDir = quickShareBean.getTmpDir();
			if (tmpDir != null && tmpDir.exists()) {
				File[] files = tmpDir.listFiles();
				if (files != null) {
					for (File child : files) {
						boolean deleted = child.delete();
						if (!deleted) {
							logger.info("could not delete tmp file: {}", child.getAbsolutePath());
						}
					}
				}
				boolean deleted = tmpDir.delete();
				if (!deleted) {
					logger.info("could not delete tmp dir: {}", tmpDir.getAbsolutePath());
				}
			}
		}
	}
}
