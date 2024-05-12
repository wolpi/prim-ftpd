package org.primftpd.ui;

import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;
import org.primftpd.R;
import org.primftpd.pojo.KeyParser;
import org.primftpd.util.Defaults;
import org.primftpd.util.ServersRunningBean;
import org.primftpd.util.ServicesStartStopUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.Fragment;

public class PubKeyAuthKeysFragment extends Fragment {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final boolean isLeanback;

    public PubKeyAuthKeysFragment(boolean isLeanback) {
        this.isLeanback = isLeanback;
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.pubkey_auth_keys, container, false);

        FloatingActionButton addButton = view.findViewById(R.id.addPubkeyAuthKey);
        if (isLeanback) {
            addButton.setVisibility(View.GONE);
        } else {
            addButton.setOnClickListener(v -> {
                AddPubkeyAuthKeyDialogFragment addDiag = new AddPubkeyAuthKeyDialogFragment(this);
                addDiag.show(requireActivity().getSupportFragmentManager(), PftpdFragment.DIALOG_TAG);
            });
        }

        List<String> keys = loadKeysForDisplay();
        displayKeys(view, keys);

        return view;
    }

    private List<String> loadKeysForDisplay() {
        String[] keyPaths = new java.lang.String[]{
                Defaults.PUB_KEY_AUTH_KEY_PATH_OLDER,
                Defaults.PUB_KEY_AUTH_KEY_PATH_OLD,
                Defaults.pubKeyAuthKeyPath(getContext()),
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
                    if (!line.startsWith("#") && line.length() > 0) {
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

        if (keys.isEmpty()) {
            TextView textView = new TextView(container.getContext());
            textView.setText(R.string.noKeysPresent);
            container.addView(textView);
        }
    }

    protected void addKeyToFile(CharSequence key) {
        final String path = Defaults.pubKeyAuthKeyPath(getContext());
        if (validateKey(key)) {
            try {
                try (FileWriter writer = new FileWriter(path, true)) {
                    writer.append("\n");
                    writer.append(key);
                }

                View view = getView();
                if (view != null) {
                    List<String> keys = loadKeysForDisplay();
                    displayKeys(view, keys);
                }

                ServersRunningBean serversRunningBean = ServicesStartStopUtil.checkServicesRunning(
                        requireContext());
                if (serversRunningBean.atLeastOneRunning()) {
                    Toast.makeText(getContext(), R.string.restartServer, Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                logger.info("could not store key in file '{}': {}, {}",
                        new Object[]{path, e.getClass().getName(), e.getMessage()});
            }
        } else {
            Toast.makeText(getContext(), R.string.pubkeyInvalid, Toast.LENGTH_SHORT).show();
        }
    }

    protected boolean validateKey(CharSequence key) {
        PublicKey pubKey = null;
        try {
            pubKey = KeyParser.parseKeyLine(
                    key.toString(),
                    str -> Base64.decode(str, Base64.DEFAULT));
        } catch (Exception e) {
            // handled by having pubKey equal null
        }
        return pubKey != null;
    }
}
