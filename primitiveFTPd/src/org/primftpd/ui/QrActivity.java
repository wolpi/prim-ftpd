package org.primftpd.ui;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.primftpd.R;
import org.primftpd.prefs.LoadPrefsUtil;
import org.primftpd.prefs.PrefsBean;
import org.primftpd.prefs.Theme;
import org.primftpd.util.IpAddressProvider;
import org.primftpd.util.NotificationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class QrActivity extends AppCompatActivity {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private ViewGroup urlsParent;
    private ImageView qrImage;
    private int width;
    private int height;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = LoadPrefsUtil.getPrefs(getBaseContext());
        ThemeUtil.applyTheme(this, prefs);
        setContentView(R.layout.qr);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        urlsParent = findViewById(R.id.qrUrlsParent);
        qrImage = findViewById(R.id.qrImage);

        width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        height = getResources().getDisplayMetrics().heightPixels / 2;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        // navigate back -> the same as for PreferencesActivity
        if (android.R.id.home == item.getItemId()) {
            finish();
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();

        boolean isLeftToRight = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Configuration config = this.getResources().getConfiguration();
            isLeftToRight = config.getLayoutDirection() == View.LAYOUT_DIRECTION_LTR;
        }

        IpAddressProvider ipAddressProvider = new IpAddressProvider();
        List<String> ipAddressTexts = ipAddressProvider.ipAddressTexts(this, false, isLeftToRight);

        SharedPreferences prefs = LoadPrefsUtil.getPrefs(this);
        PrefsBean prefsBean = LoadPrefsUtil.loadPrefs(logger, prefs);

        Boolean showIpv4 = LoadPrefsUtil.showIpv4InNotification(prefs);
        Boolean showIpv6 = LoadPrefsUtil.showIpv6InNotification(prefs);

        List<String> urls = new ArrayList<>();

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

        Theme theme = LoadPrefsUtil.theme(prefs);
        final boolean darkMode;
        switch (theme) {
            case LIGHT:
                darkMode = false;
                break;
            case SYS_DEFAULT:
                int uiMode = getResources().getConfiguration().uiMode;
                int nightModeFlags = uiMode & Configuration.UI_MODE_NIGHT_MASK;
                darkMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
                break;
            default:
                darkMode = true;
                break;
        }

        RadioGroup radioGroup = new RadioGroup(this);
        radioGroup.setOrientation(RadioGroup.VERTICAL);
        for (final String url : urls) {
            logger.debug("showing url: {}", url);
            RadioButton radioButton = new RadioButton(this);
            radioButton.setText(url);
            radioGroup.addView(radioButton);
            final QrActivity activity = this;
            radioButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bitmap qr = activity.generateQr(url, darkMode);
                    activity.qrImage.setImageBitmap(qr);
                }
            });
        }
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
}
