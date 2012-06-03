package org.primftpd;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.KeyStore;
import java.util.Enumeration;
import java.util.List;

import org.primftpd.util.KeyStoreUtil;
import org.primftpd.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity to display network info and to start FTP service.
 */
public class PrimitiveFtpdActivity extends Activity {

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
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

	protected static final String SERVICE_CLASS_NAME = "org.primftpd.FtpServerService";
	public static final String EXTRA_USERNAME = "username";
	public static final String EXTRA_PASSWORD = "password";
	public static final String EXTRA_PORT = "port";
	public static final String EXTRA_SSL_PORT = "ssl.port";

	protected Logger logger = LoggerFactory.getLogger(getClass());

	private PrefsBean prefsBean;
	private String md5Fingerprint;
	private String sha1Fingerprint;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	// basic setup
        super.onCreate(savedInstanceState);

        logger.debug("onCreate()");

        setContentView(R.layout.main);

    	// button handlers
    	updateButtonStates();
    	final Button startButton = (Button)findViewById(R.id.startButton);
    	final Button stopButton = (Button)findViewById(R.id.stopButton);
    	startButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (StringUtils.isBlank(prefsBean.getPassword()))
				{
					Toast.makeText(
							getApplicationContext(),
							R.string.haveToSetPassword,
							Toast.LENGTH_LONG).show();

				} else {
					Intent intent = createFtpServiceIntent();
			    	startService(intent);
			    	startButton.setEnabled(false);
			    	stopButton.setEnabled(true);
				}
			}
		});
    	stopButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
		    	Intent intent = createFtpServiceIntent();
		    	stopService(intent);
		    	startButton.setEnabled(true);
		    	stopButton.setEnabled(false);
			}
		});

    	final Button prefsButton = (Button)findViewById(R.id.prefsButton);
    	prefsButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = createPreferencesIntent();
				startActivity(intent);
			}
		});

    	// calc certificate fingerprints
    	KeyStore keyStore = KeyStoreUtil.loadKeyStore(getResources());
    	md5Fingerprint = KeyStoreUtil.calcKeyFingerprint(keyStore, "MD5");
    	sha1Fingerprint = KeyStoreUtil.calcKeyFingerprint(keyStore, "SHA-1");
    	createFingerprintTable();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    	if (hasFocus) {
        	logger.debug("onWindowFocusChanged(true)");
        	updateButtonStates();
    	}
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
    }

    @Override
    protected void onPause() {
    	super.onPause();

    	logger.debug("onPause()");

    	// unregister broadcast receivers
        this.unregisterReceiver(this.receiver);
        this.unregisterReceiver(this.networkStateReceiver);
    }

    /**
     * @return Intent to start/stop {@link FtpServerService}.
     */
    protected Intent createFtpServiceIntent() {
    	Intent intent = new Intent(this, FtpServerService.class);
    	intent.putExtra(EXTRA_USERNAME, prefsBean.getUserName());
    	intent.putExtra(EXTRA_PASSWORD, prefsBean.getPassword());
    	intent.putExtra(EXTRA_PORT, prefsBean.getPort());
    	intent.putExtra(EXTRA_SSL_PORT, prefsBean.getSslPort());
    	return intent;
    }

    /**
     * @return Intent to open preferences page.
     */
    protected Intent createPreferencesIntent() {
    	Intent intent = new Intent(this, FtpPrefsActivity.class);
    	return intent;
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
    	TableRow row = new TableRow(table.getContext());
    	table.addView(row);
    	row.setPadding(1, 1, 1, 5);

    	TextView labelView = new TextView(row.getContext());
    	row.addView(labelView);
    	labelView.setPadding(0, 0, 20, 0);
    	labelView.setText(label);

    	TextView valueView = new TextView(row.getContext());
    	row.addView(valueView);

    	LayoutParams params = new LayoutParams();
    	params.height = LayoutParams.WRAP_CONTENT;

    	valueView.setLayoutParams(params);
    	valueView.setGravity(Gravity.LEFT);
    	valueView.setText(value);
    }

    /**
     * Creates UI table showing ports.
     */
    protected void createPortsTable() {
    	TableLayout table = (TableLayout)findViewById(R.id.portsTable);

        // clear old entries
    	table.removeAllViews();

    	// create header line
    	createTableRow(
    		table,
    		getText(R.string.protocolLabel),
    		getText(R.string.portLabel));

    	createTableRow(
    		table,
    		"ftp",
    		prefsBean.getPortStr());

    	createTableRow(
    		table,
    		"ftps",
    		prefsBean.getSslPortStr());
    }

    protected void createFingerprintTable() {
    	TextView fingerprintsLabel = (TextView) findViewById(R.id.fingerprintsText);
    	fingerprintsLabel.setText(getText(R.string.fingerprintsLabel));

    	// note: HTML required for line breaks
    	TableLayout table = (TableLayout)findViewById(R.id.fingerprintsTable);
    	createTableRow(
    		table,
    		"MD5",
    		Html.fromHtml(md5Fingerprint));
    	createTableRow(
    		table,
    		"SHA1",
    		Html.fromHtml(sha1Fingerprint));
    }

    /**
     * @return True if {@link FtpServerService} is running.
     */
    protected boolean checkServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		List<RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
		for (RunningServiceInfo service : runningServices) {
			if (SERVICE_CLASS_NAME.equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

    /**
     * Updates enabled state of start/stop buttons.
     */
    protected void updateButtonStates() {
    	boolean serviceRunning = checkServiceRunning();
    	final Button startButton = (Button)findViewById(R.id.startButton);
    	final Button stopButton = (Button)findViewById(R.id.stopButton);
    	startButton.setEnabled(!serviceRunning);
    	stopButton.setEnabled(serviceRunning);

    	// remove status bar notification if server not running
    	if (!serviceRunning) {
    		NotificationUtil.removeStatusbarNotification(this);
    	}
    }

    private static final int MENU_ITEM_ID_PREFS = 0;
    private static final int MENU_ITEM_ID_EXIT = 1;

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(
			Menu.NONE,
			MENU_ITEM_ID_PREFS,
			0,
			getText(R.string.prefs));
		menu.add(
			Menu.NONE,
			MENU_ITEM_ID_EXIT,
			0,
			getText(R.string.exit));
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case MENU_ITEM_ID_PREFS:
				startActivity(new Intent(this, FtpPrefsActivity.class));
				return true;
			case MENU_ITEM_ID_EXIT:
				finish();
				return true;
		}
		return false;
	}

	@Override
	protected void onStart() {
		super.onStart();

		logger.debug("onStart()");

		loadPrefs();

		createPortsTable();
	}

	private static final int PORT_DEFAULT_VAL = 1233;
	private static final String PORT_DEFAULT_VAL_STR = String.valueOf(PORT_DEFAULT_VAL);
	private static final int SSL_PORT_DEFAULT_VAL = 1234;
	private static final String SSL_PORT_DEFAULT_VAL_STR = String.valueOf(SSL_PORT_DEFAULT_VAL);

	/**
	 * @return Android {@link SharedPreferences} object.
	 */
	protected SharedPreferences getPrefs() {
		return PreferenceManager.getDefaultSharedPreferences(getBaseContext());
	}


	public static final String PREF_KEY_USER = "userPref";
	public static final String PREF_KEY_PASSWORD = "passwordPref";
	public static final String PREF_KEY_PORT = "portPref";
	public static final String PREF_KEY_SSL_PORT = "sslPortPref";

	/**
	 * Loads preferences and stores in member {@link #prefsBean}.
	 */
	protected void loadPrefs() {
		logger.debug("loadPrefs()");

		SharedPreferences prefs = getPrefs();

		String userName = prefs.getString(PREF_KEY_USER, "user");

		// load password
		String password = prefs.getString(PREF_KEY_PASSWORD, null);
		logger.debug("got password: {}", password);

		// load port
		int port = loadAndValidatePort(
			prefs,
			PREF_KEY_PORT,
			PORT_DEFAULT_VAL,
			PORT_DEFAULT_VAL_STR);

		// load SSL port
		int sslPort = loadAndValidatePort(
			prefs,
			PREF_KEY_SSL_PORT,
			SSL_PORT_DEFAULT_VAL,
			SSL_PORT_DEFAULT_VAL_STR);

		// check if ports are equal
		if (port == sslPort) {
			Toast.makeText(
				getApplicationContext(),
				R.string.portsEqual,
				Toast.LENGTH_LONG).show();
			port = PORT_DEFAULT_VAL;
			sslPort = SSL_PORT_DEFAULT_VAL;

			// reset in persistent prefs
			Editor prefsEditor = prefs.edit();
			prefsEditor.putString(
				PREF_KEY_PORT,
				PORT_DEFAULT_VAL_STR);
			prefsEditor.putString(
				PREF_KEY_SSL_PORT,
				SSL_PORT_DEFAULT_VAL_STR);
			prefsEditor.commit();
		}

		// create prefsBean
		PrefsBean oldPrefs = prefsBean;
		prefsBean = new PrefsBean(userName, password, port, sslPort);

		if (oldPrefs != null) {
			if (!oldPrefs.equals(prefsBean) && checkServiceRunning()) {
				Toast.makeText(
					getApplicationContext(),
					R.string.restartServer,
					Toast.LENGTH_LONG).show();
			}
		}
	}

	protected int loadAndValidatePort(
			SharedPreferences prefs,
			String prefsKey,
			int defaultVal,
			String defaultValStr)
	{
		// load port
		int port = defaultVal;
		String portStr = prefs.getString(
			prefsKey,
			defaultValStr);
		try {
			port = Integer.valueOf(portStr);
		} catch (NumberFormatException e) {
			logger.info("NumberFormatException while parsing port key '{}'", prefsKey);
		}

		// validate port
		// I would prefer to do this in a prefsChangeListener, but that seems not to work
		if (!validatePort(port)) {
			Toast.makeText(
				getApplicationContext(),
				R.string.portInvalid,
				Toast.LENGTH_LONG).show();
			port = defaultVal;
			Editor prefsEditor = prefs.edit();
			prefsEditor.putString(
				prefsKey,
				defaultValStr);
			prefsEditor.commit();
		}

		return port;
	}

	/**
	 * @param port
	 * @return True if port is valid, false if invalid.
	 */
	protected boolean validatePort(int port) {
		if (port > 1024 && port < 64000) {
			return true;
		}
		return false;
	}
}
