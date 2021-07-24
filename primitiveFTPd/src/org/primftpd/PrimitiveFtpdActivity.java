package org.primftpd;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewManager;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.primftpd.events.ClientActionEvent;
import org.primftpd.log.PrimFtpdLoggerBinder;
import org.primftpd.prefs.AboutActivity;
import org.primftpd.prefs.FtpPrefsActivityThemeDark;
import org.primftpd.prefs.FtpPrefsActivityThemeLight;
import org.primftpd.prefs.LoadPrefsUtil;
import org.primftpd.prefs.Logging;
import org.primftpd.prefs.PrefsBean;
import org.primftpd.prefs.StorageType;
import org.primftpd.prefs.Theme;
import org.primftpd.events.ServerInfoRequestEvent;
import org.primftpd.events.ServerInfoResponseEvent;
import org.primftpd.events.ServerStateChangedEvent;
import org.primftpd.ui.CalcPubkeyFinterprintsTask;
import org.primftpd.ui.ClientActionActivity;
import org.primftpd.ui.GenKeysAskDialogFragment;
import org.primftpd.ui.GenKeysAsyncTask;
import org.primftpd.util.IpAddressProvider;
import org.primftpd.util.KeyFingerprintProvider;
import org.primftpd.util.NotificationUtil;
import org.primftpd.util.PrngFixes;
import org.primftpd.util.ServersRunningBean;
import org.primftpd.util.ServicesStartStopUtil;
import org.primftpd.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import androidx.fragment.app.FragmentActivity;

/**
 * Activity to display network info and to start FTP service.
 */
public class PrimitiveFtpdActivity extends FragmentActivity {

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

	private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0xBEEF;
	private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_LOGGING = 0xCAFE;
	private static final int REQUEST_CODE_SAF_PERM = 1234;

	public static final String DIALOG_TAG = "dialogs";

	protected Logger logger = LoggerFactory.getLogger(getClass());

	private PrefsBean prefsBean;
	private IpAddressProvider ipAddressProvider = new IpAddressProvider();
	private KeyFingerprintProvider keyFingerprintProvider = new KeyFingerprintProvider();
	private Theme theme;
	private ServersRunningBean serversRunning;
	private long timestampOfLastEvent = 0;

	private TextView clientActionView1;
	private TextView clientActionView2;
	private TextView clientActionView3;

	protected int getLayoutId() {
		return R.layout.main;
	}

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
		setContentView(getLayoutId());

		// calc keys fingerprints
		AsyncTask<Void, Void, Void> task = new CalcPubkeyFinterprintsTask(keyFingerprintProvider, this);
		task.execute();

		// create addresses label
		((TextView) findViewById(R.id.addressesLabel)).setText(
				String.format("%s (%s)", getText(R.string.ipAddrLabel), getText(R.string.ifacesLabel))
		);

		// create ports label
		((TextView) findViewById(R.id.portsLabel)).setText(
				String.format("%s / %s / %s",
						getText(R.string.protocolLabel), getText(R.string.portLabel), getText(R.string.state))
		);

		// listen for events
		EventBus.getDefault().register(this);

		// hide SAF storage type radios and texts for old androids
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			View radioStorageSaf = findViewById(R.id.radioStorageSaf);
			((ViewManager)radioStorageSaf.getParent()).removeView(radioStorageSaf);

			View radioStorageRoSaf = findViewById(R.id.radioStorageRoSaf);
			((ViewManager)radioStorageRoSaf.getParent()).removeView(radioStorageRoSaf);

			View safExplainHeading = findViewById(R.id.safExplainHeading);
			((ViewManager)safExplainHeading.getParent()).removeView(safExplainHeading);

