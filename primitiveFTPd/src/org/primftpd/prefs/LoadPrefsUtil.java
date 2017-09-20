package org.primftpd.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.primftpd.PrefsBean;
import org.primftpd.util.Defaults;
import org.slf4j.Logger;

import java.io.File;

public class LoadPrefsUtil
{
	public static final String PREF_KEY_USER = "userNamePref";
	public static final String PREF_KEY_PASSWORD = "passwordPref";
	public static final String PREF_ANONYMOUS_LOGIN = "anonymousLoginPref";
	public static final String PREF_KEY_PORT = "portPref";
	public static final String PREF_KEY_SECURE_PORT = "securePortPref";
	public static final String PREF_KEY_START_DIR = "startDirPref";
	public static final String PREF_KEY_ANNOUNCE = "announcePref";
	public static final String PREF_KEY_ANNOUNCE_NAME = "announceNamePref";
	public static final String PREF_KEY_WAKELOCK = "wakelockPref";
	public static final String PREF_KEY_WHICH_SERVER = "whichServerToStartPref";
	public static final String PREF_KEY_START_ON_BOOT = "startOnBootPref";
	public static final String PREF_KEY_PUB_KEY_AUTH = "pubKeyAuthPref";
	public static final String PREF_KEY_FOREGROUND_SERVICE = "foregroundServicePref";
	public static final String PREF_KEY_THEME = "themePref";
	public static final String PREF_KEY_LOGGING = "loggingPref";
	public static final String PREF_KEY_FTP_PASSIVE_PORTS = "ftpPassivePortsPref";
	public static final String PREF_KEY_STORAGE_TYPE = "storageTypePref";
	public static final String PREF_KEY_SAF_URL = "safUrlPref";

	public static final int PORT_DEFAULT_VAL = 12345;
	static final String PORT_DEFAULT_VAL_STR = String.valueOf(PORT_DEFAULT_VAL);
	public static final int SECURE_PORT_DEFAULT_VAL = 1234;
	static final String SECURE_PORT_DEFAULT_VAL_STR =
		String.valueOf(SECURE_PORT_DEFAULT_VAL);

