package org.primftpd;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.ftpserver.util.IoUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.primftpd.log.PrimFtpdLoggerBinder;
import org.primftpd.prefs.AboutActivity;
import org.primftpd.prefs.FtpPrefsActivityThemeDark;
import org.primftpd.prefs.FtpPrefsActivityThemeLight;
import org.primftpd.prefs.LoadPrefsUtil;
import org.primftpd.prefs.Logging;
import org.primftpd.prefs.Theme;
import org.primftpd.util.KeyGenerator;
import org.primftpd.util.KeyInfoProvider;
import org.primftpd.util.NotificationUtil;
import org.primftpd.util.PrngFixes;
import org.primftpd.util.ServersRunningBean;
import org.primftpd.util.ServicesStartStopUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Enumeration;

/**
 * Activity to display network info and to start FTP service.
 */
public class PrimitiveFtpdActivity extends Activity {

	private BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {
		@Override
	 	public void onReceive(Context context, Intent intent) {
		logger.debug("network connectivity changed, data str: '{}', action: '{}'",
			intent.getDataString(),
			intent.getAction());
		showAddresses();
		}
	};

	// flag must be static to be avail after activity change
	private static boolean prefsChanged = false;
	private OnSharedPreferenceChangeListener prefsChangeListener =
		new OnSharedPreferenceChangeListener()
	{
		@Override public void onSharedPreferenceChanged(
			SharedPreferences sharedPreferences, String key)
		{
			logger.debug("onSharedPreferenceChanged(), key: {}", key);
			prefsChanged = true;
		}
	};

	public static final String PUBLICKEY_FILENAME = "pftpd-pub.bin";
	public static final String PRIVATEKEY_FILENAME = "pftpd-priv.pk8";

	public static final String DIALOG_TAG = "dialogs";

	protected Logger logger = LoggerFactory.getLogger(getClass());

	private PrefsBean prefsBean;
	private Theme theme;
	private ServersRunningBean serversRunning;
	private boolean keyPresent = false;
	private String fingerprintMd5 = " - ";
	private String fingerprintSha1 = " - ";
	private String fingerprintSha256 = " - ";
	private long timestampOfLastEvent = 0;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// basic setup
		super.onCreate(savedInstanceState);

		logger.debug("onCreate()");

		// fixes/workarounds for android security issue below 4.3 regarding key generation
		PrngFixes.apply();

		// prefs change
		SharedPreferences prefs = LoadPrefsUtil.getPrefs(getBaseContext());
		prefs.registerOnSharedPreferenceChangeListener(prefsChangeListener);

		// layout & theme
		theme = LoadPrefsUtil.theme(prefs);
		setTheme(theme.resourceId());
		setContentView(R.layout.main);

		// calc keys fingerprints
		calcPubkeyFingerprints();
		showKeyFingerprints();

		// create addresses label
		((TextView)findViewById(R.id.addressesLabel)).setText(
			String.format("%s (%s)", getText(R.string.ipAddrLabel), getText(R.string.ifacesLabel) )
		);

		// create ports label
		((TextView)findViewById(R.id.portsLabel)).setText(
			String.format("%s / %s / %s",
				getText(R.string.protocolLabel), getText(R.string.portLabel), getText(R.string.state))
		);

		// listen for events
		EventBus.getDefault().register(this);
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		// prefs change
		SharedPreferences prefs = LoadPrefsUtil.getPrefs(getBaseContext());
		prefs.unregisterOnSharedPreferenceChangeListener(prefsChangeListener);

