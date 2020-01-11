package org.primftpd.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.os.Build;

import org.primftpd.PrefsBean;
import org.primftpd.PrimitiveFtpdActivity;
import org.primftpd.R;
import org.primftpd.services.ServicesStartingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class NotificationUtil
{
	public static final String NOTIFICATION_CHANNEL_ID = "pftpd running";
	public static final String START_STOP_CHANNEL_ID = "pftpd start/stop";

	private static final Logger LOGGER = LoggerFactory.getLogger(NotificationUtil.class);

	public static final int NOTIFICATION_ID = 1;
	public static final int START_STOP_ID = 2;

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

	private static Notification.Builder createStubNotification(
			Context ctxt,
			int text,
			int actionText,
			String channelId) {
		// create pending intent
		Intent notificationIntent = new Intent(ctxt, PrimitiveFtpdActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(ctxt, 0, notificationIntent, 0);

		Intent stopIntent = new Intent(ctxt, ServicesStartingService.class);
		PendingIntent pendingStopIntent = PendingIntent.getService(ctxt, 0, stopIntent, 0);

		// create channel
		createChannel(ctxt, channelId);

		// create notification
		int iconId = R.drawable.ic_notification;
		int stopIconId = R.drawable.ic_stop_white_24dp;
		CharSequence tickerText = ctxt.getText(text);
		CharSequence contentTitle = ctxt.getText(R.string.notificationTitle);
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
				START_STOP_CHANNEL_ID);

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
			KeyFingerprintProvider keyFingerprintProvider) {
		LOGGER.debug("createStatusbarNotification()");

		Notification.Builder builder = createStubNotification(
				ctxt,
				R.string.serverRunning,
				R.string.stopService,
				NOTIFICATION_CHANNEL_ID);

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
		StringBuilder str = new StringBuilder();
		IpAddressProvider ipAddressProvider = new IpAddressProvider();
		List<String> ipAddressTexts = ipAddressProvider.ipAddressTexts(ctxt, false);
		for (String ipAddressText : ipAddressTexts) {
			if (prefsBean.getServerToStart().startFtp()) {
				str.append("ftp://");
				str.append(ipAddressText);
				str.append(":");
				str.append(prefsBean.getPortStr());
				str.append("\n");
			}
			if (prefsBean.getServerToStart().startSftp()) {
				str.append("sftp://");
				str.append(ipAddressText);
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
}
