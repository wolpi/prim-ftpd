package org.primftpd;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

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
				RedrawUI();
			}
		}
	};

	private BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			loadPrefs();
			NetworkStateChanged();
	        RedrawUI();
			restartFTPServer();
		}
	};

	//a timer used to filter spammy broadcasts of changing network connections to avoid restarting the service too often
	protected static Timer networkStateReceiverFilter;
	private BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        logger.debug("network connectivity changed, data str: '{}', action: '{}'",
	        		intent.getDataString(),
	        		intent.getAction());

	        if(networkStateReceiverFilter!=null){//if timer already runs cancel it
	    		networkStateReceiverFilter.cancel();
	    	}
	    	networkStateReceiverFilter = new Timer();
	    	networkStateReceiverFilter.schedule(new TimerTask() {//start 5s timer until the service is restarted
	            @Override
	            public void run() {
	                runOnUiThread(new Runnable() {
	                	public void run() {
	        		        loadPrefs();
	        		        NetworkStateChanged();
	        		        RedrawUI();
	        		    	restartFTPServer();
	        		    }
	                });

	            }
	    	}, 5000);
    
	    }
	};

	protected static final String SERVICE_CLASS_NAME = "org.primftpd.FtpServerService";
	public static final String EXTRA_PREFS_BEAN = "prefs.bean";
	//contains a list of all detected interfaces
	protected List<String> interfacesList=new ArrayList<String>();
	//contains a list of the ip addresses of all detected interfaces
	protected List<String> ipList=new ArrayList<String>();
	//contains a list off all detected ip addresses and a marker if they should be bound to by the service
	protected ArrayList<String> bindIpList=new ArrayList<String>();
	//if true, the user wants to have the service off (usually after pressing the stop button)
	protected boolean forceOff;

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
        
        loadPrefs();//load the current preferences
    	NetworkStateChanged();//determine network addresses and apply filters

    	//register listener to display a toast, if the service could not be started
		IntentFilter filter = new IntentFilter();
        filter.addAction(FtpServerService.BROADCAST_ACTION_COULD_NOT_START);
        this.registerReceiver(this.receiver, filter);

    	// register listener to refresh the UI and to rebind the service to new interfaces when network connections change
    	filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
    	registerReceiver(this.networkStateReceiver, filter);
    	
    	// register listener to refresh the UI via a broadcast (e.g. when leaving the settings activity)
    	filter=new IntentFilter();
    	filter.addAction("org.primftpd.RedrawUI");
    	registerReceiver(this.refreshReceiver,filter);

    	//are we being called on bootup`?
    	if(getIntent().getExtras()!=null){
	        if (getIntent().getExtras().getString("BOOT")!=null){//start the service and quit
	            forceOff=false;
	            StartFTPServer();
	            moveTaskToBack(true);
	            finish();
	        }
        }
        
        setContentView(R.layout.main);
        //initialize forceOff to be able to determine later, if the service should be restarted or stopped, if the network connection changes
        if(checkServiceRunning()){
        	forceOff=false;
        }else{
        	forceOff=true;
        }
        
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
        	RedrawUI();
    	}
    }

	@Override
	protected void onStart() {
		super.onStart();

		logger.debug("onStart()");

		loadPrefs();
	
		RedrawUI();
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
    	if(checkServiceRunning()){//if the service is still running, do not quit the application
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

    protected MenuItem startIcon;
	protected MenuItem stopIcon;

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
        logger.debug("onCreateOptionsMenu()");
		getMenuInflater().inflate(R.menu.pftpd, menu);
		startIcon = menu.findItem(R.id.menu_start);
		stopIcon = menu.findItem(R.id.menu_stop);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_start:
			forceOff=false;
			StartFTPServer();
			break;
		case R.id.menu_stop:
			forceOff=true;
			StopFTPServer();
			break;
		case R.id.menu_prefs:
			handlePrefs();
			break;
		}
		RedrawUI();
		return super.onOptionsItemSelected(item);
	}
	
