package org.primftpd.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.primftpd.R;
import org.primftpd.events.ClientActionEvent;
import org.primftpd.prefs.LoadPrefsUtil;
import org.primftpd.prefs.Theme;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class ClientActionActivity extends Activity {

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

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

    private TextView content;
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

        content = findViewById(R.id.clientActionsContent);
        scrollView = findViewById(R.id.clientActionsScrollView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);
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

        content.append(clientAction);
        content.append("\n");
        scrollView.fullScroll(View.FOCUS_DOWN);
    }
}
