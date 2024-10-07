package org.primftpd.ui;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.primftpd.R;
import org.primftpd.prefs.LoadPrefsUtil;
import org.primftpd.prefs.PrefsBean;
import org.primftpd.util.IpAddressProvider;
import org.primftpd.util.NotificationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class QrFragment extends Fragment implements RecreateLogger {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private ViewGroup urlsParent;
    private ImageView qrImage;
    private int width;
    private int height;
    private TextView fallbackTextView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.qr, container, false);

        urlsParent = view.findViewById(R.id.qrUrlsParent);
        qrImage = view.findViewById(R.id.qrImage);
        fallbackTextView = view.findViewById(R.id.qrFallbackTextView);

        width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        height = getResources().getDisplayMetrics().heightPixels / 2;

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean isLeftToRight = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Configuration config = this.getResources().getConfiguration();
            isLeftToRight = config.getLayoutDirection() == View.LAYOUT_DIRECTION_LTR;
        }

        IpAddressProvider ipAddressProvider = new IpAddressProvider();
        List<String> ipAddressTexts = ipAddressProvider.ipAddressTexts(getContext(), false, isLeftToRight);

        SharedPreferences prefs = LoadPrefsUtil.getPrefs(getContext());
        PrefsBean prefsBean = LoadPrefsUtil.loadPrefs(logger, prefs);

        Boolean showIpv4 = LoadPrefsUtil.showIpv4InNotification(prefs);
        Boolean showIpv6 = LoadPrefsUtil.showIpv6InNotification(prefs);

        List<String> urls = new ArrayList<>();

        if (ipAddressTexts.isEmpty()) {
            fallbackTextView.setVisibility(View.VISIBLE);
        } else {
            fallbackTextView.setVisibility(View.GONE);
        }

        for (String ipAddressText : ipAddressTexts) {
            boolean ipv6 = ipAddressProvider.isIpv6(ipAddressText);
            if (!ipv6 && !showIpv4) {
                logger.debug("ignoring ip: {}", ipAddressText);
                continue;
            }
            if (ipv6 && !showIpv6) {
                logger.debug("ignoring ip: {}", ipAddressText);
                continue;
            }

            if (prefsBean.getServerToStart().startFtp()) {
                StringBuilder str = new StringBuilder();
                NotificationUtil.buildUrl(str, ipv6, "ftp", ipAddressText, prefsBean.getPortStr());
                urls.add(str.toString());
            }
            if (prefsBean.getServerToStart().startSftp()) {
                StringBuilder str = new StringBuilder();
                NotificationUtil.buildUrl(str, ipv6, "sftp", ipAddressText, prefsBean.getSecurePortStr());
                urls.add(str.toString());
            }
        }

        final boolean darkMode = UiModeUtil.isDarkMode(getResources());
        RadioGroup radioGroup = new RadioGroup(getContext());
        radioGroup.setOrientation(RadioGroup.VERTICAL);
        for (final String url : urls) {
            logger.debug("showing url: {}", url);
            RadioButton radioButton = new RadioButton(getContext());
            radioButton.setText(url);
            radioGroup.addView(radioButton);
            final QrFragment fragment = this;
            radioButton.setOnClickListener(v -> {
                Bitmap qr = fragment.generateQr(url, darkMode);
                fragment.qrImage.setImageBitmap(qr);
            });
        }
        urlsParent.removeAllViewsInLayout();
        urlsParent.addView(radioGroup);
        if (!urls.isEmpty()) {
            View firstRadio = radioGroup.getChildAt(0);
            firstRadio.callOnClick();
            ((RadioButton)firstRadio).setChecked(true);
        }
    }

    private Bitmap generateQr(String url, boolean darkMode) {
        Map<EncodeHintType, Object> hintsMap = new HashMap<>();
        hintsMap.put(EncodeHintType.CHARACTER_SET, "utf-8");
        hintsMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q);
        hintsMap.put(EncodeHintType.MARGIN, 5);

        int colorForeground = darkMode ? 0xFFFFFFFF : 0x000000;
        int colorBackground = darkMode ? 0x000000 : 0xFFFFFFFF;

        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(url, BarcodeFormat.QR_CODE, width, height, hintsMap);
            int[] pixels = new int[width * height];
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    boolean bitSet = bitMatrix.get(j, i);
                    pixels[i * width + j] = bitSet ? colorForeground : colorBackground;
                }
            }
            return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.RGB_565);
        } catch (WriterException e) {
            logger.error("could not create QR code", e);
        }
        return null;
    }

    @Override
    public void recreateLogger() {
        this.logger = LoggerFactory.getLogger(getClass());
    }
}
