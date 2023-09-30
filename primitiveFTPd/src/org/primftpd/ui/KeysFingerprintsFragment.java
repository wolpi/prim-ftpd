package org.primftpd.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.primftpd.R;
import org.primftpd.crypto.HostKeyAlgorithm;
import org.primftpd.util.KeyFingerprintBean;
import org.primftpd.util.KeyFingerprintProvider;

import androidx.fragment.app.Fragment;

public class KeysFingerprintsFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.keys_fingerprints, container, false);

        KeyFingerprintProvider keyFingerprintProvider = new KeyFingerprintProvider();
        keyFingerprintProvider.calcPubkeyFingerprints(getContext());

        HostKeyAlgorithm algo = HostKeyAlgorithm.ED_25519;
        ((TextView) view.findViewById(R.id.keyFingerprintEd25519Md5Label))
                .setText("MD5 (" + algo.getAlgorithmName() + ")");
        ((TextView) view.findViewById(R.id.keyFingerprintEd25519Sha1Label))
                .setText("SHA1 (" + algo.getAlgorithmName() + ")");
        ((TextView) view.findViewById(R.id.keyFingerprintEd25519Sha256Label))
                .setText("SHA256 (" + algo.getAlgorithmName() + ")");

        KeyFingerprintBean keyFingerprintBean = keyFingerprintProvider.getFingerprints().get(algo);

        if (keyFingerprintBean != null) {
            ((TextView) view.findViewById(R.id.keyFingerprintEd25519Md5TextView))
                    .setText(keyFingerprintBean.getFingerprintMd5());
            ((TextView) view.findViewById(R.id.keyFingerprintEd25519Sha1TextView))
                    .setText(keyFingerprintBean.getFingerprintSha1());
            ((TextView) view.findViewById(R.id.keyFingerprintEd25519Sha256TextView))
                    .setText(keyFingerprintBean.getFingerprintSha256());
        }

        algo = HostKeyAlgorithm.RSA_4096;
        ((TextView) view.findViewById(R.id.keyFingerprintRsa4096Md5Label))
                .setText("MD5 (" + algo.getAlgorithmName() + " 4096)");
        ((TextView) view.findViewById(R.id.keyFingerprintRsa4096Sha1Label))
                .setText("SHA1 (" + algo.getAlgorithmName() + " 4096)");
        ((TextView) view.findViewById(R.id.keyFingerprintRsa4096Sha256Label))
                .setText("SHA256 (" + algo.getAlgorithmName() + " 4096)");

        keyFingerprintBean = keyFingerprintProvider.getFingerprints().get(algo);

        if (keyFingerprintBean != null) {
            ((TextView) view.findViewById(R.id.keyFingerprintRsa4096Md5TextView))
                    .setText(keyFingerprintBean.getFingerprintMd5());
            ((TextView) view.findViewById(R.id.keyFingerprintRsa4096Sha1TextView))
                    .setText(keyFingerprintBean.getFingerprintSha1());
            ((TextView) view.findViewById(R.id.keyFingerprintRsa4096Sha256TextView))
                    .setText(keyFingerprintBean.getFingerprintSha256());
        }

        algo = HostKeyAlgorithm.RSA_2048;
        ((TextView) view.findViewById(R.id.keyFingerprintRsa2048Md5Label))
                .setText("MD5 (" + algo.getAlgorithmName() + " 2048)");
        ((TextView) view.findViewById(R.id.keyFingerprintRsa2048Sha1Label))
                .setText("SHA1 (" + algo.getAlgorithmName() + " 2048)");
        ((TextView) view.findViewById(R.id.keyFingerprintRsa2048Sha256Label))
                .setText("SHA256 (" + algo.getAlgorithmName() + " 2048)");

        keyFingerprintBean = keyFingerprintProvider.getFingerprints().get(algo);

        if (keyFingerprintBean != null) {
            ((TextView) view.findViewById(R.id.keyFingerprintRsa2048Md5TextView))
                    .setText(keyFingerprintBean.getFingerprintMd5());
            ((TextView) view.findViewById(R.id.keyFingerprintRsa2048Sha1TextView))
                    .setText(keyFingerprintBean.getFingerprintSha1());
            ((TextView) view.findViewById(R.id.keyFingerprintRsa2048Sha256TextView))
                    .setText(keyFingerprintBean.getFingerprintSha256());
        }
        return view;
    }
}
