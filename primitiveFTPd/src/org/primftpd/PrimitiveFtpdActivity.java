package org.primftpd;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

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
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
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

	private BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			loadPrefs();
			NetworkStateChanged();
			redrawTableandFilter();
			restartFTPServer();
		}
	};

	private BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        logger.debug("network connectivity changed, data str: '{}', action: '{}'",
	        		intent.getDataString(),
	        		intent.getAction());
			loadPrefs();
	        NetworkStateChanged();
	    	redrawTableandFilter();
	    	restartFTPServer();
	    }
	};

	protected static final String SERVICE_CLASS_NAME = "org.primftpd.FtpServerService";
	public static final String EXTRA_PREFS_BEAN = "prefs.bean";
	protected boolean boot_call=false;
	protected List<String> interfaces_list=new ArrayList<String>();
	protected List<String> ip_list=new ArrayList<String>();
	protected ArrayList<String> bind_ip_list=new ArrayList<String>();

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
        if(getIntent().getExtras()!=null){
	        if (getIntent().getExtras().getString("BOOT")!=null){
	            boot_call=true;
	        }
        }

		IntentFilter filter = new IntentFilter();
        filter.addAction(FtpServerService.BROADCAST_ACTION_COULD_NOT_START);
        this.registerReceiver(this.receiver, filter);


    	// register listener to reprint interfaces table when network connections change
    	filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
    	registerReceiver(this.networkStateReceiver, filter);
    	
    	filter=new IntentFilter();
    	filter.addAction("org.primftpd.RestartUI");
    	registerReceiver(this.refreshReceiver,filter);
    	
    	loadPrefs();
    	NetworkStateChanged();
    	// XXX SSL
    	// calc certificate fingerprints
