package org.primftpd.util;

import android.os.Environment;

import java.io.File;

public final class Defaults {
	private Defaults(){}

	public static final File HOME_DIR = Environment.getExternalStorageDirectory();
}
