package org.primftpd.util;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;

import com.nononsenseapps.filepicker.FilePickerActivity;

import org.primftpd.filepicker.ResettingFilePickerActivity;

import java.io.File;

public final class Defaults {
	private Defaults(){}

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
}
