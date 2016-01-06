package org.primftpd;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import org.primftpd.services.ServicesStartingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartStopWidgetProvider extends AppWidgetProvider
{
	protected Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
	}

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
}
