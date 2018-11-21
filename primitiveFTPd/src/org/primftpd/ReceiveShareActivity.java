package org.primftpd;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.provider.DocumentFile;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.primftpd.prefs.LoadPrefsUtil;
import org.primftpd.prefs.Theme;
import org.primftpd.util.Defaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ReceiveShareActivity extends Activity {
    protected Logger logger = LoggerFactory.getLogger(getClass());

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

        if (uris != null || contents != null) {
            logger.debug("trying to create intent");
            try {
                Intent dirPickerIntent = Defaults.createDefaultDirPicker(getBaseContext());
                logger.debug("got intent: {}", dirPickerIntent);
                startActivityForResult(dirPickerIntent, 0);
            } catch (Exception e) {
                logger.debug("could not create intent", e);
            }
        }

        // display uris, usually that should not be visible
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        logger.debug("onActivityResult()");

        if (resultCode == Activity.RESULT_OK) {
            Uri targetUri = data.getData();
            File targetDir = com.nononsenseapps.filepicker.Utils.getFileForUri(targetUri);
            logger.debug("targetDir: {}", targetDir);

            if (uris != null) {
                saveUris(targetDir);
            } else {
                saveContents(targetDir);
            }
        }
        finish();
    }

    protected void saveUris(File targetDir) {
        if (uris == null) {
            return;
        }
        for (int i=0; i<uris.size(); i++) {
            Uri uri = uris.get(i);
            if (uri == null) {
                continue;
            }
            String content = null;
            if (contents != null && i < contents.size()) {
                content = contents.get(i);
            }
            FileOutputStream fos = null;
            InputStream is = null;
            try {
                String filename = filename(uri, content, this.type, targetDir);
                File targetFile = new File(targetDir, filename);
                logger.debug("saving under: {}", targetFile);
                fos = new FileOutputStream(targetFile);
                is = getContentResolver().openInputStream(uri);
                copyStream(is, fos);
            } catch (Exception e) {
                logger.warn("could not copy shared data", e);
            } finally {
                try {
                    if (fos != null) fos.close();
                    if (is != null) is.close();
                } catch (IOException e) {
                    logger.warn("could not copy shared data", e);
                }
            }
        }
    }

    protected void saveContents(File targetDir) {
        if (contents == null) {
            return;
        }
        for (String content : contents) {
            if (content == null) {
                continue;
            }
            FileOutputStream fos = null;
            try {
                String filename = filename(null, null, "txt", targetDir);
                File targetFile = new File(targetDir, filename);
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

    protected static DateFormat FILENAME_DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
    protected String filename(Uri uri, String content, String type, File targetDir) {
        String filename = null;
        boolean addExtension = false;
        if (content != null) {
            // if we got content use that as filename
            filename = content;
            addExtension = true;
        }
        if (filename == null && uri != null) {
            // if we don't got content derive filename from url

            // 1st: try to resolve content url
            try {
                DocumentFile documentFile = DocumentFile.fromSingleUri(this, uri);
                filename = documentFile.getName();
            } catch (Exception e) {
                logger.error("could not resolve content url: " + uri, e);

                // 2nd: use last segment of url
                filename = uri.getLastPathSegment();
            }

            addExtension = !filename.contains(".");
        }
        if (filename == null || (content == null && addExtension)) {
            // if we still don't have a filename or last part of url does not contain a extension
            // use timestamp as filename
            filename = FILENAME_DATEFORMAT.format(new Date());
            addExtension = true;
        }
        String fileExt = null;
        if (addExtension) {
            // try to add extension base on given mime type
            if (type != null) {
                fileExt = type.contains("/")
                        ? type.substring(type.lastIndexOf('/') + 1, type.length())
                        : type;
                if ("plain".equals(fileExt)) {
                    fileExt = "txt";
                }
            }
        }

        // check if file exists
        boolean exists;
        int counter = 0;
        do {
            String uniquePart = counter == 0 ? "" : "_" + counter;
            String tmpName = fileExt != null ? filename + uniquePart + "." + fileExt : filename + uniquePart;
            File targetFile = new File(targetDir, tmpName);
            exists = targetFile.exists();
            if (exists) {
                logger.debug("file already exists: {}", targetFile.getAbsolutePath());
                counter++;
            }
        } while (exists);

        // make unique if was existing
        if (counter > 0) {
            filename += "_" + counter;
        }

        // add extension if present
        if (fileExt != null) {
            filename += "." + fileExt;
        }

        logger.debug("generated filename '{}' for uri '{}', content '{}', type '{}'",
                new Object[]{filename, uri, content, type});
        return filename;
    }

    private static final int BUFFER_SIZE = 4096;

    private void copyStream(InputStream is, OutputStream os) {
        try {
            byte[] bytes = new byte[BUFFER_SIZE];
            for (;;) {
                int count = is.read(bytes, 0, BUFFER_SIZE);
                if (count == -1) {
                    break;
                }
                os.write(bytes, 0, count);
            }
        } catch (Exception e) {
            logger.warn("could not copy stream", e);
        }
    }
}