/*********************************************************************************************************/
	/**
	 * redraws the complete user interface by calling all needed methods for
	 * the different parts of the layout	
	 */
	protected void RedrawUI(){
		createPortsTable();
		createUsernameTable();
		redrawTable();
		updateButtonStates();
	}
	
	/**
	 * fills the table with entries of IP and networks interfaces/SSIDs 
	 */
    protected void redrawTable(){
    	TableLayout table = (TableLayout)findViewById(R.id.ifacesTable);

    	// clear old entries
    	table.removeAllViews();
    	
    	// create header line
    	createTableRow(
    		table,
    		getText(R.string.ifacesLabel),
    		getText(R.string.ipAddrLabel),"");
    	
    	for( int i=0;i<interfacesList.size();i++ )
    	{	
    		if(bindIpList.get(i).split("\0")[1].equals("1")){
    			createTableRow(table,interfacesList.get(i),bindIpList.get(i).split("\0")[0],"o");
    		}else{
    			createTableRow(table,interfacesList.get(i),bindIpList.get(i).split("\0")[0],"x");
    		}
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

/*************************************************************************************************************************/
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
	 * determines the available network interfaces and their corresponding ip addresses
	 */
    protected void NetworkStateChanged(){
    	loadPrefs();
    	interfacesList.clear();
    	ipList.clear();
    	try {
			if(prefsBean.isWifiMode()){
	    		WifiManager wifiManager = (WifiManager) getSystemService (Context.WIFI_SERVICE);
	    		WifiInfo info = wifiManager.getConnectionInfo ();
	    		if(info!=null){
	    			if(info.getSSID()!=null){
	    				if(info.getIpAddress()!=0){
	    					String ipString = String.format(Locale.getDefault(),"%d.%d.%d.%d",(info.getIpAddress() & 0xff),(info.getIpAddress() >> 8 & 0xff),(info.getIpAddress() >> 16 & 0xff),(info.getIpAddress() >> 24 & 0xff));
	    					ipList.add(ipString);
	    					if(info.getSSID().startsWith("\"")&&info.getSSID().endsWith("\"")){
	    						interfacesList.add(info.getSSID().substring(1,info.getSSID().length()-1));
	    					}else{
	    						interfacesList.add(info.getSSID());
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
						ipList.add(hostAddr);
						interfacesList.add(ifaceName);
	                }
	
	            }
			}
			FilterBindInterfaces();
	    } catch (SocketException e) {
	    	logger.info("exception while iterating network interfaces", e);
	
	    	String msg = getText(R.string.ifacesError) + e.getLocalizedMessage();
	    	Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	    }

    }
    
    /**
     * filters out all interfaces that should not be bound to by the service
     * as setup in the preferences
     */
    protected void FilterBindInterfaces(){
    	ArrayList<String> allowedInterfaces=new ArrayList<String>();
    	bindIpList.clear();
    	for(String s:Arrays.asList(prefsBean.getAllowedInterfaces().split(","))){
    		String f=s.trim();
    		allowedInterfaces.add(f);
    	}
    	for( int i=0;i<interfacesList.size();i++ )
    	{
    		if(interfacesList.get(i)!=null){
	    		if(allowedInterfaces!=null){
	    			if(allowedInterfaces.size()>0){
	    				if((allowedInterfaces.size()==1)&&
	    						(allowedInterfaces.get(0).length()==0)){
	    	    			bindIpList.add(ipList.get(i)+"\0"+"1");
	    				}else{
		    				if(allowedInterfaces.contains(interfacesList.get(i))){
		    	    			bindIpList.add(ipList.get(i)+"\0"+"1");
		    				}else{
		    	    			bindIpList.add(ipList.get(i)+"\0"+"0");
		    				}
	    				}
	    			}else{
    	    			bindIpList.add(ipList.get(i)+"\0"+"1");
	    			}
	    		}else{
	    			bindIpList.add(ipList.get(i)+"\0"+"1");
	    		}
    		}
    	}
    }
    
    /**
     * restarts the service
     * also takes care if the user actually wants to disable the service
     */
    protected void restartFTPServer(){
    	if(forceOff){
    		StopFTPServer();
    	}else{
    		StopFTPServer();
    		StartFTPServer();
    	}
    }

    /**
     * starts the 
     */
    protected void StartFTPServer() {
		if (StringUtils.isBlank(prefsBean.getPassword()))
		{
			Toast.makeText(
				getApplicationContext(),
				R.string.haveToSetPassword,
				Toast.LENGTH_LONG).show();

		} else {
			int i=0;
    		if(bindIpList!=null){
				for(String s:bindIpList){
					if(s.split("\0")[1].equals("1")){
						i++;
					}
				}
    		}
   			if(i==0){//no servers to bind to
				StopFTPServer();
   				return;
    		}
    		Intent intent = createFtpServiceIntent();
    		intent.putStringArrayListExtra("bindIP", bindIpList);
	    	startService(intent);
		}
    }

	protected void StopFTPServer() {
		Intent intent = createFtpServiceIntent();
		stopService(intent);
	}

/*************************************************************************************************************************/
	
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
		boolean bootStart=prefs.getBoolean(PREF_KEY_BOOTSTART, Boolean.FALSE);
		logger.debug("got bootstart: {}", Boolean.valueOf(bootStart));

		// load wifi mode setting
		boolean wifiMode=prefs.getBoolean(PREF_KEY_WIFIMODE, Boolean.FALSE);
		logger.debug("got wifimode: {}", Boolean.valueOf(wifiMode));
		
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
			bootStart,
			wifiMode,
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
