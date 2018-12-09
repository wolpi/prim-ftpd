package org.primftpd.util;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;

import com.nononsenseapps.filepicker.FilePickerActivity;

import org.primftpd.filepicker.ResettingFilePickerActivity;

import java.io.File;

public final class Defaults {
	private Defaults(){}

	public static final String PUBLICKEY_FILENAME = "pftpd-pub.bin";
	public static final String PRIVATEKEY_FILENAME = "pftpd-priv.pk8";

	public static final File HOME_DIR = Environment.getExternalStorageDirectory();
	public static final String PUB_KEY_AUTH_KEY_PATH =
		HOME_DIR.getAbsolutePath() + "/.ssh/authorized_keys";
	public static final String PUB_KEY_AUTH_KEY_PATH_OLD =
			HOME_DIR.getAbsolutePath() + "/.ssh/authorized_key.pub";

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
