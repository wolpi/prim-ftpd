package org.primftpd.util;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;


import org.primftpd.filepicker.ResettingFilePickerActivity;
import org.primftpd.filepicker.nononsenseapps.FilePickerActivity;

import java.io.File;

public final class Defaults {
	private Defaults(){}

	public static final String PUBLICKEY_FILENAME = "pftpd-pub.bin";
	public static final String PRIVATEKEY_FILENAME = "pftpd-priv.pk8";

	public static final File HOME_DIR = Environment.getExternalStorageDirectory();
	public static final File DOWNLOADS_DIR = Environment.getExternalStoragePublicDirectory(
			Environment.DIRECTORY_DOWNLOADS);
	public static final String PUB_KEY_AUTH_KEY_PATH_OLD =
		HOME_DIR.getAbsolutePath() + "/.ssh/authorized_keys";
	public static final String PUB_KEY_AUTH_KEY_PATH_OLDER =
			HOME_DIR.getAbsolutePath() + "/.ssh/authorized_key.pub";

	public static File homeDirScoped(Context ctxt) {
		return ctxt.getExternalFilesDir(null);
	}
	public static String pubKeyAuthKeyPath(Context ctxt) {
		return homeDirScoped(ctxt).getAbsolutePath() + "/.ssh/authorized_keys";
	}
	public static File quickShareTmpDir(Context ctxt) {
		return new File(homeDirScoped(ctxt), "quick-share");
	}

	public static Intent createDefaultDirPicker(Context ctxt) {
		return createDefaultDirPicker(ctxt, HOME_DIR);
	}

	public static Intent createDefaultDirPicker(Context ctxt, File initialVal) {
		Intent dirPickerIntent = new Intent(ctxt, ResettingFilePickerActivity.class);
		dirPickerIntent.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
		dirPickerIntent.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
		dirPickerIntent.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR);
		dirPickerIntent.putExtra(FilePickerActivity.EXTRA_START_PATH, initialVal.getAbsolutePath());
		return dirPickerIntent;
	}

	public static Intent createDirAndFilePicker(Context ctxt) {
		Intent intent = new Intent(ctxt, ResettingFilePickerActivity.class);
		intent.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
		intent.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
		intent.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE_AND_DIR);
		intent.putExtra(FilePickerActivity.EXTRA_START_PATH, HOME_DIR.getAbsolutePath());
		return intent;
	}
}
