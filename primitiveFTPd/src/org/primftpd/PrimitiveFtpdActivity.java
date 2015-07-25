package org.primftpd;

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
import org.primftpd.prefs.FtpPrefsActivity;
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
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
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
	        createIfaceTable();
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

	public static final int KEYS_REFRESH_ICON_ID = 1;

	public static final String DIALOG_TAG = "dialogs";

	protected Logger logger = LoggerFactory.getLogger(getClass());

	private PrefsBean prefsBean;
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
		Theme theme = LoadPrefsUtil.theme(prefs);
		setTheme(theme.resourceId());
        setContentView(R.layout.main);

    	// calc keys fingerprints
        calcPubkeyFingerprints();
    	createFingerprintTable();
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
		createUsernameTable();
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
    protected void createIfaceTable() {
    	TableLayout table = (TableLayout)findViewById(R.id.ifacesTable);

        // clear old entries
    	table.removeAllViews();

    	// create header line
    	createTableRow(
    		table,
    		getText(R.string.ifacesLabel),
    		getText(R.string.ipAddrLabel));

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

                    createTableRow(table, ifaceDispName, hostAddr);
                }
            }
        } catch (SocketException e) {
        	logger.info("exception while iterating network interfaces", e);

        	String msg = getText(R.string.ifacesError) + e.getLocalizedMessage();
        	Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Creates a 2 column row in a table.
     *
     * @param table Table to add row to.
     * @param label Text for left column.
     * @param value Text for right column.
     */
    protected void createTableRow(
		TableLayout table,
		CharSequence label,
		CharSequence value)
    {
    	createTableRow(table, label, value, 0, null, false, null);
    }

    /**
	 * Creates a 3 column row. To be used for header line of ports table.
	 * 3rd col is left empty but is required for data-rows.
	 *
     * @param table Table to add row to.
     * @param label Text for left column.
     * @param value Text for middle column.
	 * @param serverRunning Flag indicating which icon to show.
	 */
    protected void createTableRowPortHeader(
		TableLayout table,
		CharSequence label,
		CharSequence value)
    {
		createTableRow(
			table,
			label,
			value,
			0,
			null,
			false,
			getText(R.string.state));
    }

    /**
	 * Creates a 3 column row. To be used for ports, 3rd col shows icon for
	 * server state.
	 *
     * @param table Table to add row to.
     * @param label Text for left column.
     * @param value Text for middle column.
	 * @param serverRunning Flag indicating which icon to show.
	 */
    protected void createTableRowPort(
		TableLayout table,
		CharSequence label,
		CharSequence value,
		boolean serverRunning)
    {
    	CharSequence serverState = getText(serverRunning
    		? R.string.serverStarted
    		: R.string.serverStopped);
    	createTableRow(table, label, value, 0, null, false, serverState);
    }

    /**
     * Creates a 2 column row. Instead of a value string it uses an image id.
     *
     * @param table Table to add row to.
     * @param label Text for left column.
     * @param value Resource id of image to show in right column.
     */
    protected void createTableRowKeyFingerprintHeader(
		TableLayout table,
		CharSequence label,
		int imageId)
    {
    	createTableRow(table, label, null, imageId, null, false, null);
    }

    protected void createTableRowKeyFingerprint(
		TableLayout table,
		CharSequence label,
		CharSequence value)
    {
    	Integer labelMaxWidth = Integer.valueOf(150);
    	createTableRow(table, label, value, 0, labelMaxWidth, true, null);
    }

    protected void createTableRow(
		TableLayout table,
		CharSequence label,
		CharSequence value,
		int imageId,
		Integer labelMaxWidth,
		boolean monospace,
		CharSequence serverState)
    {
    	TableRow row = new TableRow(table.getContext());
    	table.addView(row);
    	row.setPadding(1, 1, 1, 5);

    	TextView labelView = new TextView(row.getContext());
    	row.addView(labelView);
    	labelView.setPadding(0, 0, 20, 0);
    	if (labelMaxWidth != null) {
    		labelView.setMaxWidth(labelMaxWidth.intValue());
    	}
    	labelView.setText(label);

    	View valueView = null;
    	if (value != null) {
    		TextView valueTextView = new TextView(row.getContext());
    		valueTextView.setText(value);
    		valueView = valueTextView;
    		if (monospace) {
    			valueTextView.setTypeface(Typeface.MONOSPACE);
    		}
    	} else {
    		// it is a little hacky to always create refresh icon
    		// but this is the only case we need an imageId here
    		ImageView valueImgView = new ImageView(row.getContext());
    		valueImgView.setImageResource(imageId);
    		valueImgView.setId(KEYS_REFRESH_ICON_ID);
    		valueView = valueImgView;
    	}
    	row.addView(valueView);

    	LayoutParams params = new LayoutParams();
    	params.height = LayoutParams.WRAP_CONTENT;
    	params.gravity = Gravity.LEFT;
    	valueView.setLayoutParams(params);

    	if (serverState != null) {
    		TextView serverStateView = new TextView(row.getContext());
        	row.addView(serverStateView);
    		serverStateView.setText(serverState);
    		serverStateView.setGravity(Gravity.LEFT);
    		serverStateView.setPadding(50, 0, 0, 0);
    	}
    }

    /**
     * Creates UI table showing ports.
     */
    protected void createPortsTable() {
    	TableLayout table = (TableLayout)findViewById(R.id.portsTable);

        // clear old entries
    	table.removeAllViews();

    	// create header line
    	createTableRowPortHeader(
    		table,
    		getText(R.string.protocolLabel),
    		getText(R.string.portLabel));

    	createTableRowPort(
    		table,
    		"ftp",
    		prefsBean.getPortStr(),
    		serversRunning.ftp);

    	createTableRowPort(
    		table,
    		"sftp",
    		prefsBean.getSecurePortStr(),
    		serversRunning.ssh);
    }

    protected void createUsernameTable() {
    	TableLayout table = (TableLayout)findViewById(R.id.usernameTable);

        // clear old entries
    	table.removeAllViews();

    	// create header line
    	createTableRow(
    		table,
    		getText(R.string.prefTitleUser),
    		prefsBean.getUserName());
    }

    protected void createFingerprintTable() {
    	TableLayout table = (TableLayout)findViewById(R.id.keysInfoTable);
        // clear old entries
    	table.removeAllViews();

    	createTableRowKeyFingerprintHeader(
    		table,
    		getText(R.string.fingerprintsLabel),
    		R.drawable.refresh);

    	table = (TableLayout)findViewById(R.id.fingerprintsTable);
        // clear old entries
    	table.removeAllViews();

    	createTableRowKeyFingerprint(
    		table,
    		"MD5",
    		fingerprintMd5);
    	createTableRowKeyFingerprint(
    		table,
    		"SHA1",
    		fingerprintSha1);
    	createTableRowKeyFingerprint(
    		table,
    		"SHA256",
    		fingerprintSha256);

    	// create onRefreshListener
    	View refreshButton = findViewById(KEYS_REFRESH_ICON_ID);
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
			createFingerprintTable();

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
			createPortsTable();
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
    	Intent intent = new Intent(this, FtpPrefsActivity.class);
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

		String userName = prefs.getString(
			LoadPrefsUtil.PREF_KEY_USER,
			"user");

		// load password
		String password = prefs.getString(
			LoadPrefsUtil.PREF_KEY_PASSWORD,
			null);
		logger.debug("got password: {}", password);

		// load announcement setting
		// default to false as it may cause crashes
		boolean announce = prefs.getBoolean(
			LoadPrefsUtil.PREF_KEY_ANNOUNCE,
			Boolean.FALSE);
		logger.debug("got announce: {}", Boolean.valueOf(announce));

		// load wakelock setting
		boolean wakelock = prefs.getBoolean(
			LoadPrefsUtil.PREF_KEY_WAKELOCK,
			Boolean.TRUE);
		logger.debug("got wakelock: {}", Boolean.valueOf(wakelock));

		// load list pref: which server to start
		String whichServerStr = prefs.getString(
			LoadPrefsUtil.PREF_KEY_WHICH_SERVER,
			ServerToStart.ALL.xmlValue());
		ServerToStart serverToStart = ServerToStart.byXmlVal(whichServerStr);
		logger.debug("got 'which server': {}", serverToStart);

		// load port
		int port = LoadPrefsUtil.loadAndValidatePort(
			getBaseContext(),
			logger,
			prefs,
			LoadPrefsUtil.PREF_KEY_PORT,
			LoadPrefsUtil.PORT_DEFAULT_VAL,
			LoadPrefsUtil.PORT_DEFAULT_VAL_STR);

		// load secure port
		int securePort = LoadPrefsUtil.loadAndValidatePort(
			getBaseContext(),
			logger,
			prefs,
			LoadPrefsUtil.PREF_KEY_SECURE_PORT,
			LoadPrefsUtil.SECURE_PORT_DEFAULT_VAL,
			LoadPrefsUtil.SECURE_PORT_DEFAULT_VAL_STR);

		// check if ports are equal
		if (port == securePort) {
			Toast.makeText(
				getBaseContext(),
				R.string.portsEqual,
				Toast.LENGTH_LONG).show();
			port = LoadPrefsUtil.PORT_DEFAULT_VAL;
			securePort = LoadPrefsUtil.SECURE_PORT_DEFAULT_VAL;

			// reset in persistent prefs
			Editor prefsEditor = prefs.edit();
			prefsEditor.putString(
				LoadPrefsUtil.PREF_KEY_PORT,
				LoadPrefsUtil.PORT_DEFAULT_VAL_STR);
			prefsEditor.putString(
				LoadPrefsUtil.PREF_KEY_SECURE_PORT,
				LoadPrefsUtil.SECURE_PORT_DEFAULT_VAL_STR);
			prefsEditor.commit();
		}

		// create prefsBean
		prefsBean = new PrefsBean(
			userName,
			password,
			port,
			securePort,
			announce,
			wakelock,
			serverToStart);

		if (prefsChanged) {
			prefsChanged = false;
			if (serversRunning != null && serversRunning.atLeastOneRunning()) {
				Toast.makeText(
					getApplicationContext(),
					R.string.restartServer,
					Toast.LENGTH_LONG).show();
			}
		}

		// load list pref: logging
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
