package org.primftpd.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import org.primftpd.R;
import org.primftpd.share.ReceiveSaveAsActivity;
import org.primftpd.services.DownloadsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

public class DownloadOrSaveDialogFragment extends DialogFragment {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String KEY_URL = "URL";

    private String url;

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        url = args.getString(KEY_URL);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        logger.debug("showing download or save dialog");
        final FragmentActivity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(R.string.downloadOrSaveUrlToFile);

        builder.setPositiveButton(R.string.download, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                logger.debug("positive button");

//                Uri uri = Uri.parse(url);
//                String filename = null;
//                List<String> pathSegments = uri.getPathSegments();
//                if (pathSegments != null) {
//                    filename = pathSegments.get(pathSegments.size() - 1);
//                }
//                if (filename == null) {
//                    filename = "download";
//                }

//                DownloadManager.Request downloadRequest = new DownloadManager.Request(uri);
//                downloadRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
//                downloadRequest.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
//                downloadRequest.allowScanningByMediaScanner();
//                DownloadManager dm = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
//                dm.enqueue(downloadRequest);

                Intent intent = new Intent(activity, DownloadsService.class);
                intent.setAction(DownloadsService.ACTION_START);
                intent.putExtra(DownloadsService.URL, url);
                activity.startService(intent);
                activity.finish();
            }
        });
        builder.setNegativeButton(R.string.saveToTextFile, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                logger.debug("negative button");
                ((ReceiveSaveAsActivity) activity).prepareSaveToIntent();
            }
        });
        return builder.create();
    }
}
