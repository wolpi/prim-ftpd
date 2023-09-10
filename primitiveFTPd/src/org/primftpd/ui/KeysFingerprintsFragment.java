package org.primftpd.ui;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.primftpd.R;
import org.primftpd.crypto.HostKeyAlgorithm;
import org.primftpd.prefs.LoadPrefsUtil;
import org.primftpd.util.KeyFingerprintBean;
import org.primftpd.util.KeyFingerprintProvider;

public class KeysFingerprintsActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = LoadPrefsUtil.getPrefs(getBaseContext());
        ThemeUtil.applyTheme(this, prefs);
        setContentView(R.layout.keys_fingerprints);

        // show action bar to allow user to navigate back
        // -> the same as for PreferencesActivity
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
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
    @SuppressLint("SetTextI18n")
    protected void onStart() {
        super.onStart();

        KeyFingerprintProvider keyFingerprintProvider = new KeyFingerprintProvider();
        keyFingerprintProvider.calcPubkeyFingerprints(this);

        HostKeyAlgorithm algo = HostKeyAlgorithm.ED_25519;
        ((TextView) findViewById(R.id.keyFingerprintEd25519Md5Label))
                .setText("MD5 (" + algo.getAlgorithmName() + ")");
        ((TextView) findViewById(R.id.keyFingerprintEd25519Sha1Label))
                .setText("SHA1 (" + algo.getAlgorithmName() + ")");
        ((TextView) findViewById(R.id.keyFingerprintEd25519Sha256Label))
                .setText("SHA256 (" + algo.getAlgorithmName() + ")");

        KeyFingerprintBean keyFingerprintBean = keyFingerprintProvider.getFingerprints().get(algo);

        if (keyFingerprintBean != null) {
            ((TextView) findViewById(R.id.keyFingerprintEd25519Md5TextView))
                    .setText(keyFingerprintBean.getFingerprintMd5());
            ((TextView) findViewById(R.id.keyFingerprintEd25519Sha1TextView))
                    .setText(keyFingerprintBean.getFingerprintSha1());
            ((TextView) findViewById(R.id.keyFingerprintEd25519Sha256TextView))
                    .setText(keyFingerprintBean.getFingerprintSha256());
        }

        algo = HostKeyAlgorithm.RSA_4096;
        ((TextView) findViewById(R.id.keyFingerprintRsa4096Md5Label))
                .setText("MD5 (" + algo.getAlgorithmName() + " 4096)");
        ((TextView) findViewById(R.id.keyFingerprintRsa4096Sha1Label))
                .setText("SHA1 (" + algo.getAlgorithmName() + " 4096)");
        ((TextView) findViewById(R.id.keyFingerprintRsa4096Sha256Label))
                .setText("SHA256 (" + algo.getAlgorithmName() + " 4096)");

        keyFingerprintBean = keyFingerprintProvider.getFingerprints().get(algo);

        if (keyFingerprintBean != null) {
            ((TextView) findViewById(R.id.keyFingerprintRsa4096Md5TextView))
                    .setText(keyFingerprintBean.getFingerprintMd5());
            ((TextView) findViewById(R.id.keyFingerprintRsa4096Sha1TextView))
                    .setText(keyFingerprintBean.getFingerprintSha1());
            ((TextView) findViewById(R.id.keyFingerprintRsa4096Sha256TextView))
                    .setText(keyFingerprintBean.getFingerprintSha256());
        }

        algo = HostKeyAlgorithm.RSA_2048;
        ((TextView) findViewById(R.id.keyFingerprintRsa2048Md5Label))
                .setText("MD5 (" + algo.getAlgorithmName() + " 2048)");
        ((TextView) findViewById(R.id.keyFingerprintRsa2048Sha1Label))
                .setText("SHA1 (" + algo.getAlgorithmName() + " 2048)");
        ((TextView) findViewById(R.id.keyFingerprintRsa2048Sha256Label))
                .setText("SHA256 (" + algo.getAlgorithmName() + " 2048)");

        keyFingerprintBean = keyFingerprintProvider.getFingerprints().get(algo);

        if (keyFingerprintBean != null) {
            ((TextView) findViewById(R.id.keyFingerprintRsa2048Md5TextView))
                    .setText(keyFingerprintBean.getFingerprintMd5());
            ((TextView) findViewById(R.id.keyFingerprintRsa2048Sha1TextView))
                    .setText(keyFingerprintBean.getFingerprintSha1());
            ((TextView) findViewById(R.id.keyFingerprintRsa2048Sha256TextView))
                    .setText(keyFingerprintBean.getFingerprintSha256());
        }
    }
}
