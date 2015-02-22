package org.primftpd;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.PrivateKey;
import java.util.Enumeration;
import java.util.List;

import org.apache.ftpserver.util.IoUtils;
import org.primftpd.prefs.FtpPrefsActivity;
import org.primftpd.services.FtpServerService;
import org.primftpd.services.SshServerService;
import org.primftpd.util.KeyGenerator;
import org.primftpd.util.KeyInfoProvider;
import org.primftpd.util.NotificationUtil;
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
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
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

	public static final String EXTRA_PREFS_BEAN = "prefs.bean";

	public static final String PUBLICKEY_FILENAME = "pftpd-pub.pk8";
	public static final String PRIVATEKEY_FILENAME = "pftpd-priv.pk8";

	public static final int KEYS_REFRESH_ICON_ID = 1;

	public static final String DIALOG_TAG = "dialogs";

	protected Logger logger = LoggerFactory.getLogger(getClass());

	private PrefsBean prefsBean;
	private String md5Fingerprint = " - ";
	private String sha1Fingerprint = " - ";
	private String sha256Fingerprint = " - ";

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	// basic setup
        super.onCreate(savedInstanceState);

        logger.debug("onCreate()");

        setContentView(R.layout.main);

    	// button handlers
        // makes no sense anymore since buttons have been moved to action bar
    	//updateButtonStates();

    	// calc keys fingerprints
        gatherKeysInfo();
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
	protected void onStart() {
		super.onStart();

		logger.debug("onStart()");

		loadPrefs();

		createPortsTable();
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
    }

    @Override
    protected void onPause() {
    	super.onPause();

    	logger.debug("onPause()");

    	// unregister broadcast receivers
        this.unregisterReceiver(this.receiver);
        this.unregisterReceiver(this.networkStateReceiver);
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
	 * Reads key file and gathers some info, like figerprints.
	 */
    protected void gatherKeysInfo() {
    	FileInputStream fis = null;
    	try {
        	fis = buildPrivatekeyInStream();

	    	// check if key is present
    		if (fis.available() <= 0) {
    			throw new Exception("key seems not to be present");
    		}

	    	KeyInfoProvider keyInfoprovider = new KeyInfoProvider();
    		PrivateKey privateKey = keyInfoprovider.readPrivatekey(fis);

	    	// fingerprints
	    	String fp = keyInfoprovider.fingerprint(privateKey, "MD5");
	    	if (fp != null) {
	    		md5Fingerprint = fp;
	    	}

    		fp = keyInfoprovider.fingerprint(privateKey, "SHA-1");
	    	if (fp != null) {
	    		sha1Fingerprint = fp;
	    	}

    		fp = keyInfoprovider.fingerprint(privateKey, "SHA-256");
	    	if (fp != null) {
	    		sha256Fingerprint = fp;
	    	}
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
    	createTableRow(table, label, value, 0);
    }

    /**
     * Creates a 2 column row in a table.
     *
     * @param table Table to add row to.
     * @param label Text for left column.
     * @param value Resource id of image to show in right column.
     */
    protected void createTableRow(
		TableLayout table,
		CharSequence label,
		int imageId)
    {
    	createTableRow(table, label, null, imageId);
    }

    protected void createTableRow(
		TableLayout table,
		CharSequence label,
		CharSequence value,
		int imageId)
    {
    	TableRow row = new TableRow(table.getContext());
    	table.addView(row);
    	row.setPadding(1, 1, 1, 5);

    	TextView labelView = new TextView(row.getContext());
    	row.addView(labelView);
    	labelView.setPadding(0, 0, 20, 0);
    	labelView.setText(label);

    	View valueView = null;
    	if (value != null) {
    		TextView valueTextView = new TextView(row.getContext());
    		valueTextView.setGravity(Gravity.LEFT);
    		valueTextView.setText(value);
    		valueView = valueTextView;
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
    	valueView.setLayoutParams(params);
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
    		"sftp",
    		prefsBean.getSecurePortStr());
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

    	createTableRow(
    		table,
    		getText(R.string.fingerprintsLabel),
    		R.drawable.refresh);

    	table = (TableLayout)findViewById(R.id.fingerprintsTable);
        // clear old entries
    	table.removeAllViews();

    	// note: HTML required for line breaks
    	// TODO better display of fingerprints
    	createTableRow(
    		table,
    		"MD5",
    		Html.fromHtml(md5Fingerprint));
    	createTableRow(
    		table,
    		"SHA1",
    		Html.fromHtml(sha1Fingerprint));
    	createTableRow(
    		table,
    		"SHA256",
    		Html.fromHtml(sha256Fingerprint));

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

    protected void genKeysAndShowProgressDiag() {
    	// critical: do not pass getApplicationContext() to dialog
    	final ProgressDialog progressDiag = new ProgressDialog(this);
    	progressDiag.setCancelable(false);
    	progressDiag.setMessage(getText(R.string.generatingKeysMessage));

    	AsyncTask<Void, Void, Void> task = new GenKeysAsyncTask(progressDiag);
		task.execute();

		progressDiag.show();
    }

    class GenKeysAskDialogFragment extends DialogFragment {
    	@Override
    	public Dialog onCreateDialog(Bundle savedInstanceState) {
    		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.generateKeysMessage);
            builder.setPositiveButton(R.string.generate, new DialogInterface.OnClickListener() {
            	@Override
				public void onClick(DialogInterface dialog, int id) {
            		genKeysAndShowProgressDiag();
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
    	private ProgressDialog progressDiag;

    	public GenKeysAsyncTask(ProgressDialog progressDiag) {
    		this.progressDiag = progressDiag;
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
			gatherKeysInfo();
			progressDiag.dismiss();
			createFingerprintTable();
		}
    }

    /**
     * @return True if at least one service is running.
     */
    protected boolean checkServicesRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		List<RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
		String ftpServiceClassName = FtpServerService.class.getName();
		String sshServiceClassName = SshServerService.class.getName();
		for (RunningServiceInfo service : runningServices) {
			String currentClassName = service.service.getClassName();
			if (ftpServiceClassName.equals(currentClassName)) {
				return true;
			}
			if (sshServiceClassName.equals(currentClassName)) {
				return true;
			}
		}
		return false;
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

    	boolean serviceRunning = checkServicesRunning();

    	startIcon.setVisible(!serviceRunning);
    	stopIcon.setVisible(serviceRunning);

    	// remove status bar notification if server not running
    	if (!serviceRunning) {
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

		// to avoid icon flicker when invoked via notification
		updateButtonStates();

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

		updateButtonStates();

		return super.onOptionsItemSelected(item);
	}

    protected void handleStart(MenuItem startIcon, MenuItem stopIcon) {
		if (StringUtils.isBlank(prefsBean.getPassword()))
		{
			Toast.makeText(
				getApplicationContext(),
				R.string.haveToSetPassword,
				Toast.LENGTH_LONG).show();

		} else {
	    	startService(createFtpServiceIntent());
	    	startService(createSshServiceIntent());
	    	startIcon.setVisible(false);
	    	stopIcon.setVisible(true);
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

    private static final int PORT_DEFAULT_VAL = 12345;
	private static final String PORT_DEFAULT_VAL_STR = String.valueOf(PORT_DEFAULT_VAL);
	private static final int SECURE_PORT_DEFAULT_VAL = 1234;
	private static final String SECURE_PORT_DEFAULT_VAL_STR =
		String.valueOf(SECURE_PORT_DEFAULT_VAL);

	/**
	 * @return Android {@link SharedPreferences} object.
	 */
	protected SharedPreferences getPrefs() {
		return PreferenceManager.getDefaultSharedPreferences(getBaseContext());
	}


	public static final String PREF_KEY_USER = "userNamePref";
	public static final String PREF_KEY_PASSWORD = "passwordPref";
	public static final String PREF_KEY_PORT = "portPref";
	public static final String PREF_KEY_SECURE_PORT = "securePortPref";
	public static final String PREF_KEY_ANNOUNCE = "announcePref";

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

		// load announcement setting
		boolean announce = prefs.getBoolean(PREF_KEY_ANNOUNCE, Boolean.TRUE);
		logger.debug("got announce: {}", Boolean.valueOf(announce));

		// load port
		int port = loadAndValidatePort(
			prefs,
			PREF_KEY_PORT,
			PORT_DEFAULT_VAL,
			PORT_DEFAULT_VAL_STR);

		// load secure port
		int securePort = loadAndValidatePort(
			prefs,
			PREF_KEY_SECURE_PORT,
			SECURE_PORT_DEFAULT_VAL,
			SECURE_PORT_DEFAULT_VAL_STR);

		// check if ports are equal
		if (port == securePort) {
			Toast.makeText(
				getApplicationContext(),
				R.string.portsEqual,
				Toast.LENGTH_LONG).show();
			port = PORT_DEFAULT_VAL;
			securePort = SECURE_PORT_DEFAULT_VAL;

			// reset in persistent prefs
			Editor prefsEditor = prefs.edit();
			prefsEditor.putString(
				PREF_KEY_PORT,
				PORT_DEFAULT_VAL_STR);
			prefsEditor.putString(
				PREF_KEY_SECURE_PORT,
				SECURE_PORT_DEFAULT_VAL_STR);
			prefsEditor.commit();
		}

		// create prefsBean
		PrefsBean oldPrefs = prefsBean;
		prefsBean = new PrefsBean(
			userName,
			password,
			port,
			securePort,
			announce);

		// TODO oldPrefs is null when user navigates via action bar,
		// find other way to figure out if prefs have changed
		if (oldPrefs != null) {
			if (!oldPrefs.equals(prefsBean) && checkServicesRunning()) {
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