//    	KeyStore keyStore = KeyStoreUtil.loadKeyStore(getResources());
//    	md5Fingerprint = KeyStoreUtil.calcKeyFingerprint(keyStore, "MD5");
//    	sha1Fingerprint = KeyStoreUtil.calcKeyFingerprint(keyStore, "SHA-1");
//    	createFingerprintTable();
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
		
    	if(boot_call&&!prefsBean.isBootStart()){
			finish();
		}else{
			createPortsTable();
			createUsernameTable();
		}
	}

    @Override
    protected void onResume() {
    	super.onResume();

    	logger.debug("onResume()");


    }
    protected void onDestroy() {
    	super.onDestroy();

    	logger.debug("onDestroy()");

    	// unregister broadcast receivers
        this.unregisterReceiver(this.receiver);
        this.unregisterReceiver(this.networkStateReceiver);
        this.unregisterReceiver(this.refreshReceiver);
    }
    
    @Override
    public void onBackPressed() {
    	if(checkServiceRunning()){
    		moveTaskToBack(true);
    	}else{
    		super.onBackPressed();    		
    	}
    }

    @Override
    protected void onPause() {
    	super.onPause();

    	logger.debug("onPause()");

    }

    protected void NetworkStateChanged(){
    	interfaces_list.clear();
    	ip_list.clear();
    	try {
			if(prefsBean.isWifiMode()){
	    		WifiManager wifiManager = (WifiManager) getSystemService (Context.WIFI_SERVICE);
	    		WifiInfo info = wifiManager.getConnectionInfo ();
	    		if(info!=null){
	    			if(info.getSSID()!=null){
	    				if(info.getIpAddress()!=0){
	    					String ipString = String.format(Locale.getDefault(),"%d.%d.%d.%d",(info.getIpAddress() & 0xff),(info.getIpAddress() >> 8 & 0xff),(info.getIpAddress() >> 16 & 0xff),(info.getIpAddress() >> 24 & 0xff));
	    					ip_list.add(ipString);
	    					if(info.getSSID().startsWith("\"")&&info.getSSID().endsWith("\"")){
	    						interfaces_list.add(info.getSSID().substring(1,info.getSSID().length()-1));
	    					}else{
	    						interfaces_list.add(info.getSSID());
	    					}
	    				}
	    			}
	    		}
			}else{
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
						ip_list.add(hostAddr);
						interfaces_list.add(ifaceName);
	                }
	
	            }
			}
	    } catch (SocketException e) {
	    	logger.info("exception while iterating network interfaces", e);
	
	    	String msg = getText(R.string.ifacesError) + e.getLocalizedMessage();
	    	Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	    }
    }
    
    protected void redrawTableandFilter(){
    	TableLayout table = (TableLayout)findViewById(R.id.ifacesTable);
    	ArrayList<String> allowed_interfaces=new ArrayList<String>();
    	bind_ip_list.clear();
    	for(String s:Arrays.asList(prefsBean.getAllowedInterfaces().split(","))){
    		String f=s.trim();
    		allowed_interfaces.add(f);
    	}
    	
    	// clear old entries
    	table.removeAllViews();

    	// create header line
    	createTableRow(
    		table,
    		getText(R.string.ifacesLabel),
    		getText(R.string.ipAddrLabel),"");
    	
    	for( int i=0;i<interfaces_list.size();i++ )
    	{
    		if(interfaces_list.get(i)!=null){
	    		if(allowed_interfaces!=null){
	    			if(allowed_interfaces.size()>0){
	    				if((allowed_interfaces.size()==1)&&
	    						(allowed_interfaces.get(0).length()==0)){
	    	    			createTableRow(table,interfaces_list.get(i),ip_list.get(i),"o");
	    	    			bind_ip_list.add(ip_list.get(i));
	    				}else{
		    				if(allowed_interfaces.contains(interfaces_list.get(i))){
		    	    			createTableRow(table,interfaces_list.get(i),ip_list.get(i),"o");
		    	    			bind_ip_list.add(ip_list.get(i));
		    				}else{
		    	    			createTableRow(table,interfaces_list.get(i),ip_list.get(i),"x");
		    				}
	    				}
	    			}else{
    	    			createTableRow(table,interfaces_list.get(i),ip_list.get(i),"o");
    	    			bind_ip_list.add(ip_list.get(i));
	    			}
	    		}else{
	    			createTableRow(table,interfaces_list.get(i),ip_list.get(i),"o");
	    			bind_ip_list.add(ip_list.get(i));
	    		}
    		}
    	}
    }

    protected void restartFTPServer(){
    	if(checkServiceRunning()){
	        handleStop(startIcon, stopIcon);
	        handleStart(startIcon, stopIcon);
    
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
    		CharSequence value,
    		CharSequence bound)
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
    	if(bound.length()>0){
    		valueView.setText(value+" ("+bound+")");
    	}else{
    		valueView.setText(value);
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
    	createTableRow(
    		table,
    		getText(R.string.protocolLabel),
    		getText(R.string.portLabel),"");

    	createTableRow(
    		table,
    		"ftp",
    		prefsBean.getPortStr(),"");

    	// XXX SSL
//    	createTableRow(
//    		table,
//    		"ftps",
//    		prefsBean.getSslPortStr());
    }

    protected void createUsernameTable() {
    	TableLayout table = (TableLayout)findViewById(R.id.usernameTable);

        // clear old entries
    	table.removeAllViews();

    	// create header line
    	createTableRow(
    		table,
    		getText(R.string.prefTitleUser),
    		prefsBean.getUserName(),"");
    }

    protected void createFingerprintTable() {
    	// note: HTML required for line breaks
    	TableLayout table = (TableLayout)findViewById(R.id.fingerprintsTable);
    	createTableRow(
    		table,
    		"MD5",
    		Html.fromHtml(md5Fingerprint),"");
    	createTableRow(
    		table,
    		"SHA1",
    		Html.fromHtml(sha1Fingerprint),"");
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
    	if (startIcon == null || stopIcon == null) {
            logger.debug("updateButtonStates(), no icons");
    		return;
    	}

        logger.debug("updateButtonStates()");

    	boolean serviceRunning = checkServiceRunning();

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
		if(boot_call&&prefsBean.isBootStart()){
	        handleStart(startIcon, stopIcon);
			moveTaskToBack(true);
			boot_call=false;
		}
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
    		if(bind_ip_list!=null){
    			if(bind_ip_list.size()==0){//no servers to bind to
   					handleStop(startIcon, stopIcon);
    				return;
    			}
    		}
    		Intent intent = createFtpServiceIntent();
    		intent.putStringArrayListExtra("bind_ip", bind_ip_list);
	    	startService(intent);
	    	if((startIcon!=null)&&(stopIcon!=null)){
	    		startIcon.setVisible(false);
	    		stopIcon.setVisible(true);
	    	}
		}
    }

    protected void handleStop(MenuItem startIcon, MenuItem stopIcon) {
    	Intent intent = createFtpServiceIntent();
    	stopService(intent);
    	if((startIcon!=null)&&(stopIcon!=null)){
	    	startIcon.setVisible(true);
	    	stopIcon.setVisible(false);
    	}
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
    	intent.putExtra(EXTRA_PREFS_BEAN, prefsBean);
    	return intent;
    }

	private static final int PORT_DEFAULT_VAL = 12345;
	private static final String PORT_DEFAULT_VAL_STR = String.valueOf(PORT_DEFAULT_VAL);
	private static final int SSL_PORT_DEFAULT_VAL = 1234;
	@SuppressWarnings("unused") // XXX SSL
	private static final String SSL_PORT_DEFAULT_VAL_STR = String.valueOf(SSL_PORT_DEFAULT_VAL);

	/**
	 * @return Android {@link SharedPreferences} object.
	 */
	protected SharedPreferences getPrefs() {
		return PreferenceManager.getDefaultSharedPreferences(getBaseContext());
	}


	public static final String PREF_KEY_USER = "userNamePref";
	public static final String PREF_KEY_PASSWORD = "passwordPref";
	public static final String PREF_KEY_PORT = "portPref";
	public static final String PREF_KEY_SSL_PORT = "sslPortPref";
	public static final String PREF_KEY_ANNOUNCE = "announcePref";
	public static final String PREF_KEY_BOOTSTART = "bootstartPref";
	public static final String PREF_KEY_WIFIMODE = "wifimodePref";
	public static final String PREF_KEY_ALLOWED_INTERFACES = "allowedInterfacesPref";

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

		// load start on boot setting
		boolean bootstart=prefs.getBoolean(PREF_KEY_BOOTSTART, Boolean.FALSE);
		logger.debug("got bootstart: {}", Boolean.valueOf(bootstart));

		// load wifi mode setting
		boolean wifimode=prefs.getBoolean(PREF_KEY_WIFIMODE, Boolean.FALSE);
		logger.debug("got wifimode: {}", Boolean.valueOf(wifimode));
		
		String allowed_interfaces=prefs.getString(PREF_KEY_ALLOWED_INTERFACES,"");
		logger.debug("got allowed interfaces: {}", allowed_interfaces);
		
		// load port
		int port = loadAndValidatePort(
			prefs,
			PREF_KEY_PORT,
			PORT_DEFAULT_VAL,
			PORT_DEFAULT_VAL_STR);

		// XXX SSL
		int sslPort = SSL_PORT_DEFAULT_VAL;
		// load SSL port
