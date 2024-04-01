package org.primftpd.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.primftpd.R;
import org.primftpd.util.ServersRunningBean;
import org.primftpd.util.ServicesStartStopUtil;

import java.util.Objects;

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
            view.findViewById(R.id.fallbackButtonToggleServer).setOnClickListener(
                    v -> {
                        logger.debug("click on fallback toggle server");
                        Context ctxt = requireContext();
                        ServersRunningBean serversRunningBean = ServicesStartStopUtil.checkServicesRunning(ctxt);
                        if (serversRunningBean.atLeastOneRunning()) {
                            ServicesStartStopUtil.stopServers(ctxt);
                        } else {
                            ServicesStartStopUtil.startServers(this);
                        }
                    });
        }
        return view;
    }
}