	/**
	 * @return Android {@link SharedPreferences} object.
	 */
	public static SharedPreferences getPrefs(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context);
	}

	public static Boolean anonymousLogin(SharedPreferences prefs) {
		return prefs.getBoolean(
			LoadPrefsUtil.PREF_ANONYMOUS_LOGIN,
			Boolean.FALSE);
	}

	public static String userName(SharedPreferences prefs) {
		return prefs.getString(
			LoadPrefsUtil.PREF_KEY_USER,
			"user");
	}

	public static String password(SharedPreferences prefs) {
		return prefs.getString(
			LoadPrefsUtil.PREF_KEY_PASSWORD,
			null);
	}

	public static File startDir(SharedPreferences prefs) {
		String prefStr = prefs.getString(
			LoadPrefsUtil.PREF_KEY_START_DIR,
			null);
		return prefStr != null ? new File(prefStr) : Defaults.HOME_DIR;
	}

	public static Boolean announce(SharedPreferences prefs) {
		// default to false as it may cause crashes
		return prefs.getBoolean(
			LoadPrefsUtil.PREF_KEY_ANNOUNCE,
			Boolean.FALSE);
	}

	public static String announceName(SharedPreferences prefs) {
		return prefs.getString(
			LoadPrefsUtil.PREF_KEY_ANNOUNCE_NAME,
			"primitive ftpd");
	}

	public static Boolean wakelock(SharedPreferences prefs) {
		return prefs.getBoolean(
			LoadPrefsUtil.PREF_KEY_WAKELOCK,
			Boolean.TRUE);
	}

	public static Boolean startOnBoot(SharedPreferences prefs) {
		return prefs.getBoolean(
			LoadPrefsUtil.PREF_KEY_START_ON_BOOT,
			Boolean.FALSE);
	}

	public static Boolean pubKeyAuth(SharedPreferences prefs) {
		return prefs.getBoolean(
			LoadPrefsUtil.PREF_KEY_PUB_KEY_AUTH,
			Boolean.FALSE);
	}

	public static Boolean foregroundService(SharedPreferences prefs) {
		return prefs.getBoolean(
				LoadPrefsUtil.PREF_KEY_FOREGROUND_SERVICE,
				Boolean.FALSE);
	}

	public static ServerToStart serverToStart(SharedPreferences prefs) {
		String whichServerStr = prefs.getString(
			LoadPrefsUtil.PREF_KEY_WHICH_SERVER,
			ServerToStart.ALL.xmlValue());
		return ServerToStart.byXmlVal(whichServerStr);
	}

	public static Theme theme(SharedPreferences prefs) {
		String themeStr = prefs.getString(
			PREF_KEY_THEME,
			Theme.DARK.xmlValue());
		return Theme.byXmlVal(themeStr);
	}

	public static String ftpPassivePorts(SharedPreferences prefs) {
		return prefs.getString(
				LoadPrefsUtil.PREF_KEY_FTP_PASSIVE_PORTS,
				null);
	}

	public static StorageType storageType(SharedPreferences prefs) {
		String storageTypeStr = prefs.getString(
				PREF_KEY_STORAGE_TYPE,
				StorageType.PLAIN.xmlValue());
		return StorageType.byXmlVal(storageTypeStr);
	}

	public static void storeStorageType(SharedPreferences prefs, StorageType value) {
		prefs.edit().putString(PREF_KEY_STORAGE_TYPE, value.xmlValue()).commit();
	}

	public static String safUrl(SharedPreferences prefs) {
		return prefs.getString(
				LoadPrefsUtil.PREF_KEY_SAF_URL,
				"");
	}

	public static void storeSafUrl(SharedPreferences prefs, String value) {
		prefs.edit().putString(PREF_KEY_SAF_URL, value).commit();
	}

	public static int loadPortInsecure(
		Logger logger,
		SharedPreferences prefs)
	{
		return loadPort(
				logger,
				prefs,
				PREF_KEY_PORT,
				PORT_DEFAULT_VAL,
				PORT_DEFAULT_VAL_STR);
	}

	public static int loadPortSecure(
		Logger logger,
		SharedPreferences prefs)
	{
		return loadPort(
				logger,
				prefs,
				PREF_KEY_SECURE_PORT,
				SECURE_PORT_DEFAULT_VAL,
				SECURE_PORT_DEFAULT_VAL_STR);
	}

	static int loadPort(
		Logger logger,
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

		return port;
	}

	/**
	 * @param port Port to validate
	 * @return True if port is valid, false if invalid.
	 */
	static boolean validatePort(int port) {
		return port > 1024 && port <= 64000;
	}

	public static PrefsBean loadPrefs(Logger logger, SharedPreferences prefs) {
		boolean anonymousLogin = anonymousLogin(prefs);
		logger.debug("got anonymousLogin: {}", Boolean.valueOf(anonymousLogin));

		String userName = userName(prefs);
		logger.debug("got userName: {}", userName);

		String password = password(prefs);
		logger.debug("got password: {}", password);

		File startDir = startDir(prefs);
		logger.debug("got startDir: {}", startDir);

		boolean announce = announce(prefs);
		logger.debug("got announce: {}", Boolean.valueOf(announce));

		String announceName = announceName(prefs);
		logger.debug("got announceName: {}", announceName);

		boolean wakelock = wakelock(prefs);
		logger.debug("got wakelock: {}", Boolean.valueOf(wakelock));

		boolean pubKeyAuth = pubKeyAuth(prefs);
		logger.debug("got pubKeyAuth: {}", Boolean.valueOf(pubKeyAuth));

		boolean foregroundService = foregroundService(prefs);
		logger.debug("got foregroundService: {}", Boolean.valueOf(foregroundService));

		ServerToStart serverToStart = serverToStart(prefs);
		logger.debug("got 'which server': {}", serverToStart);

		String ftpPassivePorts = ftpPassivePorts(prefs);
		logger.debug("got ftpPassivePorts: {}", ftpPassivePorts);

		int port = loadPortInsecure(logger, prefs);
		logger.debug("got 'port': {}", Integer.valueOf(port));

		int securePort = loadPortSecure(logger, prefs);
		logger.debug("got 'secure port': {}", Integer.valueOf(securePort));

		StorageType storageType = storageType(prefs);
		logger.debug("got 'StorageType': {}", storageType);

		String safUrl = safUrl(prefs);
		logger.debug("got safUrl: {}", safUrl);

		// create prefsBean
		return new PrefsBean(
				userName,
				password,
				anonymousLogin,
				port,
				securePort,
				startDir,
				announce,
				announceName,
				wakelock,
				pubKeyAuth,
				foregroundService,
				serverToStart,
				ftpPassivePorts,
				storageType,
				safUrl);
	}
}