//		int sslPort = loadAndValidatePort(
//			prefs,
//			PREF_KEY_SSL_PORT,
//			SSL_PORT_DEFAULT_VAL,
//			SSL_PORT_DEFAULT_VAL_STR);
//
//		// check if ports are equal
//		if (port == sslPort) {
//			Toast.makeText(
//				getApplicationContext(),
//				R.string.portsEqual,
//				Toast.LENGTH_LONG).show();
//			port = PORT_DEFAULT_VAL;
//			sslPort = SSL_PORT_DEFAULT_VAL;
//
//			// reset in persistent prefs
//			Editor prefsEditor = prefs.edit();
//			prefsEditor.putString(
//				PREF_KEY_PORT,
//				PORT_DEFAULT_VAL_STR);
//			prefsEditor.putString(
//				PREF_KEY_SSL_PORT,
//				SSL_PORT_DEFAULT_VAL_STR);
//			prefsEditor.commit();
//		}

		// create prefsBean
		PrefsBean oldPrefs = prefsBean;
		prefsBean = new PrefsBean(
			userName,
			password,
			port,
			sslPort,
			announce,
			bootstart,
			wifimode,
			allowed_interfaces);

		// TODO oldPrefs is null when user navigates via action bar,
		// find other way to figure out if prefs have changed
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
