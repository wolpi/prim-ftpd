package org.primftpd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Enumeration;
import java.util.List;

import org.apache.ftpserver.util.IoUtils;
import org.primftpd.log.PrimFtpdLoggerBinder;
import org.primftpd.prefs.FtpPrefsActivityThemeDark;
import org.primftpd.prefs.FtpPrefsActivityThemeLight;
import org.primftpd.prefs.LoadPrefsUtil;
import org.primftpd.prefs.Logging;
import org.primftpd.prefs.ServerToStart;
import org.primftpd.prefs.Theme;
import org.primftpd.services.FtpServerService;
import org.primftpd.services.SshServerService;
import org.primftpd.util.KeyGenerator;
import org.primftpd.util.KeyInfoProvider;
import org.primftpd.util.NotificationUtil;
import org.primftpd.util.PrngFixes;
import org.primftpd.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
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
import android.os.PersistableBundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity to display network info and to start FTP service.
 */
public class PrimitiveFtpdActivity extends Activity {

	public static class ServersRunningBean {
		boolean ftp = false;
		boolean ssh = false;

		boolean atLeastOneRunning() {
			return ftp || ssh;
		}
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
	        logger.debug(
	        	"BroadcastReceiver.onReceive(), action: '{}'",
	        	intent.getAction());
			if (FtpServerService.BROADCAST_ACTION_COULD_NOT_START.equals(intent.getAction())) {
				updateButtonStates();
			}
		}
	};

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

	public static final String EXTRA_PREFS_BEAN = "prefs.bean";

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
    		getText(R.string.ipAddrLabel) + " (" +
    		getText(R.string.ifacesLabel) + ")");

    	// create ports label
    	((TextView)findViewById(R.id.portsLabel)).setText(
    		getText(R.string.protocolLabel) + " / " +
    		getText(R.string.portLabel) + " / " +
    		getText(R.string.state));

    	// allow to finish activity
        getActionBar().setDisplayHomeAsUpEnabled(true);

		// handle starting of server at boot
		Bundle intentExtras = getIntent().getExtras();
		if(intentExtras != null
				&& intentExtras.getBoolean(BootUpReceiver.EXTRAS_KEY)) {
			// start server
			boolean startOnBoot = LoadPrefsUtil.startOnBoot(prefs);
			if (startOnBoot) {
				logger.debug("starting server on boot");
				handleStart();
			}

			// stop activity
			moveTaskToBack(true);
			finish();
		}
    }

    @Override
    protected void onDestroy()
    {
    	super.onDestroy();

    	// prefs change
    	SharedPreferences prefs = LoadPrefsUtil.getPrefs(getBaseContext());
		prefs.unregisterOnSharedPreferenceChangeListener(prefsChangeListener);
    }

	@Override
	protected void onStart() {
		super.onStart();

		logger.debug("onStart()");

		loadPrefs();
		showUsername();
	}

	@Override
    protected void onResume() {
    	super.onResume();

    	logger.debug("onResume()");

    	// broadcast receiver to update buttons
        IntentFilter filter = new IntentFilter();
        filter.addAction(FtpServerService.BROADCAST_ACTION_COULD_NOT_START);
        this.registerReceiver(this.receiver, filter);


    	// register listener to reprint interfaces table when network connections change
    	filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
    	registerReceiver(this.networkStateReceiver, filter);

    	displayServersState();
	}

    @Override
    protected void onPause() {
    	super.onPause();

    	logger.debug("onPause()");

    	// unregister broadcast receivers
        this.unregisterReceiver(this.receiver);
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

    class GenKeysAskDialogFragment extends DialogFragment {
    	private final boolean startServerOnFinish;

    	public GenKeysAskDialogFragment() {
    		this(false);
    	}
    	public GenKeysAskDialogFragment(boolean startServerOnFinish) {
    		this.startServerOnFinish = startServerOnFinish;
    	}

    	@Override
    	public Dialog onCreateDialog(Bundle savedInstanceState) {
    		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.generateKeysMessage);
            builder.setPositiveButton(R.string.generate, new DialogInterface.OnClickListener() {
            	@Override
				public void onClick(DialogInterface dialog, int id) {
            		genKeysAndShowProgressDiag(startServerOnFinish);
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
				// must update server state after start
				displayServersState();
			}
		}
    }

    /**
	 * Displays UI-elements showing if servers are running. That includes
	 * Actionbar Icon and Ports-Table. When Activity is shown the first time
	 * this is triggered by {@link #onCreateOptionsMenu()}, when user comes back from
	 * preferences, this is triggered by {@link #onResume()}. It may be invoked by
	 * {@link GenKeysAsyncTask}.
	 */
    protected void displayServersState() {
    	logger.debug("displayServersState()");

    	// should be triggered by onCreateOptionsMenu() to avoid icon flicker
    	// when invoked via notification
		updateButtonStates();

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
    	ServersRunningBean serversRunning = new ServersRunningBean();
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		List<RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
		String ftpServiceClassName = FtpServerService.class.getName();
		String sshServiceClassName = SshServerService.class.getName();
		for (RunningServiceInfo service : runningServices) {
			String currentClassName = service.service.getClassName();
			if (ftpServiceClassName.equals(currentClassName)) {
				serversRunning.ftp = true;
			}
			if (sshServiceClassName.equals(currentClassName)) {
				serversRunning.ssh = true;
			}
			if (serversRunning.ftp && serversRunning.ssh) {
				break;
			}
		}
		this.serversRunning = serversRunning;
	}

    /**
     * Updates enabled state of start/stop buttons.
     */
    protected void updateButtonStates() {
    	if (startIcon == null || stopIcon == null) {
            logger.debug("updateButtonStates(), no icons");
    		return;
    	}

        logger.debug("updateButtonStates()");

    	checkServicesRunning();
    	boolean atLeastOneRunning = serversRunning.atLeastOneRunning();

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

		displayServersState();

		return true;
	}

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_start:
			handleStart(startIcon, stopIcon);
			break;
		case R.id.menu_stop:
			handleStop(startIcon, stopIcon);
			break;
		case R.id.menu_prefs:
			handlePrefs();
			break;
        case android.R.id.home:
            finish();
            break;
		}

		displayServersState();

		return super.onOptionsItemSelected(item);
	}

    /**
	 * if this version of the method is called, it is necessary to call
	 * {@link #displayServersState()} afterwards.
	 */
    protected void handleStart() {
    	handleStart(null, null);
    }
    protected void handleStart(MenuItem startIcon, MenuItem stopIcon) {
		if (StringUtils.isBlank(prefsBean.getPassword()))
		{
			Toast.makeText(
				getApplicationContext(),
				R.string.haveToSetPassword,
				Toast.LENGTH_LONG).show();

		} else {
			boolean continueServerStart = true;
			if (prefsBean.getServerToStart().startSftp()) {
				if (!keyPresent) {
					// cannot start sftp server when key is not present
					// ask user to generate it
	    			GenKeysAskDialogFragment askDiag = new GenKeysAskDialogFragment(true);
	    			askDiag.show(getFragmentManager(), DIALOG_TAG);
	    			continueServerStart = false;
				}
				if (keyPresent) {
					startService(createSshServiceIntent());
				}
			}
			if (continueServerStart) {
				if (prefsBean.getServerToStart().startFtp()) {
					startService(createFtpServiceIntent());
				}
				if (startIcon != null && stopIcon != null) {
			    	startIcon.setVisible(false);
			    	stopIcon.setVisible(true);
				}
			}
		}
    }

    protected void handleStop(MenuItem startIcon, MenuItem stopIcon) {
    	stopService(createFtpServiceIntent());
    	stopService(createSshServiceIntent());
    	startIcon.setVisible(true);
    	stopIcon.setVisible(false);
    }

    protected void handlePrefs() {
    	Class<?> prefsActivityClass = theme == Theme.DARK
    		? FtpPrefsActivityThemeDark.class
    		: FtpPrefsActivityThemeLight.class;
    	Intent intent = new Intent(this, prefsActivityClass);
		startActivity(intent);
    }

    /**
     * @return Intent to start/stop {@link FtpServerService}.
     */
    protected Intent createFtpServiceIntent() {
    	Intent intent = new Intent(this, FtpServerService.class);
    	putPrefsInIntent(intent);
    	return intent;
    }

    /**
     * @return Intent to start/stop {@link SshServerService}.
     */
    protected Intent createSshServiceIntent() {
    	Intent intent = new Intent(this, SshServerService.class);
    	putPrefsInIntent(intent);
    	return intent;
    }

    protected void putPrefsInIntent(Intent intent) {
    	intent.putExtra(EXTRA_PREFS_BEAN, prefsBean);
    }

	/**
	 * Loads and parses preferences.
	 *
	 * @return {@link PrefsBean}
	 */
	protected void loadPrefs() {
		logger.debug("loadPrefs()");

		SharedPreferences prefs = LoadPrefsUtil.getPrefs(getBaseContext());

		String userName = LoadPrefsUtil.userName(prefs);
		logger.debug("got userName: {}", userName);

		String password = LoadPrefsUtil.password(prefs);
		logger.debug("got password: {}", password);

		File startDir = LoadPrefsUtil.startDir(prefs);
		logger.debug("got startDir: {}", startDir);

		boolean announce = LoadPrefsUtil.announce(prefs);
		logger.debug("got announce: {}", Boolean.valueOf(announce));

		boolean wakelock = LoadPrefsUtil.wakelock(prefs);
		logger.debug("got wakelock: {}", Boolean.valueOf(wakelock));

		ServerToStart serverToStart = LoadPrefsUtil.serverToStart(prefs);
		logger.debug("got 'which server': {}", serverToStart);

		int port = LoadPrefsUtil.loadPortInsecure(logger, prefs);
		logger.debug("got 'port': {}", Integer.valueOf(port));

		int securePort = LoadPrefsUtil.loadPortSecure(logger,prefs);
		logger.debug("got 'secure port': {}", Integer.valueOf(securePort));

		// create prefsBean
		prefsBean = new PrefsBean(
			userName,
			password,
			port,
			securePort,
			startDir,
			announce,
			wakelock,
			serverToStart);

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
