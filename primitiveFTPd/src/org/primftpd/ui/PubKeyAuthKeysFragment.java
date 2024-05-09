package org.primftpd.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.primftpd.R;
import org.primftpd.util.Defaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.Fragment;

public class PubKeyAuthKeysFragment extends Fragment {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.pubkey_auth_keys, container, false);

        List<String> keys = loadKeysForDisplay();
        displayKeys(view, keys);

        return view;
    }

    private List<String> loadKeysForDisplay() {
        String[] keyPaths = new java.lang.String[]{
                Defaults.pubKeyAuthKeyPath(getContext()),
                Defaults.PUB_KEY_AUTH_KEY_PATH_OLD,
                Defaults.PUB_KEY_AUTH_KEY_PATH_OLDER,
        };

        List<String> keys = new ArrayList<>();
        for (String path : keyPaths) {
            keys.addAll(loadKeysOfFile(path));
        }
        return keys;
    }

    private List<String> loadKeysOfFile(String path) {
        List<String> keys = new ArrayList<>();
        try {
            try (FileReader filereader = new FileReader(path)) {
                BufferedReader reader = new BufferedReader(filereader);
                while (reader.ready()) {
                    String line = reader.readLine();
                    if (!line.startsWith("#")) {
                        keys.add(line);
                    }
                }
            }
        } catch (IOException e) {
            logger.info("could not load key of path '{}': {}, {}",
                    new Object[]{path, e.getClass().getName(), e.getMessage()});
        }
        return keys;
    }

    private void displayKeys(View view, List<String> keys) {
        LinearLayout container = view.findViewById(R.id.pubkeyAuthKeysContainer);
        container.removeAllViews();
        for (String key : keys) {
            TextView textView = new TextView(container.getContext());
            textView.setText(key);
            textView.setPadding(1, 1, 1, 5);
            container.addView(textView);
        }
    }
}
