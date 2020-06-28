package org.primftpd;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import org.primftpd.services.DownloadsService;
import org.primftpd.util.ServersRunningBean;
import org.primftpd.util.ServicesStartStopUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartStopWidgetProvider extends AppWidgetProvider
{
	public static final String WIDGET_TOUCH_ACTION = "org.primftpd.APPWIDGET_TOUCH";

	protected Logger logger() {
		return LoggerFactory.getLogger(getClass());
	}

	public static Intent buildServerStartStopIntent(Context context) {
		Intent intent = new Intent(context, StartStopWidgetProvider.class);
		intent.setAction(WIDGET_TOUCH_ACTION);
		return intent;
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		Logger logger = logger();
		logger.debug("onUpdate()");

		for (int appWidgetId : appWidgetIds) {
			Intent intent = buildServerStartStopIntent(context);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(
					context, 0, intent, 0);
			logger.debug("pendingIntent: {}", pendingIntent);

			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
			views.setOnClickPendingIntent(R.id.widgetIcon, pendingIntent);
			views.setOnClickPendingIntent(R.id.widgetText, pendingIntent);
			appWidgetManager.updateAppWidget(appWidgetId, views);
		}
	}

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		logger().debug("onEnabled()");
	}

	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);
		logger().debug("onDisabled()");
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		String action = intent != null ? intent.getAction() : null;
		//logger().debug("onReceive(), action: {}", action);
		Log.d(getClass().getName(), "onReceive(), action: " + action);

		if (WIDGET_TOUCH_ACTION.equals(action)) {
			ServersRunningBean serversRunningBean = ServicesStartStopUtil.checkServicesRunning(context);
			if (!serversRunningBean.atLeastOneRunning()) {
				ServicesStartStopUtil.startServers(context);
			} else {
				ServicesStartStopUtil.stopServers(context);
			}
		} else if (DownloadsService.ACTION_STOP.equals(action)) {
			context.stopService(new Intent(context, DownloadsService.class));
		}
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		logger().debug("onDeleted()");
	}

	@Override
	public void onAppWidgetOptionsChanged(
			Context context,
			AppWidgetManager appWidgetManager,
			int appWidgetId,
			Bundle newOptions) {
		super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
		logger().debug("onAppWidgetOptionsChanged()");
	}
}
