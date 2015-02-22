package org.primftpd.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.ContextWrapper;

public class NotificationUtil
{
	protected static final int NOTIFICATION_ID = 1;

	public static void createStatusbarNotification(
			ContextWrapper androidObject,
			Notification notification)
	{
		NotificationManager notiMgr = (NotificationManager) androidObject.getSystemService(
			Context.NOTIFICATION_SERVICE);
		notiMgr.notify(NOTIFICATION_ID, notification);
	}

	public static void removeStatusbarNotification(ContextWrapper androidObject) {
		NotificationManager notiMgr = (NotificationManager) androidObject.getSystemService(
			Context.NOTIFICATION_SERVICE);
		notiMgr.cancel(NOTIFICATION_ID);
	}
}
