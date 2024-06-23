package org.primftpd.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.primftpd.R;
import org.primftpd.crypto.HostKeyAlgorithm;
import org.primftpd.events.ClientActionEvent;
import org.primftpd.events.ServerInfoRequestEvent;
import org.primftpd.events.ServerInfoResponseEvent;
import org.primftpd.events.ServerStateChangedEvent;
import org.primftpd.prefs.LoadPrefsUtil;
import org.primftpd.prefs.PrefsBean;
import org.primftpd.prefs.StorageType;
import org.primftpd.util.IpAddressProvider;
import org.primftpd.util.KeyFingerprintBean;
import org.primftpd.util.KeyFingerprintProvider;
import org.primftpd.util.PrngFixes;
import org.primftpd.util.SampleAuthKeysFileCreator;
import org.primftpd.util.ServersRunningBean;
import org.primftpd.util.ServicesStartStopUtil;
import org.primftpd.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public class PftpdFragment extends Fragment implements RecreateLogger, RadioGroup.OnCheckedChangeListener {

	private final BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {
		@Override
	 	public void onReceive(Context context, Intent intent) {
		logger.debug("network connectivity changed, data str: '{}', action: '{}'",
			intent.getDataString(),
			intent.getAction());
		showAddresses();
		}
	};

	private static final int REQUEST_CODE_SAF_PERM = 1234;

	public static final String DIALOG_TAG = "dialogs";

	protected Logger logger = LoggerFactory.getLogger(getClass());

	private PrefsBean prefsBean;
	private final IpAddressProvider ipAddressProvider = new IpAddressProvider();
	private final KeyFingerprintProvider keyFingerprintProvider = new KeyFingerprintProvider();
	private ServersRunningBean serversRunning;
	private long timestampOfLastEvent = 0;

	private TextView clientActionView1;
	private TextView clientActionView2;
	private TextView clientActionView3;

	private boolean onStartOngoing = false;

	protected int getLayoutId() {
		return R.layout.main;
	}

	/** Called when the activity is first created. */
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// basic setup
		super.onCreateView(inflater, container, savedInstanceState);

		logger.debug("onCreateView()");

		// fixes/workarounds for android security issue below 4.3 regarding key generation
		PrngFixes.apply();

		// layout
		View view = inflater.inflate(getLayoutId(), container, false);

		// calc keys fingerprints
		AsyncTask<Void, Void, Void> task = new CalcPubkeyFinterprintsTask(keyFingerprintProvider, this);
		task.execute();

		// create addresses label
		((TextView) view.findViewById(R.id.addressesLabel)).setText(
				String.format("%s (%s)", getText(R.string.ipAddrLabel), getText(R.string.ifacesLabel))
		);

		// create ports label
		((TextView) view.findViewById(R.id.portsLabel)).setText(
				String.format("%s / %s / %s",
						getText(R.string.protocolLabel), getText(R.string.portLabel), getText(R.string.state))
		);

		// listen for events
		EventBus.getDefault().register(this);

		// hide SAF storage type radios and texts for old androids
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			View radioStorageSaf = view.findViewById(R.id.radioStorageSaf);
			((ViewManager)radioStorageSaf.getParent()).removeView(radioStorageSaf);

			View radioStorageRoSaf = view.findViewById(R.id.radioStorageRoSaf);
			((ViewManager)radioStorageRoSaf.getParent()).removeView(radioStorageRoSaf);

			View safExplainHeading = view.findViewById(R.id.safExplainHeading);
			((ViewManager)safExplainHeading.getParent()).removeView(safExplainHeading);

			View safExplain = view.findViewById(R.id.safExplain);
			((ViewManager)safExplain.getParent()).removeView(safExplain);
		}

		// start on open ?
		SharedPreferences prefs = LoadPrefsUtil.getPrefs(getContext());
		Boolean startOnOpen = LoadPrefsUtil.startOnOpen(prefs);
		if (startOnOpen) {
			keyFingerprintProvider.calcPubkeyFingerprints(getContext()); // see GH issue #204
			ServicesStartStopUtil.startServers(this);
		}

		// init client action views
		clientActionView1 = view.findViewById(R.id.clientActionsLine1);
		clientActionView2 = view.findViewById(R.id.clientActionsLine2);
		clientActionView3 = view.findViewById(R.id.clientActionsLine3);

		// make links clickable
		((TextView)view.findViewById(R.id.radioStoragePlain)).setMovementMethod(LinkMovementMethod.getInstance());
		((TextView)view.findViewById(R.id.safExplain)).setMovementMethod(LinkMovementMethod.getInstance());

		// create sample authorized_keys files
		new SampleAuthKeysFileCreator().createSampleAuthorizedKeysFiles(getContext());

		return view;
	}

	@Override
	public void onDestroyView()
	{
		super.onDestroyView();

		// server state change events
		EventBus.getDefault().unregister(this);
	}

    @Override
    public void onStart() {
        super.onStart();

        logger.debug("onStart()");
		onStartOngoing = true;

        loadPrefs();
        showLogindata();

        // init storage type radio
        View view = getView();
        if (view == null) {
            return;
        }
        ((RadioGroup)view.findViewById(R.id.radioGroupStorage)).setOnCheckedChangeListener(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            switch (prefsBean.getStorageType()) {
                case PLAIN:
                    ((RadioButton) view.findViewById(R.id.radioStoragePlain)).setChecked(true);
                    break;
                case ROOT:
                    ((RadioButton) view.findViewById(R.id.radioStorageRoot)).setChecked(true);
                    break;
                case SAF:
                    ((RadioButton) view.findViewById(R.id.radioStorageSaf)).setChecked(true);
                    showSafUrl(prefsBean.getSafUrl());
                    break;
                case RO_SAF:
                    ((RadioButton) view.findViewById(R.id.radioStorageRoSaf)).setChecked(true);
                    showSafUrl(prefsBean.getSafUrl());
                    break;
                case VIRTUAL:
                    ((RadioButton) view.findViewById(R.id.radioStorageVirtual)).setChecked(true);
                    showSafUrl(prefsBean.getSafUrl());
                    break;
            }
        } else {
            switch (prefsBean.getStorageType()) {
                case PLAIN:
                    ((RadioButton) view.findViewById(R.id.radioStoragePlain)).setChecked(true);
                    break;
                case ROOT:
                    ((RadioButton) view.findViewById(R.id.radioStorageRoot)).setChecked(true);
                    break;
            }
        }

		onStartOngoing = false;
    }

	@Override
	public void onResume() {
		super.onResume();

		logger.debug("onResume()");

		// register listener to reprint interfaces table when network connections change
		// android sends those events when registered in code but not when registered in manifest
		IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		requireActivity().registerReceiver(this.networkStateReceiver, filter);

		// e.g. necessary when ports preferences have been changed
		displayServersState();

		// check if chosen SAF directory can be accessed
		checkSafAccess();

		// validate bind IP
		if (!ipAddressProvider.isIpAvail(prefsBean.getBindIp())) {
			String msg = "IP " + prefsBean.getBindIp() +
					" is currently not assigned to an interface. May lead to a crash.";
			Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		logger.debug("onPause()");

		// unregister broadcast receiver
		requireActivity().unregisterReceiver(this.networkStateReceiver);
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		logger.debug("onCheckedChanged()");
		View view = getView();
		if (view == null) {
			return;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			view.findViewById(R.id.safUriLabel).setVisibility(View.GONE);
			view.findViewById(R.id.safUri).setVisibility(View.GONE);

			StorageType storageType = null;

			Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
			intent.addFlags(
					Intent.FLAG_GRANT_READ_URI_PERMISSION
							| Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

			int checkedRadioButtonId = group.getCheckedRadioButtonId();
			try {
				switch (checkedRadioButtonId) {
					case R.id.radioStoragePlain:
						storageType = StorageType.PLAIN;
						break;
					case R.id.radioStorageRoot:
						storageType = StorageType.ROOT;
						break;
					case R.id.radioStorageSaf:
						storageType = StorageType.SAF;
						if (!onStartOngoing) {
							startActivityForResult(intent, REQUEST_CODE_SAF_PERM);
						}
						break;
					case R.id.radioStorageRoSaf:
						storageType = StorageType.RO_SAF;
						if (!onStartOngoing) {
							startActivityForResult(intent, REQUEST_CODE_SAF_PERM);
						}
						break;
					case R.id.radioStorageVirtual:
						storageType = StorageType.VIRTUAL;
						if (!onStartOngoing) {
							startActivityForResult(intent, REQUEST_CODE_SAF_PERM);
						}
						break;
				}
			} catch (ActivityNotFoundException e) {
				Toast.makeText(getContext(), "SAF seems to be broken on your device :(", Toast.LENGTH_SHORT).show();
				storageType = StorageType.PLAIN;
			}

			if (storageType != null) {
				SharedPreferences prefs = LoadPrefsUtil.getPrefs(getContext());
				LoadPrefsUtil.storeStorageType(prefs, storageType);
			}

			if (storageType == StorageType.PLAIN || storageType == StorageType.ROOT) {
				loadPrefs();
				checkSafAccess();
			}
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
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
					FragmentActivity activity = requireActivity();
					String oldUrl = prefsBean.getSafUrl();
					if (!StringUtils.isBlank(oldUrl)) {
						try {
							activity.getContentResolver().releasePersistableUriPermission(Uri.parse(oldUrl), modeFlags);
						} catch (SecurityException e) {
							logger.info("SecurityException while calling releasePersistableUriPermission()");
							logger.trace("", e);
						}
					}

					// persist permissions
					try {
						activity.grantUriPermission(activity.getPackageName(), uri, modeFlags);
						activity.getContentResolver().takePersistableUriPermission(uri, modeFlags);
					} catch (SecurityException e) {
						logger.info("SecurityException while calling takePersistableUriPermission()");
						logger.trace("", e);
					}

					// store uri
					SharedPreferences prefs = LoadPrefsUtil.getPrefs(getContext());
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
			View view = getView();
			if (view == null) {
				return;
			}
			RadioButton safRadio = view.findViewById(R.id.radioStorageSaf);
			if (prefsBean.getStorageType() == StorageType.SAF || prefsBean.getStorageType() == StorageType.RO_SAF) {
				// let's see if the OS has persisted something for us
				List<UriPermission> persistedUriPermissions = requireActivity().getContentResolver().getPersistedUriPermissions();
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
					cursor = requireActivity().getContentResolver().query(
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
				final boolean darkMode = UiModeUtil.isDarkMode(getResources());
				int icon = darkMode
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
		View view = getView();
		if (view == null) {
			return;
		}
		LinearLayout container = view.findViewById(R.id.addressesContainer);

		// clear old entries
		container.removeAllViews();

		boolean isLeftToRight = isLeftToRight();
		List<String> displayTexts = ipAddressProvider.ipAddressTexts(getContext(), true, isLeftToRight);
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

		View view = getView();
		if (view == null) {
			return;
		}
		if (isLeftToRight) {
			((TextView) view.findViewById(R.id.ftpTextView))
					.setText("ftp / " + prefsBean.getPortStr() + " / " +
							getText(serversRunning.ftp
									? R.string.serverStarted
									: R.string.serverStopped));

			((TextView) view.findViewById(R.id.sftpTextView))
					.setText("sftp / " + prefsBean.getSecurePortStr() + " / " +
							getText(serversRunning.ssh
									? R.string.serverStarted
									: R.string.serverStopped));
		} else {
			((TextView) view.findViewById(R.id.ftpTextView))
					.setText(prefsBean.getPortStr() + " / " +
							getText(serversRunning.ftp
									? R.string.serverStarted
									: R.string.serverStopped)
					+ " / " + "ftp");

			((TextView) view.findViewById(R.id.sftpTextView))
					.setText(prefsBean.getSecurePortStr() + " / " +
							getText(serversRunning.ssh
									? R.string.serverStarted
									: R.string.serverStopped)
					+ " / " + "sftp");
		}
	}

	protected void showLogindata() {
		View view = getView();
		if (view == null) {
			return;
		}

		TextView usernameView = view.findViewById(R.id.usernameTextView);
		usernameView.setText(prefsBean.getUserName());

		TextView anonymousView = view.findViewById(R.id.anonymousLoginTextView);
		anonymousView.setText(getString(R.string.isAnonymous, prefsBean.isAnonymousLogin()));

		TextView passwordPresentView = view.findViewById(R.id.passwordPresentTextView);
		passwordPresentView.setText(getString(R.string.passwordPresent,
				StringUtils.isNotEmpty(prefsBean.getPassword())));

		TextView pubKeyAuthView = view.findViewById(R.id.pubKeyAuthTextView);
		pubKeyAuthView.setText(getString(R.string.pubKeyAuth, prefsBean.isPubKeyAuth()));

		displayNormalStorageAccess();
		displayFullStorageAccess();
		displayMediaLocationAccess();
		displayNotificationPermission();
	}

	private final ActivityResultLauncher<String[]> permissionRequestLauncher = registerForActivityResult(
			new ActivityResultContracts.RequestMultiplePermissions(),
				isGranted -> showLogindata());

	private void displayNormalStorageAccess() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
			displayPermission(
					R.id.hasNormalStorageAccessTextView,
					R.string.hasNormalAccessToStorage,
					Manifest.permission.WRITE_EXTERNAL_STORAGE);
		}
	}

	private void displayFullStorageAccess() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			View view = getView();
			if (view == null) {
				return;
			}
			TextView hasFullStorageAccessTextView = view.findViewById(R.id.hasFullStorageAccessTextView);
			boolean hasFullStorageAccess = Environment.isExternalStorageManager();
			String hasStorageAccessStr = getString(R.string.hasFullAccessToStorage, hasFullStorageAccess);

			if (!hasFullStorageAccess) {
				buildPermissionRequestLink(hasFullStorageAccessTextView, hasStorageAccessStr, v -> {
					Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
					Uri uri = Uri.fromParts("package", requireActivity().getPackageName(), null);
					intent.setData(uri);
					startActivity(intent);
				});
			} else {
				hasFullStorageAccessTextView.setText(hasStorageAccessStr);
			}
		}
	}
	private void displayMediaLocationAccess() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			displayPermission(
					R.id.hasMediaLocationAccessTextView,
					R.string.hasAccessToMediaLocation,
					Manifest.permission.ACCESS_MEDIA_LOCATION);
		}
	}

	private void displayNotificationPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			displayPermission(
					R.id.hasNotificationPermissionTextView,
					R.string.hasNotificationPermission,
					Manifest.permission.POST_NOTIFICATIONS);
		}
	}

	private void displayPermission(int textViewId, int textId, String permission) {
		View view = getView();
		if (view == null) {
			return;
		}
		boolean hasPermission = hasPermission(permission);
		TextView textView = view.findViewById(textViewId);
		String hasPermissionStr = getString(textId, hasPermission);
		if (!hasPermission) {
			buildPermissionRequestLink(
					textView,
					hasPermissionStr,
					v -> permissionRequestLauncher.launch(new String[] {permission}));
		} else {
			textView.setText(hasPermissionStr);
		}
	}
	protected void buildPermissionRequestLink(
			TextView textView,
			String baseText,
			View.OnClickListener onClickListener) {
		String request = getString(R.string.Request);
		String completeText = baseText + " " + request;
		SpannableString spannable = new SpannableString(completeText);
		spannable.setSpan(new UnderlineSpan(), baseText.length() + 1, completeText.length(), 0);
		textView.setText(spannable);
		textView.setOnClickListener(onClickListener);
	}

	protected boolean hasPermission(String permission) {
		Context context = getContext();
		if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			logger.trace("hasPermission({})", permission);
			return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
		}
		return true;
	}

	protected void showSafUrl(String url) {
		View view = getView();
		if (view == null) {
			return;
		}
		view.findViewById(R.id.safUriLabel).setVisibility(View.VISIBLE);
		TextView safUriView = view.findViewById(R.id.safUri);
		safUriView.setVisibility(View.VISIBLE);
		safUriView.setText(url);
	}

	@SuppressLint("SetTextI18n")
	public void showKeyFingerprints() {
		View view = getView();
		if (view == null) {
			return;
		}
		// find algo to show
		HostKeyAlgorithm chosenAlgo = keyFingerprintProvider.findPreferredHostKeyAlog(this.getContext());

		// show info about chosen algo and it's key
		((TextView)view.findViewById(R.id.keyFingerprintMd5Label))
				.setText("MD5 (" + chosenAlgo.getDisplayName() + ")");
		((TextView)view.findViewById(R.id.keyFingerprintSha1Label))
				.setText("SHA1 (" + chosenAlgo.getDisplayName() + ")");
		((TextView)view.findViewById(R.id.keyFingerprintSha256Label))
				.setText("SHA256 (" + chosenAlgo.getDisplayName() + ")");

		KeyFingerprintBean keyFingerprintBean = keyFingerprintProvider.getFingerprints().get(chosenAlgo);

		if (keyFingerprintBean != null) {
			((TextView) view.findViewById(R.id.keyFingerprintMd5TextView))
					.setText(keyFingerprintBean.getFingerprintMd5());
			((TextView) view.findViewById(R.id.keyFingerprintSha1TextView))
					.setText(keyFingerprintBean.getFingerprintSha1());
			((TextView) view.findViewById(R.id.keyFingerprintSha256TextView))
					.setText(keyFingerprintBean.getFingerprintSha256());
		}

		// create onRefreshListener
		PftpdFragment pftpdFragment = this;
		View refreshButton = view.findViewById(R.id.keyFingerprintsLabel);
		refreshButton.setOnClickListener(v -> {
			logger.trace("refreshButton OnClickListener");
			GenKeysAskDialogFragment askDiag = new GenKeysAskDialogFragment(pftpdFragment);
			askDiag.show(requireActivity().getSupportFragmentManager(), DIALOG_TAG);
		});

		// link to keys fingerprints activity
		TextView showAllKeysFingerprints = view.findViewById(R.id.allKeysFingerprintsLabel);
		CharSequence text = showAllKeysFingerprints.getText();
		SpannableString spannable = new SpannableString(text);
		spannable.setSpan(new UnderlineSpan(), 0, text.length(), 0);
		showAllKeysFingerprints.setText(spannable);
		showAllKeysFingerprints.setOnClickListener(v -> {
			TabLayout tabLayout = view.getRootView().findViewById(R.id.tabs);
			TabLayout.Tab tab = tabLayout.getTabAt(MainTabsActivity.INDEX_FINGERPRINTS);
			if (tab != null) {
				tab.select();
			}
		});
	}

	private boolean isEventInTime(Object event) {
		long currentTime = System.currentTimeMillis();
		long offset = currentTime - timestampOfLastEvent;
		boolean inTime = offset > 20;
		if (inTime) {
			logger.debug("handling event '{}', offset: {} ms", event.getClass().getName(), offset);
			timestampOfLastEvent = currentTime;
		} else {
			logger.debug("ignoring event '{}', offset: {} ms", event.getClass().getName(), offset);
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
		int numberOfFiles = event.getQuickShareNumberOfFiles();
		logger.debug("got ServerInfoResponseEvent, QuickShare numberOfFiles: {}", numberOfFiles);
		if (isEventInTime(event)) {
			if (numberOfFiles >= 0) {
				View view = getView();
				if (view == null) {
					return;
				}
				TextView quickShareInfo = view.findViewById(R.id.quickShareInfo);
				quickShareInfo.setVisibility(View.VISIBLE);
				quickShareInfo.setText(String.format(getString(R.string.quickShareInfoActivityV2), numberOfFiles));
			}
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(ClientActionEvent event) {
		String clientAction = ClientActionFragment.format(event);

		clientActionView2.setVisibility(View.VISIBLE);
		clientActionView3.setVisibility(View.VISIBLE);

		clientActionView1.setText(clientActionView2.getText());
		clientActionView2.setText(clientActionView3.getText());
		clientActionView3.setText(clientAction);
	}

	protected void displayServersState() {
		logger.debug("displayServersState()");

		checkServicesRunning();
		Boolean running = null;
		if (serversRunning != null) {
			running = serversRunning.atLeastOneRunning();
		}

		// should be triggered by onCreateOptionsMenu() to avoid icon flicker
		// when invoked via notification
		updateFallbackButtonStates(running);

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
			View view = getView();
			if (view != null) {
				view.findViewById(R.id.quickShareInfo).setVisibility(View.GONE);
			}
		}
	}

	protected void checkServicesRunning() {
		logger.debug("checkServicesRunning()");
		Context context = getContext();
		if (context != null) {
			this.serversRunning = ServicesStartStopUtil.checkServicesRunning(context);
		}
	}

	protected void updateFallbackButtonStates(Boolean running) {
		logger.debug("updateButtonStates()");

		boolean atLeastOneRunning;
		if (running == null) {
			checkServicesRunning();
			atLeastOneRunning = serversRunning.atLeastOneRunning();
		} else {
			atLeastOneRunning = running;
		}

		// update fallback buttons
		View view = getView();
		if (view != null) {
			Button fallbackButtonToggle = view.findViewById(R.id.fallbackButtonToggleServer);
			if (fallbackButtonToggle != null) {
				if (atLeastOneRunning) {
					fallbackButtonToggle.setText(R.string.stopService);
				} else {
					fallbackButtonToggle.setText(R.string.startService);
				}
			}
		}
	}

	protected void loadPrefs() {
		logger.debug("loadPrefs()");

		SharedPreferences prefs = LoadPrefsUtil.getPrefs(getContext());
		this.prefsBean = LoadPrefsUtil.loadPrefs(logger, prefs);
	}

	public PrefsBean getPrefsBean() {
		return prefsBean;
	}

	public KeyFingerprintProvider getKeyFingerprintProvider() {
		return keyFingerprintProvider;
	}

	@Override
	public void recreateLogger() {
		this.logger = LoggerFactory.getLogger(getClass());
	}
}
