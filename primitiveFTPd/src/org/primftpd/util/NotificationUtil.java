package org.primftpd.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.view.View;

import org.primftpd.PrimitiveFtpdActivity;
import org.primftpd.R;
import org.primftpd.StartStopWidgetProvider;
import org.primftpd.prefs.LoadPrefsUtil;
import org.primftpd.prefs.PrefsBean;
import org.primftpd.services.DownloadsService;
import org.primftpd.share.QuickShareBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class NotificationUtil
{
	public static final String NOTIFICATION_CHANNEL_ID = "pftpd running";
	public static final String START_STOP_CHANNEL_ID = "pftpd start/stop";
	public static final String DOWNLOAD_CHANNEL_ID = "pftpd download";

	private static final Logger LOGGER = LoggerFactory.getLogger(NotificationUtil.class);

	public static final int NOTIFICATION_ID = 1;
	public static final int START_STOP_ID = 2;
	public static final int DOWNLOAD_ID = 3;

	public static void createStatusbarNotification(
			Context ctxt,
			Notification notification,
			int id)
	{
		NotificationManager notiMgr = (NotificationManager) ctxt.getSystemService(
			Context.NOTIFICATION_SERVICE);
		notiMgr.notify(id, notification);
	}

	public static void removeStatusbarNotification(Context ctxt) {
		NotificationManager notiMgr = (NotificationManager) ctxt.getSystemService(
			Context.NOTIFICATION_SERVICE);
		notiMgr.cancel(NOTIFICATION_ID);
	}

	public static void removeStartStopNotification(Context ctxt) {
		NotificationManager notiMgr = (NotificationManager) ctxt.getSystemService(
				Context.NOTIFICATION_SERVICE);
		notiMgr.cancel(START_STOP_ID);
	}

	public static void removeDownloadNotification(Context ctxt) {
		NotificationManager notiMgr = (NotificationManager) ctxt.getSystemService(
				Context.NOTIFICATION_SERVICE);
		notiMgr.cancel(DOWNLOAD_ID);
	}

	private static Notification.Builder createStubNotification(
			Context ctxt,
			int text,
			int actionText,
			String channelId,
			QuickShareBean quickShareBean) {
		// create pending intent
		Intent notificationIntent = new Intent(ctxt, PrimitiveFtpdActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(ctxt, 0, notificationIntent, 0);

		Intent stopIntent = StartStopWidgetProvider.buildServerStartStopIntent(ctxt);
		PendingIntent pendingStopIntent = PendingIntent.getBroadcast(ctxt, 0, stopIntent, 0);

		// create channel
		createChannel(ctxt, channelId);

		// create notification
		int iconId = R.drawable.ic_notification;
		int stopIconId = R.drawable.ic_stop_white_24dp;
		CharSequence tickerText = ctxt.getText(text);
		CharSequence contentTitle = quickShareBean != null
			? String.format(ctxt.getResources().getString(R.string.quickShareInfoNotification), quickShareBean.filename())
			: ctxt.getText(R.string.notificationTitle);
		CharSequence contentText = tickerText;

		// use main icon as large one
		Bitmap largeIcon = BitmapFactory.decodeResource(
				ctxt.getResources(),
				R.drawable.ic_launcher);

		long when = System.currentTimeMillis();

		Notification.Builder builder = new Notification.Builder(ctxt)
				.setTicker(tickerText)
				.setContentTitle(contentTitle)
				.setContentText(contentText)
				.setSmallIcon(iconId)
				.setLargeIcon(largeIcon)
				.setContentIntent(contentIntent)
				.setWhen(when);
		addChannel(builder, channelId);

		// notification action
		addAction(ctxt, builder, pendingStopIntent, actionText, stopIconId);
		return builder;
	}

	private static void createChannel(Context ctxt, String channelId) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(
					channelId,
					channelId,
					NotificationManager.IMPORTANCE_LOW);
			NotificationManager notificationManager = ctxt.getSystemService(NotificationManager.class);
			notificationManager.createNotificationChannel(channel);
		}
	}

	private static void addChannel(Notification.Builder builder, String channelId) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			builder.setChannelId(channelId);
		}
	}

	private static void addAction(
			Context ctxt,
			Notification.Builder builder,
			PendingIntent pendingIntent,
			int text,
			int iconId) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			// TODO check icon for android 7
			Icon icon = Icon.createWithResource(ctxt, iconId);
			Notification.Action stopAction = new Notification.Action.Builder(
					icon,
					ctxt.getString(text),
					pendingIntent).build();
			builder.addAction(stopAction);
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			builder.addAction(
					iconId,
					ctxt.getString(text),
					pendingIntent);
		}
	}

	public static void createStartStopNotification(Context ctxt) {
		LOGGER.debug("createStartStopNotification()");

		Notification.Builder builder = createStubNotification(
				ctxt,
				R.string.toggleService,
				R.string.toggleService,
				START_STOP_CHANNEL_ID,
				null);

		Notification notification = null;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			notification = builder.build();
		} else {
			notification = builder.getNotification();
		}
		notification.flags |= Notification.FLAG_NO_CLEAR;

		createStatusbarNotification(ctxt, notification, START_STOP_ID);
	}

	public static Notification createStatusbarNotification(
            Context ctxt,
            PrefsBean prefsBean,
            KeyFingerprintProvider keyFingerprintProvider,
			QuickShareBean quickShareBean) {
		LOGGER.debug("createStatusbarNotification()");

		Notification.Builder builder = createStubNotification(
				ctxt,
				R.string.serverRunning,
				R.string.stopService,
				NOTIFICATION_CHANNEL_ID,
				quickShareBean);

		Notification notification = null;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			if (prefsBean.showConnectionInfoInNotification()) {
				String longText = buildLongText(ctxt, prefsBean, keyFingerprintProvider);
				builder.setStyle(new Notification.BigTextStyle().bigText(longText));
			}

			notification = builder.build();
		} else {
			notification = builder.getNotification();
		}
		notification.flags |= Notification.FLAG_NO_CLEAR;

		createStatusbarNotification(ctxt, notification, NOTIFICATION_ID);
		return notification;
	}

	private static String buildLongText(
			Context ctxt,
			PrefsBean prefsBean,
			KeyFingerprintProvider keyFingerprintProvider) {
		LOGGER.trace("buildLongText()");

		boolean isLeftToRight = true;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			Configuration config = ctxt.getResources().getConfiguration();
			isLeftToRight = config.getLayoutDirection() == View.LAYOUT_DIRECTION_LTR;
		}

		StringBuilder str = new StringBuilder();
		IpAddressProvider ipAddressProvider = new IpAddressProvider();
		List<String> ipAddressTexts = ipAddressProvider.ipAddressTexts(ctxt, false, isLeftToRight);

		SharedPreferences prefs = LoadPrefsUtil.getPrefs(ctxt);
		Boolean showIpv4 = LoadPrefsUtil.showIpv4InNotification(prefs);
		Boolean showIpv6 = LoadPrefsUtil.showIpv6InNotification(prefs);

		for (String ipAddressText : ipAddressTexts) {
			boolean ipv6 = ipAddressProvider.isIpv6(ipAddressText);
			if (!ipv6 && !showIpv4) {
				continue;
			}
			if (ipv6 && !showIpv6) {
				continue;
			}

			if (prefsBean.getServerToStart().startFtp()) {
				str.append("ftp://");
				if (ipv6) {
					str.append("[");
				}
				str.append(ipAddressText);
				if (ipv6) {
					str.append("]");
				}
				str.append(":");
				str.append(prefsBean.getPortStr());
				str.append("\n");
			}
			if (prefsBean.getServerToStart().startSftp()) {
				str.append("sftp://");
				if (ipv6) {
					str.append("[");
				}
				str.append(ipAddressText);
				if (ipv6) {
					str.append("]");
				}
				str.append(":");
				str.append(prefsBean.getSecurePortStr());
				str.append("\n");
			}
		}

		if (prefsBean.getServerToStart().startSftp()) {
			if (!keyFingerprintProvider.areFingerprintsGenerated()) {
				keyFingerprintProvider.calcPubkeyFingerprints(ctxt);
			}
			str.append("\n");
			str.append("Key Fingerprints");
			str.append("\n");
			// show md5-bytes and sha256-base64 (no sha1) as that is what clients usually show
			str.append("SHA256: ");
			str.append(keyFingerprintProvider.getBase64Sha256());
			str.append("\n");
			str.append("MD5: ");
			str.append(keyFingerprintProvider.getBytesMd5());
		}

		return str.toString();
	}

	public static Notification createDownloadNotification(
			Context ctxt,
			String filename,
			String path,
			boolean canceled,
			boolean finished,
			long currentBytes,
			long size) {

		// create channel
		createChannel(ctxt, DOWNLOAD_CHANNEL_ID);

		// create notification
		int iconId = R.drawable.outline_cloud_download_black_18;
		int stopIconId = R.drawable.ic_stop_white_24dp;
		CharSequence tickerText;
		if (canceled) {
			tickerText = "canceled";
		} else if (finished) {
			tickerText = "finished";
		} else {
			String sizeStr = size != 0 ? String.valueOf(size) : "unknown";
			StringBuilder builder = new StringBuilder();
			builder.append("downloading ... (");
			builder.append(currentBytes);
			builder.append(" / ");
			builder.append(sizeStr);
			builder.append(")");
			builder.append("\n");
			builder.append("to ");
			builder.append("path");
			tickerText =  builder.toString();
		}
		CharSequence contentTitle = filename;
		CharSequence contentText = tickerText;

		// use main icon as large one
		Bitmap largeIcon = BitmapFactory.decodeResource(
				ctxt.getResources(),
				R.drawable.outline_cloud_download_black_48);

		long when = System.currentTimeMillis();

		Notification.Builder builder = new Notification.Builder(ctxt)
				.setTicker(tickerText)
				.setContentTitle(contentTitle)
				.setContentText(contentText)
				.setSmallIcon(iconId)
				.setLargeIcon(largeIcon)
				.setWhen(when)
				.setProgress((int)size, (int)currentBytes, false);
		addChannel(builder, DOWNLOAD_CHANNEL_ID);

		// intent to open file
		// -> security exception
//		Intent openFileIntent = new Intent(Intent.ACTION_VIEW);
//		openFileIntent.setData(Uri.fromFile(new File(path)));
//		openFileIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//		Intent chooserIntent = Intent.createChooser(openFileIntent, "");
//		PendingIntent pendingChooserIntent = PendingIntent.getBroadcast(ctxt, 0, chooserIntent, 0);

		// intent to stop download
		if (!canceled && !finished) {
			// note: need to route stop intent through WidgetProvider to avoid creating another service instance
			//Intent stopIntent = new Intent(ctxt, DownloadsService.class);
			Intent stopIntent = new Intent(ctxt, StartStopWidgetProvider.class);
			stopIntent.setAction(DownloadsService.ACTION_STOP);
			PendingIntent pendingStopIntent = PendingIntent.getBroadcast(ctxt, 0, stopIntent, 0);

			// notification action
			//addAction(ctxt, builder, pendingChooserIntent, R.string.open, stopIconId);
			addAction(ctxt, builder, pendingStopIntent, R.string.cancel, stopIconId);
		}

		Notification notification;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			notification = builder.build();
		} else {
			notification = builder.getNotification();
		}

		NotificationManager notiMgr = (NotificationManager) ctxt.getSystemService(
				Context.NOTIFICATION_SERVICE);
		notiMgr.notify(DOWNLOAD_ID, notification);
		return notification;
	}
}