		// server state change events
		EventBus.getDefault().unregister(this);
	}

	@Override
	protected void onStart() {
		super.onStart();

		logger.debug("onStart()");

		loadPrefs();
		showUsername();
		showAnonymousLogin();
	}

	@Override
	protected void onResume() {
		super.onResume();

		logger.debug("onResume()");

		// register listener to reprint interfaces table when network connections change
		IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(this.networkStateReceiver, filter);
	}

	@Override
	protected void onPause() {
		super.onPause();

		logger.debug("onPause()");

		// unregister broadcast receiver
		this.unregisterReceiver(this.networkStateReceiver);
	}

	protected FileInputStream buildPublickeyInStream() throws IOException {
		FileInputStream fis = openFileInput(PUBLICKEY_FILENAME);
		return fis;
	}

	protected FileOutputStream buildPublickeyOutStream() throws IOException {
		FileOutputStream fos = openFileOutput(PUBLICKEY_FILENAME, Context.MODE_PRIVATE);
		return fos;
	}

	protected FileInputStream buildPrivatekeyInStream() throws IOException {
		FileInputStream fis = openFileInput(PRIVATEKEY_FILENAME);
		return fis;
	}

	protected FileOutputStream buildPrivatekeyOutStream() throws IOException {
		FileOutputStream fos = openFileOutput(PRIVATEKEY_FILENAME, Context.MODE_PRIVATE);
		return fos;
	}

	/**
	 * Creates figerprints of public key.
	 */
	protected void calcPubkeyFingerprints() {
		FileInputStream fis = null;
		try {
			fis = buildPublickeyInStream();

			// check if key is present
			if (fis.available() <= 0) {
				keyPresent = false;
				throw new Exception("key seems not to be present");
			}

			KeyInfoProvider keyInfoprovider = new KeyInfoProvider();
			PublicKey pubKey = keyInfoprovider.readPublicKey(fis);
			RSAPublicKey rsaPubKey = (RSAPublicKey) pubKey;
			byte[] encodedKey = keyInfoprovider.encodeAsSsh(rsaPubKey);

			// fingerprints
			String fp = keyInfoprovider.fingerprint(encodedKey, "MD5");
			if (fp != null) {
				fingerprintMd5 = fp;
			}

			fp = keyInfoprovider.fingerprint(encodedKey, "SHA-1");
			if (fp != null) {
				fingerprintSha1 = fp;
			}

			fp = keyInfoprovider.fingerprint(encodedKey, "SHA-256");
			if (fp != null) {
				fingerprintSha256 = fp;
			}

			keyPresent = true;

		} catch (Exception e) {
			logger.debug("key does probably not exist");
		} finally {
			if (fis != null) {
				IoUtils.close(fis);
			}
		}
	}

	/**
	 * Creates table containing network interfaces.
	 */
	protected void showAddresses() {
		LinearLayout container = (LinearLayout)findViewById(R.id.addressesContainer);

		// clear old entries
		container.removeAllViews();

		try {
			Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
			while (ifaces.hasMoreElements()) {
				NetworkInterface iface = ifaces.nextElement();
				String ifaceDispName = iface.getDisplayName();
				String ifaceName = iface.getName();
				Enumeration<InetAddress> inetAddrs = iface.getInetAddresses();

				while (inetAddrs.hasMoreElements()) {
					InetAddress inetAddr = inetAddrs.nextElement();
					String hostAddr = inetAddr.getHostAddress();

					logger.debug("addr: '{}', iface name: '{}', disp name: '{}', loopback: '{}'",
						new Object[]{
							inetAddr,
							ifaceName,
							ifaceDispName,
							inetAddr.isLoopbackAddress()});

					if (inetAddr.isLoopbackAddress()) {
						continue;
					}

					String displayText = hostAddr + " (" + ifaceDispName + ")";
					if(displayText.contains("::")) {
						// Don't include the raw encoded names. Just the raw IP addresses.
						logger.debug("Skipping IPv6 address '{}'", displayText);
						continue;
					}
					TextView textView = new TextView(container.getContext());
					container.addView(textView);
					textView.setText(displayText);
					textView.setGravity(Gravity.CENTER_HORIZONTAL);
					textView.setTextIsSelectable(true);
				}
			}
		} catch (SocketException e) {
			logger.info("exception while iterating network interfaces", e);

			String msg = getText(R.string.ifacesError) + e.getLocalizedMessage();
			Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
		}
	}

	@SuppressLint("SetTextI18n")
	protected void showPortsAndServerState() {
		((TextView)findViewById(R.id.ftpTextView))
			.setText("ftp / " + prefsBean.getPortStr() + " / " +
			getText(serversRunning.ftp
				? R.string.serverStarted
				: R.string.serverStopped));

		((TextView)findViewById(R.id.sftpTextView))
				.setText("sftp / " + prefsBean.getSecurePortStr() + " / " +
						getText(serversRunning.ssh
								? R.string.serverStarted
								: R.string.serverStopped));
	}

	protected void showUsername() {
		TextView usernameView = (TextView)findViewById(R.id.usernameTextView);
		usernameView.setText(prefsBean.getUserName());
	}

	protected void showAnonymousLogin() {
		TextView anonymousView = (TextView)findViewById(R.id.anonymousLoginTextView);
		anonymousView.setText(getString(R.string.isAnonymous, prefsBean.isAnonymousLogin()));
	}

	@SuppressLint("SetTextI18n")
	protected void showKeyFingerprints() {
		((TextView)findViewById(R.id.keyFingerprintMd5Label))
				.setText("MD5");
		((TextView)findViewById(R.id.keyFingerprintSha1Label))
				.setText("SHA1");
		((TextView)findViewById(R.id.keyFingerprintSha256Label))
				.setText("SHA256");

		((TextView)findViewById(R.id.keyFingerprintMd5TextView))
			.setText(fingerprintMd5);
		((TextView)findViewById(R.id.keyFingerprintSha1TextView))
			.setText(fingerprintSha1);
		((TextView)findViewById(R.id.keyFingerprintSha256TextView))
			.setText(fingerprintSha256);

		// create onRefreshListener
		View refreshButton = findViewById(R.id.keyFingerprintsLabel);
		refreshButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				GenKeysAskDialogFragment askDiag = new GenKeysAskDialogFragment();
				askDiag.show(getFragmentManager(), DIALOG_TAG);
			}
		});
	}

	protected void genKeysAndShowProgressDiag(boolean startServerOnFinish) {
		// critical: do not pass getApplicationContext() to dialog
		final ProgressDialog progressDiag = new ProgressDialog(this);
		progressDiag.setCancelable(false);
		progressDiag.setMessage(getText(R.string.generatingKeysMessage));

		AsyncTask<Void, Void, Void> task = new GenKeysAsyncTask(
			progressDiag,
			startServerOnFinish);
		task.execute();

		progressDiag.show();
	}

	public static class GenKeysAskDialogFragment extends DialogFragment {
		public static final String KEY_START_SERVER = "START_SERVER";

		private boolean startServerOnFinish;

		@Override
		public void setArguments(Bundle args) {
			super.setArguments(args);
			startServerOnFinish = args.getBoolean(KEY_START_SERVER);
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage(R.string.generateKeysMessage);
			builder.setPositiveButton(R.string.generate, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					PrimitiveFtpdActivity activity = (PrimitiveFtpdActivity) getActivity();
					activity.genKeysAndShowProgressDiag(startServerOnFinish);
				}
			});
			builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					// nothing
				}
			});
			return builder.create();
		}
	}

	class GenKeysAsyncTask extends AsyncTask<Void, Void, Void> {
		private final ProgressDialog progressDiag;
		private final boolean startServerOnFinish;

		public GenKeysAsyncTask(
			ProgressDialog progressDiag,
			boolean startServerOnFinish)
		{
			this.progressDiag = progressDiag;
			this.startServerOnFinish = startServerOnFinish;
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				FileOutputStream publickeyFos = buildPublickeyOutStream();
				FileOutputStream privatekeyFos = buildPrivatekeyOutStream();
				try {
					new KeyGenerator().generate(publickeyFos, privatekeyFos);
				} finally {
					publickeyFos.close();
					privatekeyFos.close();
				}
			} catch (Exception e) {
				logger.error("could not generate keys", e);
			}
			return null;
		}
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			calcPubkeyFingerprints();
			progressDiag.dismiss();
			showKeyFingerprints();

			if (startServerOnFinish) {
				// icon members should be set at this time
				handleStart();
			}
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(ServerStateChangedEvent event) {
		long currentTime = System.currentTimeMillis();
		long offset = currentTime - timestampOfLastEvent;
		if (offset > 300) {
			logger.debug("handling event, offset: {} ms", Long.valueOf(offset));
			timestampOfLastEvent = currentTime;
			displayServersState();
		} else {
			logger.debug("ignoring event, offset: {} ms", Long.valueOf(offset));
		}
	}

	/**
	 * Displays UI-elements showing if servers are running. That includes
	 * Actionbar Icon and Ports-Table. When Activity is shown the first time
	 * this is triggered by {@link #onCreateOptionsMenu(Menu)}, when user comes back from
	 * preferences, this is triggered by {@link #onResume()}. It may be invoked by
	 * {@link GenKeysAsyncTask}.
	 */
	protected void displayServersState() {
		logger.debug("displayServersState()");

		checkServicesRunning();
		Boolean running = null;
		if (serversRunning != null) {
			running = Boolean.valueOf(serversRunning.atLeastOneRunning());
		}

		// should be triggered by onCreateOptionsMenu() to avoid icon flicker
		// when invoked via notification
		updateButtonStates(running);

		// by checking ButtonStates we get info which services are running
		// that is displayed in portsTable
		// as there are no icons when this runs first time,
		// we don't get serversRunning, yet
		if (serversRunning != null) {
			showPortsAndServerState();
		}
	}

	protected void checkServicesRunning() {
		logger.debug("checkServicesRunning()");
		this.serversRunning = ServicesStartStopUtil.checkServicesRunning(this);
	}

	/**
	 * Updates enabled state of start/stop buttons.
	 */
	protected void updateButtonStates(Boolean running) {
		if (startIcon == null || stopIcon == null) {
			logger.debug("updateButtonStates(), no icons");
			return;
		}

		logger.debug("updateButtonStates()");

		boolean atLeastOneRunning;
		if (running == null) {
			checkServicesRunning();
			atLeastOneRunning = serversRunning.atLeastOneRunning();
		} else {
			atLeastOneRunning = running.booleanValue();
		}

		startIcon.setVisible(!atLeastOneRunning);
		stopIcon.setVisible(atLeastOneRunning);

		// remove status bar notification if server not running
		if (!atLeastOneRunning) {
			NotificationUtil.removeStatusbarNotification(this);
		}
	}

	protected MenuItem startIcon;
	protected MenuItem stopIcon;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		logger.debug("onCreateOptionsMenu()");

		getMenuInflater().inflate(R.menu.pftpd, menu);

		startIcon = menu.findItem(R.id.menu_start);
		stopIcon = menu.findItem(R.id.menu_stop);

		// at least required on app start
		displayServersState();

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_start:
			handleStart();
			break;
		case R.id.menu_stop:
			handleStop();
			break;
		case R.id.menu_prefs:
			handlePrefs();
			break;
		case R.id.menu_about:
			handleAbout();
			break;
		case R.id.menu_exit:
			finish();
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	protected void handleStart() {
		ServicesStartStopUtil.startServers(this, prefsBean, this);
	}

	public boolean isKeyPresent() {
		return keyPresent;
	}

	public void showGenKeyDialog() {
		GenKeysAskDialogFragment askDiag = new GenKeysAskDialogFragment();
		Bundle diagArgs = new Bundle();
		diagArgs.putBoolean(GenKeysAskDialogFragment.KEY_START_SERVER, true);
		askDiag.setArguments(diagArgs);
		askDiag.show(getFragmentManager(), DIALOG_TAG);
	}

	protected void handleStop() {
		ServicesStartStopUtil.stopServers(this);
	}

	protected void handlePrefs() {
		Class<?> prefsActivityClass = theme == Theme.DARK
			? FtpPrefsActivityThemeDark.class
			: FtpPrefsActivityThemeLight.class;
		Intent intent = new Intent(this, prefsActivityClass);
		startActivity(intent);
	}

	protected void handleAbout() {
		Intent intent = new Intent(this, AboutActivity.class);
		startActivity(intent);
	}

	/**
	 * Loads and parses preferences.
	 *
	 * @return {@link PrefsBean}
	 */
	protected void loadPrefs() {
		logger.debug("loadPrefs()");

		SharedPreferences prefs = LoadPrefsUtil.getPrefs(getBaseContext());
		this.prefsBean = LoadPrefsUtil.loadPrefs(logger, prefs);

		handlePrefsChanged();
		handleLoggingPref(prefs);
	}

	protected void handlePrefsChanged() {
		if (prefsChanged) {
			prefsChanged = false;
			if (serversRunning != null && serversRunning.atLeastOneRunning()) {
				Toast.makeText(
					getApplicationContext(),
					R.string.restartServer,
					Toast.LENGTH_LONG).show();
			}
		}
	}

	protected void handleLoggingPref(SharedPreferences prefs) {
		String loggingStr = prefs.getString(
			LoadPrefsUtil.PREF_KEY_LOGGING,
			Logging.NONE.xmlValue());
		Logging logging = Logging.byXmlVal(loggingStr);
		// one could argue if this makes sense :)
		logger.debug("got 'logging': {}", logging);
		PrimFtpdLoggerBinder.setLoggingPref(logging);
		// re-create own log, don't care about other classes
		this.logger = LoggerFactory.getLogger(getClass());
	}
}
