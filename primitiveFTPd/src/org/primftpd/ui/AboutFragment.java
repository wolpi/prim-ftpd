package org.primftpd.ui;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.primftpd.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.fragment.app.Fragment;

public class AboutFragment extends Fragment
{
    public static final String URL_APL =
        "https://www.apache.org/licenses/LICENSE-2.0";
    public static final String URL_GITHUB =
        "https://github.com/wolpi/prim-ftpd";
    public static final String URL_FDROID =
        "https://f-droid.org/repository/browse/?fdid=org.primftpd";
    public static final String URL_MINA = "https://mina.apache.org";
    public static final String URL_BC = "https://bouncycastle.org/";
    public static final String URL_SLF4J = "https://www.slf4j.org/";
    public static final String URL_FILEPICKER = "https://github.com/spacecowboy/NoNonsense-FilePicker";
    public static final String URL_LIBSUPERUSER = "https://su.chainfire.eu/";
    public static final String URL_EVENTBUS = "https://github.com/greenrobot/EventBus";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.about, container, false);

        // show version num
        TextView versionLabel = view.findViewById(R.id.versionLabel);
        versionLabel.setText("Version");

        TextView versionView = view.findViewById(R.id.versionTextView);
        String pkgName = getContext().getPackageName();
        PackageManager pkgMgr = getContext().getPackageManager();
        PackageInfo packageInfo = null;
        try {
            packageInfo = pkgMgr.getPackageInfo(
                pkgName,
                0);
        } catch (NameNotFoundException e) {
            logger.error("could not get version", e);
        }
        String version = packageInfo != null
            ? packageInfo.versionName
            : "unknown";
        if (packageInfo != null) {
            version += " (code: " + packageInfo.versionCode + ")";
        }

        logger.debug("pkgName: '{}'", pkgName);
        logger.debug("versionName: '{}'", version);

        versionView.setText(version);

        // show licence
        TextView lisenseView = view.findViewById(R.id.licenceTextView);
        lisenseView.setText("APL \n"+URL_APL);

        // show other links
        ((TextView)view.findViewById(R.id.githubLabel)).setText("GitHub");
        ((TextView)view.findViewById(R.id.githubTextView)).setText(URL_GITHUB);

        ((TextView)view.findViewById(R.id.fdroidLabel)).setText("F-Droid");
        ((TextView)view.findViewById(R.id.fdroidTextView)).setText(URL_FDROID);

        ((TextView)view.findViewById(R.id.minaTextView)).setText(URL_MINA);
        ((TextView)view.findViewById(R.id.bouncyCastleTextView)).setText(URL_BC);
        ((TextView)view.findViewById(R.id.slf4jTextView)).setText(URL_SLF4J);
        ((TextView)view.findViewById(R.id.filepickerTextView)).setText(URL_FILEPICKER);
        ((TextView)view.findViewById(R.id.libsuperuserTextView)).setText(URL_LIBSUPERUSER);
        ((TextView)view.findViewById(R.id.eventbusTextView)).setText(URL_EVENTBUS);
        return view;
    }
}
