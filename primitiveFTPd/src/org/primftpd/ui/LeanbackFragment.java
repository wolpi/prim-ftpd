package org.primftpd.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.primftpd.R;
import org.primftpd.util.ServicesStartStopUtil;

import androidx.annotation.NonNull;


public class LeanbackFragment extends PftpdFragment {
    @Override
    protected int getLayoutId() {
        return R.layout.leanback;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view != null) {
            view.findViewById(R.id.fallbackButtonStartServer).setOnClickListener(
                    v -> {
                        logger.debug("click on fallback start");
                        ServicesStartStopUtil.startServers(this);
                    });
            view.findViewById(R.id.fallbackButtonStopServer).setOnClickListener(
                    v -> {
                        logger.debug("click on fallback stop");
                        ServicesStartStopUtil.stopServers(requireActivity());
                    });
        }
        return view;
    }
}
