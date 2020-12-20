package org.primftpd.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.primftpd.R;
import org.primftpd.events.ClientActionEvent;
import org.primftpd.events.DataTransferredEvent;
import org.primftpd.prefs.LoadPrefsUtil;
import org.primftpd.prefs.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.CharacterIterator;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ClientActionActivity extends Activity {

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static final int EVENT_UPDATE_STATS = 1;
    public static final int INTERVAL_UPDATE_STATS = 100;
    public static final int FACTOR_1S = 1000 / INTERVAL_UPDATE_STATS;
    private static final List<DataTransferredEvent> DATA_TRANSFERRED_EVENTS = new LinkedList<>();
    private static long TIMESTAMP_LAST_STAT_UPDATE;
    private static long TOTAL_BYTES_READ;
    private static long TOTAL_BYTES_WRITTEN;

    private static class UpdateStatsHandler extends Handler {
        private final Logger logger = LoggerFactory.getLogger(getClass());

        private final TextView sentTotal;
        private final TextView receivedTotal;
        private final TextView sentPerSec;
        private final TextView receivedPerSec;

        public UpdateStatsHandler(TextView sentTotal, TextView receivedTotal, TextView sentPerSec, TextView receivedPerSec) {
            this.sentTotal = sentTotal;
            this.receivedTotal = receivedTotal;
            this.sentPerSec = sentPerSec;
            this.receivedPerSec = receivedPerSec;
        }

        @Override
        public void handleMessage(Message msg) {
            logger.trace("handleMessage(), num events: {}", DATA_TRANSFERRED_EVENTS.size());
            TIMESTAMP_LAST_STAT_UPDATE = System.currentTimeMillis();
            if (updateStats) {
                switch (msg.what) {
                    case EVENT_UPDATE_STATS:
                        long bytesRead = 0;
                        long bytesWritten = 0;
                        for (DataTransferredEvent event : DATA_TRANSFERRED_EVENTS) {
                            if (event.getTimestamp() > TIMESTAMP_LAST_STAT_UPDATE - INTERVAL_UPDATE_STATS) {
                                if (event.isWrite()) {
                                    bytesWritten += event.getBytes();
                                } else {
                                    bytesRead += event.getBytes();
                                }
                            }
                        }
                        bytesRead *= FACTOR_1S;
                        bytesWritten *= FACTOR_1S;

                        sentTotal.setText(humanReadableByteCountSI(TOTAL_BYTES_READ));
                        receivedTotal.setText(humanReadableByteCountSI(TOTAL_BYTES_WRITTEN));
                        sentPerSec.setText(humanReadableByteCountSI(bytesRead, "/s"));
                        receivedPerSec.setText(humanReadableByteCountSI(bytesWritten, "/s"));
                        break;

                    default:
                        break;
                }
                Message nextMsg = this.obtainMessage(EVENT_UPDATE_STATS);
                this.sendMessageDelayed(nextMsg, INTERVAL_UPDATE_STATS);
            }
        }
    }

    private static boolean updateStats;

    public static String humanReadableByteCountSI(long bytes) {
        return  humanReadableByteCountSI(bytes, "");
    }
    public static String humanReadableByteCountSI(long bytes, String suffix) {
        if (bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format("%.1f %2$cB%3$s", bytes / 1000.0, ci.current(), suffix);
    }

    public static String format(ClientActionEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append(DATE_FORMAT.format(event.getTimestamp()));
        sb.append(" ");
        sb.append(event.getStorage());
        sb.append(" ");
        sb.append(event.getProtocol());
        sb.append(" ");
        sb.append(event.getClientIp());
        sb.append(" ");
        sb.append(event.getClientAction());
        sb.append(" ");
        sb.append(event.getPath());
        return sb.toString();
    }

    private TextView logContent;
    private ScrollView scrollView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = LoadPrefsUtil.getPrefs(getBaseContext());
        Theme theme = LoadPrefsUtil.theme(prefs);
        setTheme(theme.resourceId());
        setContentView(R.layout.client_action);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        EventBus.getDefault().register(this);

        logContent = findViewById(R.id.clientActionsContent);
        scrollView = findViewById(R.id.clientActionsScrollView);

        TOTAL_BYTES_READ = 0;
        TOTAL_BYTES_WRITTEN = 0;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
/*
        updateStats = true;

        TextView sentTotal = findViewById(R.id.statsTotalSent);
        TextView receivedTotal = findViewById(R.id.statsTotalReceived);
        TextView sentPerSec = findViewById(R.id.statsSentPerSec);
        TextView receivedPerSec = findViewById(R.id.statsReceivedPerSec);

        UpdateStatsHandler handler = new UpdateStatsHandler(sentTotal, receivedTotal, sentPerSec, receivedPerSec);
        Message msg = handler.obtainMessage(EVENT_UPDATE_STATS);
        handler.sendMessageDelayed(msg, INTERVAL_UPDATE_STATS);
*/    }

    @Override
    protected void onPause() {
        super.onPause();
        updateStats = false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        // navigate back -> the same as for PreferencesActivity
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEvent(ClientActionEvent event) {
        String clientAction = format(event);

        logContent.append(clientAction);
        logContent.append("\n");
        scrollView.fullScroll(View.FOCUS_DOWN);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEvent(DataTransferredEvent event) {
        List<DataTransferredEvent> toBeRemoved = new ArrayList<>();
        for (DataTransferredEvent oldEvent : DATA_TRANSFERRED_EVENTS) {
            if (oldEvent.getTimestamp() < event.getTimestamp() - TIMESTAMP_LAST_STAT_UPDATE) {
                toBeRemoved.add(oldEvent);
            }
        }
        for (DataTransferredEvent oldEvent : toBeRemoved) {
            DATA_TRANSFERRED_EVENTS.remove(oldEvent);
        }
        DATA_TRANSFERRED_EVENTS.add(event);
        if (event.isWrite()) {
            TOTAL_BYTES_WRITTEN += event.getBytes();
        } else {
            TOTAL_BYTES_READ += event.getBytes();
        }
    }
}
