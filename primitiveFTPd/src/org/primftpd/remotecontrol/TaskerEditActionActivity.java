package org.primftpd.remotecontrol;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.primftpd.R;
import org.primftpd.prefs.LoadPrefsUtil;
import org.primftpd.prefs.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskerEditActionActivity extends ListActivity {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private TaskerAction selectedAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        logger.debug("onCreate()");
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = LoadPrefsUtil.getPrefs(getBaseContext());
        Theme theme = LoadPrefsUtil.theme(prefs);
        setTheme(theme.resourceId());
        setContentView(R.layout.tasker_edit_activity);

        TaskerAction[] actions = TaskerAction.values();
        String[] data = new String[actions.length];
        for (int i=0; i<actions.length; i++) {
            data[i] = getText(actions[i].getStringId()).toString();
        }

        ListView listView = findViewById(android.R.id.list);
        listView.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                data
        ));
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        this.selectedAction = TaskerAction.values()[position];
        logger.debug("onListItemClick() selectedAction: '{}'", selectedAction);
        finish();
    }

    @Override
    public void finish() {
        logger.debug("finish()");
        String blurb = selectedAction != null
                ? selectedAction.getBlurb()
                : TaskerAction.START.getBlurb();
        Intent resultIntent = TaskerReceiver.buildResultIntent(this, blurb);
        setResult(RESULT_OK, resultIntent);

        super.finish();
    }
}
