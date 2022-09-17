package org.primftpd.share;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.primftpd.R;
import org.primftpd.filepicker.nononsenseapps.Utils;
import org.primftpd.prefs.LoadPrefsUtil;
import org.primftpd.prefs.Theme;
import org.primftpd.ui.DownloadOrSaveDialogFragment;
import org.primftpd.util.Defaults;
import org.primftpd.util.FilenameUnique;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
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

        // display uris
        ListView listView = findViewById(R.id.list);
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
    }

    public void prepareSaveToIntent() {
        if (uris != null || contents != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                logger.debug("trying to create SAF intent");
                Intent safIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                safIntent.addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(safIntent, 1);
            } else {
                logger.debug("trying to create custom filepicker intent");
                try {
                    Intent dirPickerIntent = Defaults.createDefaultDirPicker(getBaseContext());
                    logger.debug("got intent: {}", dirPickerIntent);
                    startActivityForResult(dirPickerIntent, 0);
                } catch (Exception e) {
                    logger.debug("could not create intent", e);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        logger.debug("onActivityResult()");

        if (resultCode == Activity.RESULT_OK) {
            TargetDir targetDir = null;
            if (requestCode == 0) {
                Uri targetUri = data.getData();
                File targetPath = Utils.getFileForUri(targetUri);
                logger.debug("targetDir: {}", targetPath);
                targetDir = new TargetDir(targetPath);

            } else if (requestCode == 1) {
                Uri uri = data.getData();
                String uriStr = uri.toString();
                logger.debug("got SAF uri: '{}'", uriStr);
                targetDir = new TargetDir(this, uriStr);

            }

            if (uris != null) {
                ProgressDialog progressDialog = createProgressDialog(uris.size());
                saveUris(progressDialog, targetDir, uris, contents, type);
            } else {
                saveContents(targetDir);
            }
        }

        // finish is called by async task
        //finish();
    }


    protected void saveContents(TargetDir targetDir) {
        if (contents == null) {
            finish();
            return;
        }
        for (String content : contents) {
            if (content == null) {
                continue;
            }
            OutputStream os = null;
            try {
                String filename = FilenameUnique.filename(null, null, "txt", targetDir, this);
                logger.debug("saving with filename: {}", filename);
                os = targetDir.createOutStream(filename);
                PrintStream ps = new PrintStream(os);
                ps.println(content);
            } catch (Exception e) {
                logger.warn("could not copy shared data", e);
            } finally {
                try {
                    if (os != null) os.close();
                } catch (IOException e) {
                    logger.warn("could not copy shared data", e);
                }
            }
        }
        finish();
    }
}
