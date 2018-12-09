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

public class TaskerEditConditionActivity extends ListActivity {

    private TaskerCondition selectedCondition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = LoadPrefsUtil.getPrefs(getBaseContext());
        Theme theme = LoadPrefsUtil.theme(prefs);
        setTheme(theme.resourceId());
        setContentView(R.layout.tasker_edit_activity);

        TaskerCondition[] conditions = TaskerCondition.values();
        String[] data = new String[conditions.length];
        for (int i=0; i<conditions.length; i++) {
            data[i] = getText(conditions[i].getStringId()).toString();
        }

        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                data
        ));
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        this.selectedCondition = TaskerCondition.values()[position];
        finish();
    }

    @Override
    public void finish() {
        String blurb = selectedCondition != null
                ? selectedCondition.getBlurb()
                : TaskerCondition.IS_SERVER_RUNNING.getBlurb();
        Intent resultIntent = TaskerReceiver.buildResultIntent(this, blurb);
        setResult(RESULT_OK, resultIntent);

        super.finish();
    }
}