			View safExplain = findViewById(R.id.safExplain);
			((ViewManager)safExplain.getParent()).removeView(safExplain);
		}

		// start on open ?
		Boolean startOnOpen = LoadPrefsUtil.startOnOpen(prefs);
		if (startOnOpen) {
			PrefsBean prefsBean = LoadPrefsUtil.loadPrefs(logger, prefs);
			keyFingerprintProvider.calcPubkeyFingerprints(this); // see GH issue #204
			ServicesStartStopUtil.startServers(
					getBaseContext(),
					prefsBean,
					keyFingerprintProvider,
					this);
		}

		// init client action views
		clientActionView1 = findViewById(R.id.clientActionsLine1);
		clientActionView2 = findViewById(R.id.clientActionsLine2);
		clientActionView3 = findViewById(R.id.clientActionsLine3);
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
		showLogindata();

        // init storage type radio
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            switch (prefsBean.getStorageType()) {
                case PLAIN:
                    ((RadioButton) findViewById(R.id.radioStoragePlain)).setChecked(true);
                    break;
                case ROOT:
                    ((RadioButton) findViewById(R.id.radioStorageRoot)).setChecked(true);
                    break;
                case SAF:
                    ((RadioButton) findViewById(R.id.radioStorageSaf)).setChecked(true);
                    showSafUrl(prefsBean.getSafUrl());
                    break;
                case RO_SAF:
                    ((RadioButton) findViewById(R.id.radioStorageRoSaf)).setChecked(true);
                    showSafUrl(prefsBean.getSafUrl());
                    break;
            }
        } else {
            switch (prefsBean.getStorageType()) {
                case PLAIN:
                    ((RadioButton) findViewById(R.id.radioStoragePlain)).setChecked(true);
                    break;
                case ROOT:
                    ((RadioButton) findViewById(R.id.radioStorageRoot)).setChecked(true);
                    break;
            }
        }
	}

	@Override
	protected void onResume() {
		super.onResume();

		logger.debug("onResume()");

		// register listener to reprint interfaces table when network connections change
		// android sends those events when registered in code but not when registered in manifest
		IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(this.networkStateReceiver, filter);

		// e.g. necessary when ports preferences have been changed
		displayServersState();

		// check if chosen SAF directory can be accessed
		checkSafAccess();
	}

	@Override
	protected void onPause() {
		super.onPause();

		logger.debug("onPause()");

		// unregister broadcast receiver
		this.unregisterReceiver(this.networkStateReceiver);
	}

	public void onRadioButtonClicked(View view) {
		logger.debug("onRadioButtonClicked()");
		findViewById(R.id.safUriLabel).setVisibility(View.GONE);
		findViewById(R.id.safUri).setVisibility(View.GONE);

		StorageType storageType = null;

		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		intent.addFlags(
				Intent.FLAG_GRANT_READ_URI_PERMISSION
				| Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		try {
			switch (view.getId()) {
				case R.id.radioStoragePlain:
					storageType = StorageType.PLAIN;
					break;
				case R.id.radioStorageRoot:
					storageType = StorageType.ROOT;
					break;
				case R.id.radioStorageSaf:
					storageType = StorageType.SAF;
					startActivityForResult(intent, REQUEST_CODE_SAF_PERM);
					break;
				case R.id.radioStorageRoSaf:
					storageType = StorageType.RO_SAF;
					startActivityForResult(intent, REQUEST_CODE_SAF_PERM);
					break;
			}
		} catch (ActivityNotFoundException e) {
			Toast.makeText(getBaseContext(), "SAF seems to be broken on your device :(", Toast.LENGTH_SHORT);
			storageType = StorageType.PLAIN;
		}

		SharedPreferences prefs = LoadPrefsUtil.getPrefs(getBaseContext());
		LoadPrefsUtil.storeStorageType(prefs, storageType);

		if (storageType == StorageType.PLAIN || storageType == StorageType.ROOT) {
			loadPrefs();
			checkSafAccess();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		logger.debug("onActivityResult()");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			if (requestCode == REQUEST_CODE_SAF_PERM && resultCode == Activity.RESULT_OK) {
				if (intent != null) {
					Uri uri = intent.getData();
					String uriStr = uri.toString();
					logger.debug("got uri: '{}'", uriStr);

					int modeFlags =
							(Intent.FLAG_GRANT_READ_URI_PERMISSION
							| Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

					// release old permissions
					String oldUrl = prefsBean.getSafUrl();
					if (!StringUtils.isBlank(oldUrl)) {
						try {
							getContentResolver().releasePersistableUriPermission(Uri.parse(oldUrl), modeFlags);
						} catch (SecurityException e) {
							logger.info("SecurityException while calling releasePersistableUriPermission()");
							logger.trace("", e);
						}
					}

					// persist permissions
					try {
						grantUriPermission(getPackageName(), uri, modeFlags);
						getContentResolver().takePersistableUriPermission(uri, modeFlags);
					} catch (SecurityException e) {
						logger.info("SecurityException while calling takePersistableUriPermission()");
						logger.trace("", e);
					}

					// store uri
					SharedPreferences prefs = LoadPrefsUtil.getPrefs(getBaseContext());
					LoadPrefsUtil.storeSafUrl(prefs, uriStr);

					// display uri
					showSafUrl(uriStr);

					// update prefs
					loadPrefs();

					// note: onResume() is about to be called
				}
			}
		}
	}

	protected void checkSafAccess() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			boolean hideWarning = true;
			RadioButton safRadio = findViewById(R.id.radioStorageSaf);
			if (prefsBean.getStorageType() == StorageType.SAF || prefsBean.getStorageType() == StorageType.RO_SAF) {
				// let's see if the OS has persisted something for us
				List<UriPermission> persistedUriPermissions = getContentResolver().getPersistedUriPermissions();
				for (UriPermission uriPerm : persistedUriPermissions) {
					logger.debug("persisted uri perm: '{}', pref uri: '{}'", uriPerm.getUri(), prefsBean.getSafUrl());
				}
				if (persistedUriPermissions.isEmpty()) {
					logger.debug("no persisted uri perm");
				}

				Cursor cursor = null;
				try {
					String url = prefsBean.getSafUrl();
					Uri uri = Uri.parse(url);
					cursor = getContentResolver().query(
							uri,
							new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID},
							null,
							null,
							null,
							null);
					cursor.moveToFirst();

				} catch (UnsupportedOperationException e) {
					// this seems to be the normal case for directory uris
				} catch (SecurityException | NullPointerException e) {
					logger.debug("checkSafAccess failed: {}", e.toString());
					logger.trace("", e);
					hideWarning = false;
				} finally {
					if (cursor != null) {
						cursor.close();
					}
				}
			}
			if (hideWarning) {
				// remove warning if it was present
				safRadio.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			} else {
				int icon = theme == Theme.DARK
						? R.drawable.ic_warning_white_36dp
						: R.drawable.ic_warning_black_36dp;
				safRadio.setCompoundDrawablesWithIntrinsicBounds(0, 0, icon, 0);
			}
		}
	}

	protected boolean isLeftToRight() {
		boolean isLeftToRight = true;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			Configuration config = getResources().getConfiguration();
			isLeftToRight = config.getLayoutDirection() == View.LAYOUT_DIRECTION_LTR;
		}
		return isLeftToRight;
	}

	/**
	 * Creates table containing network interfaces.
	 */
	protected void showAddresses() {
		LinearLayout container = (LinearLayout)findViewById(R.id.addressesContainer);

		// clear old entries
		container.removeAllViews();

		boolean isLeftToRight = isLeftToRight();
		List<String> displayTexts = ipAddressProvider.ipAddressTexts(this, true, isLeftToRight);
		for (String displayText : displayTexts) {
			TextView textView = new TextView(container.getContext());
			container.addView(textView);
			textView.setText(displayText);
			textView.setGravity(Gravity.CENTER_HORIZONTAL);
			textView.setTextIsSelectable(true);
		}

	}

	@SuppressLint("SetTextI18n")
	protected void showPortsAndServerState() {
		boolean isLeftToRight = isLeftToRight();

		if (isLeftToRight) {
			((TextView) findViewById(R.id.ftpTextView))
					.setText("ftp / " + prefsBean.getPortStr() + " / " +
							getText(serversRunning.ftp
									? R.string.serverStarted
									: R.string.serverStopped));

			((TextView) findViewById(R.id.sftpTextView))
					.setText("sftp / " + prefsBean.getSecurePortStr() + " / " +
							getText(serversRunning.ssh
									? R.string.serverStarted
									: R.string.serverStopped));
		} else {
			((TextView) findViewById(R.id.ftpTextView))
					.setText(prefsBean.getPortStr() + " / " +
							getText(serversRunning.ftp
									? R.string.serverStarted
									: R.string.serverStopped)
					+ " / " + "ftp");

			((TextView) findViewById(R.id.sftpTextView))
					.setText(prefsBean.getSecurePortStr() + " / " +
							getText(serversRunning.ssh
									? R.string.serverStarted
									: R.string.serverStopped)
					+ " / " + "sftp");
		}
	}

	protected void showLogindata() {
		TextView usernameView = (TextView)findViewById(R.id.usernameTextView);
		usernameView.setText(prefsBean.getUserName());

		TextView anonymousView = (TextView)findViewById(R.id.anonymousLoginTextView);
		anonymousView.setText(getString(R.string.isAnonymous, prefsBean.isAnonymousLogin()));

		TextView passwordPresentView = (TextView)findViewById(R.id.passwordPresentTextView);
		passwordPresentView.setText(getString(R.string.passwordPresent,
				StringUtils.isNotEmpty(prefsBean.getPassword())));

		TextView pubKeyAuthView = (TextView)findViewById(R.id.pubKeyAuthTextView);
		pubKeyAuthView.setText(getString(R.string.pubKeyAuth, prefsBean.isPubKeyAuth()));
	}

	protected void showSafUrl(String url) {
		findViewById(R.id.safUriLabel).setVisibility(View.VISIBLE);
		TextView safUriView = (TextView)findViewById(R.id.safUri);
		safUriView.setVisibility(View.VISIBLE);
		safUriView.setText(url);
	}

	@SuppressLint("SetTextI18n")
	public void showKeyFingerprints() {
		((TextView)findViewById(R.id.keyFingerprintMd5Label))
				.setText("MD5");
		((TextView)findViewById(R.id.keyFingerprintSha1Label))
				.setText("SHA1");
		((TextView)findViewById(R.id.keyFingerprintSha256Label))
				.setText("SHA256");

		((TextView)findViewById(R.id.keyFingerprintMd5TextView))
			.setText(keyFingerprintProvider.getFingerprintMd5());
		((TextView)findViewById(R.id.keyFingerprintSha1TextView))
			.setText(keyFingerprintProvider.getFingerprintSha1());
		((TextView)findViewById(R.id.keyFingerprintSha256TextView))
			.setText(keyFingerprintProvider.getFingerprintSha256());

		// create onRefreshListener
		final PrimitiveFtpdActivity activity = this;
		View refreshButton = findViewById(R.id.keyFingerprintsLabel);
		refreshButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				logger.trace("refreshButton OnClickListener");
				GenKeysAskDialogFragment askDiag = new GenKeysAskDialogFragment();
				askDiag.show(activity.getSupportFragmentManager(), DIALOG_TAG);
			}
		});
	}

	public void genKeysAndShowProgressDiag(boolean startServerOnFinish) {
		logger.trace("genKeysAndShowProgressDiag()");
		// critical: do not pass getApplicationContext() to dialog
		final ProgressDialog progressDiag = new ProgressDialog(this);
		progressDiag.setCancelable(false);
		progressDiag.setMessage(getText(R.string.generatingKeysMessage));

		AsyncTask<Void, Void, Void> task = new GenKeysAsyncTask(
			keyFingerprintProvider,
			this,
			progressDiag,
			startServerOnFinish);
		task.execute();

		progressDiag.show();
	}

	private boolean isEventInTime(Object event) {
		long currentTime = System.currentTimeMillis();
		long offset = currentTime - timestampOfLastEvent;
		boolean inTime = offset > 20;
		if (inTime) {
			logger.debug("handling event '{}', offset: {} ms", event.getClass().getName(), Long.valueOf(offset));
			timestampOfLastEvent = currentTime;
		} else {
			logger.debug("ignoring event '{}', offset: {} ms", event.getClass().getName(), Long.valueOf(offset));
		}
		return inTime;
	}
	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(ServerStateChangedEvent event) {
		logger.debug("got ServerStateChangedEvent");
		if (isEventInTime(event)) {
			displayServersState();
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(ServerInfoResponseEvent event) {
		String quickShareFilename = event.getQuickShareFilename();
		logger.debug("got ServerInfoResponseEvent, quickShareFilename: {}", quickShareFilename);
		if (isEventInTime(event)) {
			if (quickShareFilename != null) {
				TextView quickShareInfo = findViewById(R.id.quickShareInfo);
				quickShareInfo.setVisibility(View.VISIBLE);
				quickShareInfo.setText(String.format(getString(R.string.quickShareInfoActivity), quickShareFilename));
			}
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(ClientActionEvent event) {
		String clientAction = ClientActionActivity.format(event);

		clientActionView2.setVisibility(View.VISIBLE);
		clientActionView3.setVisibility(View.VISIBLE);

		clientActionView1.setText(clientActionView2.getText());
		clientActionView2.setText(clientActionView3.getText());
		clientActionView3.setText(clientAction);
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

		// if running, query server info
		if (Boolean.TRUE.equals(running)) {
			logger.debug("posting ServerInfoRequestEvent");
			EventBus.getDefault().post(new ServerInfoRequestEvent());
		} else {
			findViewById(R.id.quickShareInfo).setVisibility(View.GONE);
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
		logger.debug("updateButtonStates()");

		boolean atLeastOneRunning;
		if (running == null) {
			checkServicesRunning();
			atLeastOneRunning = serversRunning.atLeastOneRunning();
		} else {
			atLeastOneRunning = running.booleanValue();
		}

		// update fallback buttons
		View fallbackButtonStart = findViewById(R.id.fallbackButtonStartServer);
		if (fallbackButtonStart != null) {
			fallbackButtonStart.setVisibility(atLeastOneRunning ? View.GONE : View.VISIBLE);
		}
		View fallbackButtonStop = findViewById(R.id.fallbackButtonStopServer);
		if (fallbackButtonStop != null) {
			fallbackButtonStop.setVisibility(atLeastOneRunning ? View.VISIBLE : View.GONE);
		}

		// remove status bar notification if server not running
		if (!atLeastOneRunning) {
			NotificationUtil.removeStatusbarNotification(this);
		}

		// action bar icons
		if (startIcon == null || stopIcon == null) {
			return;
		}

		startIcon.setVisible(!atLeastOneRunning);
		stopIcon.setVisible(atLeastOneRunning);
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
		logger.debug("onOptionsItemSelected()");
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
		case R.id.menu_client_action:
			handleClientAction();
			break;
		case R.id.menu_translate:
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://pftpd.rocks/projects/pftpd/pftpd/"));
			startActivity(intent);
			break;
		case R.id.menu_about:
			handleAbout();
			break;
		case R.id.menu_exit:
			handleExit();
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	public void handleStart() {
		logger.trace("handleStart()");
		if (hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)) {
			ServicesStartStopUtil.startServers(this, prefsBean, keyFingerprintProvider, this);
		}
	}

	/**
	 * Checks whether the app has the following permission.
	 * @param permission The permission name
	 * @param requestCode The request code to check against in the {@link #onRequestPermissionsResult} callback.
	 * @return true if permission has been granted.
	 */
	protected boolean hasPermission(String permission, int requestCode) {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			logger.trace("hasPermission()");
			if (prefsBean.getStorageType() == StorageType.PLAIN) {
				if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
					requestPermissions(new String[]{permission}, requestCode);
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		logger.trace("onRequestPermissionsResult()");
		boolean granted = grantResults.length > 0
				&& grantResults[0] == PackageManager.PERMISSION_GRANTED;
		switch (requestCode) {
			case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
				// If request is cancelled, the result arrays are empty.
				if (granted) {
					ServicesStartStopUtil.startServers(this, prefsBean, keyFingerprintProvider, this);
				} else {
					String textPara = getString(R.string.permissionNameStorage);
					Toast.makeText(this, getString(R.string.permissionRequired, textPara), Toast.LENGTH_LONG).show();
				}
			}
			case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_LOGGING: {
				if (granted) {
					SharedPreferences prefs = LoadPrefsUtil.getPrefs(getBaseContext());
					handleLoggingPref(prefs, true);
					this.logger = LoggerFactory.getLogger(getClass());
				} else {
					SharedPreferences prefs = LoadPrefsUtil.getPrefs(getBaseContext());
					LoadPrefsUtil.storeLogging(prefs, Logging.NONE);
				}
			}
		}
	}

	public boolean isKeyPresent() {
		if (!keyFingerprintProvider.areFingerprintsGenerated()) {
			logger.debug("checking if key is present, but fingerprints have not been generated yet");
			keyFingerprintProvider.calcPubkeyFingerprints(this);
		}
		boolean keyPresent = keyFingerprintProvider.isKeyPresent();
		logger.trace("isKeyPresent() -> {}", keyPresent);
		return keyPresent;
	}

	public void showGenKeyDialog() {
		logger.trace("showGenKeyDialog()");
		GenKeysAskDialogFragment askDiag = new GenKeysAskDialogFragment();
		Bundle diagArgs = new Bundle();
		diagArgs.putBoolean(GenKeysAskDialogFragment.KEY_START_SERVER, true);
		askDiag.setArguments(diagArgs);
		askDiag.show(getSupportFragmentManager(), DIALOG_TAG);
	}

	protected void handleStop() {
		logger.trace("handleStop()");
		ServicesStartStopUtil.stopServers(this);
	}

	protected void handlePrefs() {
		logger.trace("handlePrefs()");
		Class<?> prefsActivityClass = theme == Theme.DARK
			? FtpPrefsActivityThemeDark.class
			: FtpPrefsActivityThemeLight.class;
		Intent intent = new Intent(this, prefsActivityClass);
		startActivity(intent);
	}

	protected void handleClientAction() {
		logger.trace("handleClientAction()");
		Intent intent = new Intent(this, ClientActionActivity.class);
		startActivity(intent);
	}

	protected void handleAbout() {
		logger.trace("handleAbout()");
		Intent intent = new Intent(this, AboutActivity.class);
		startActivity(intent);
	}

	protected void handleExit() {
		logger.trace("handleExit()");
		android.os.Process.killProcess(android.os.Process.myPid());
		System.exit(1);
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
		handleLoggingPref(prefs, false);
	}
	protected void handleLoggingPref(SharedPreferences prefs, boolean permissionJustGranted) {
		String loggingStr = prefs.getString(
			LoadPrefsUtil.PREF_KEY_LOGGING,
			Logging.NONE.xmlValue());
		Logging logging = Logging.byXmlVal(loggingStr);
		// one could argue if this makes sense :)
		logger.debug("got 'logging': {}", logging);

		boolean recreateLogger = true;
		// request storage permission if necessary for logging
		if (!permissionJustGranted && logging == Logging.TEXT) {
			recreateLogger = hasPermission(
					Manifest.permission.WRITE_EXTERNAL_STORAGE,
					PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_LOGGING);
		}

		if (recreateLogger) {
			// re-create own log, don't care about other classes
			PrimFtpdLoggerBinder.setLoggingPref(logging);
			this.logger = LoggerFactory.getLogger(getClass());
		}
	}
}
