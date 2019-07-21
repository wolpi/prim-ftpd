package org.primftpd;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.RemoteViews;

import org.primftpd.services.ServicesStartingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartStopWidgetProvider extends AppWidgetProvider
{
	protected Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		logger.debug("onUpdate()");

		for (int appWidgetId : appWidgetIds) {
			Intent intent = new Intent(context, ServicesStartingService.class);
			PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);
			logger.debug("pendingIntent: {}", pendingIntent);

			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
			views.setOnClickPendingIntent(R.id.widgetLayout, pendingIntent);
			appWidgetManager.updateAppWidget(appWidgetId, views);
		}
	}

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		logger.debug("onEnabled()");

		// set as enabled to make sure we are enabled as broadcast receiver
		PackageManager pm = context.getPackageManager();
		pm.setComponentEnabledSetting(
				new ComponentName("com.example.android.apis", ".StartStopWidgetProvider"),
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
				PackageManager.DONT_KILL_APP);
	}

	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);
		logger.debug("onDisabled()");

		// this shall unregister from broadcast events
		// looks weird ...
		PackageManager pm = context.getPackageManager();
		pm.setComponentEnabledSetting(
				new ComponentName("com.example.android.apis", ".StartStopWidgetProvider"),
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
				PackageManager.DONT_KILL_APP);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		logger.debug("onReceive()");
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		logger.debug("onDeleted()");
	}

	@Override
	public void onAppWidgetOptionsChanged(
			Context context,
			AppWidgetManager appWidgetManager,
			int appWidgetId,
			Bundle newOptions) {
		super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
		logger.debug("onAppWidgetOptionsChanged()");
	}
}
