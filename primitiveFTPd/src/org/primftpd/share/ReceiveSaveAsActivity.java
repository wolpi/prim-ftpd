package org.primftpd.share;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.primftpd.R;
import org.primftpd.filepicker.nononsenseapps.Utils;
import org.primftpd.prefs.LoadPrefsUtil;
import org.primftpd.prefs.Theme;
import org.primftpd.ui.DownloadOrSaveDialogFragment;
import org.primftpd.util.Defaults;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReceiveSaveAsActivity extends AbstractReceiveShareActivity {

    public static final String DIALOG_TAG = "dialogs";

    public static final String PREFIX_HTTP = "http://";
    public static final String PREFIX_HTTPS = "https://";

    private List<Uri> uris;
    private List<String> contents;
    private String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logger.debug("onCreate()");

        // set theme
        SharedPreferences prefs = LoadPrefsUtil.getPrefs(getBaseContext());
        Theme theme = LoadPrefsUtil.theme(prefs);
        setTheme(theme.resourceId());

        // set layout
        setContentView(R.layout.receive_share);

        // read intent
        Intent intent = getIntent();
        String action = intent.getAction();
        type = intent.getType();

        logger.debug("got action: '{}' and type: '{}'", action, type);

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri != null) {
                uris = Collections.singletonList(uri);
            }
            String content = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (content != null) {
                contents = Collections.singletonList(content);
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            contents = intent.getStringArrayListExtra(Intent.EXTRA_TEXT);
        }

        logger.debug("got uris: '{}', contents: '{}'", uris, contents);

        // check for save to file or use download intent
        List<String> strings = new ArrayList<>();
        if (contents != null) {
            strings.addAll(contents);
        }
        if (uris != null) {
            for (Uri uri : uris) {
                strings.add(uri.toString());
            }
        }
        boolean showDownloadDialog = false;
        String singleString = null;
        if (strings.size() == 1) {
            singleString = strings.get(0);
            if (singleString.startsWith(PREFIX_HTTP) || singleString.startsWith(PREFIX_HTTPS)) {
                showDownloadDialog = true;
            }
        }

        if (showDownloadDialog) {
            DownloadOrSaveDialogFragment dialog = new DownloadOrSaveDialogFragment();
            dialog.show(getSupportFragmentManager(), DIALOG_TAG);
            Bundle diagArgs = new Bundle();
            diagArgs.putString(DownloadOrSaveDialogFragment.KEY_URL, singleString);
            dialog.setArguments(diagArgs);
        } else {
            prepareSaveToIntent();
        }

        // display uris, visible for download dialog
        ListView listView = findViewById(android.R.id.list);
        if (uris != null) {
            listView.setAdapter(new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_list_item_1,
                    uris
            ));
        } else if (contents != null) {
            listView.setAdapter(new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_list_item_1,
                    contents
            ));
        }
    }

    public void prepareSaveToIntent() {
        if (uris != null || contents != null) {
            logger.debug("trying to create intent");
            try {
                Intent dirPickerIntent = Defaults.createDirAndFilePicker(getBaseContext());
                logger.debug("got intent: {}", dirPickerIntent);
                startActivityForResult(dirPickerIntent, 0);
            } catch (Exception e) {
                logger.debug("could not create intent", e);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        logger.debug("onActivityResult()");

        if (resultCode == Activity.RESULT_OK) {
            Uri targetUri = data.getData();
            File targetPath = Utils.getFileForUri(targetUri);
            logger.debug("targetDir: {}", targetPath);

            if (uris != null) {
                saveUris(targetPath, uris, contents, type);
            } else {
                saveContents(targetPath);
            }
        }
        finish();
    }


    protected void saveContents(File targetPath) {
        if (contents == null) {
            return;
        }
        for (String content : contents) {
            if (content == null) {
                continue;
            }
            FileOutputStream fos = null;
            try {
                File targetFile = targetFile(null, null, "txt", targetPath);
                logger.debug("saving under: {}", targetFile);
                fos = new FileOutputStream(targetFile);
                PrintStream ps = new PrintStream(fos);
                ps.println(content);
            } catch (Exception e) {
                logger.warn("could not copy shared data", e);
            } finally {
                try {
                    if (fos != null) fos.close();
                } catch (IOException e) {
                    logger.warn("could not copy shared data", e);
                }
            }
        }
    }
}
