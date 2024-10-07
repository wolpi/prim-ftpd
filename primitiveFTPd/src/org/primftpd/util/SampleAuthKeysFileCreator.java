package org.primftpd.util;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class SampleAuthKeysFileCreator
{
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public void createSampleAuthorizedKeysFiles(Context context) {
		String[] paths = new String[]{
				Defaults.pubKeyAuthKeyPath(context),
		};

		for (String path : paths) {
			File file = new File(path);
			if (!file.exists()) {
				File dir = file.getParentFile();
				if (!dir.exists()) {
					logger.debug("trying to create dir: {}", dir.getAbsolutePath());
					dir.mkdirs();
				}
				if (dir.exists()) {
					logger.debug("trying to create sample authorized keys file: {}", file.getAbsolutePath());
					try {
						PrintStream ps = new PrintStream(new FileOutputStream(file));
						ps.println("# sample authorized keys file");
						ps.println("# place your keys in this file");
						ps.println("# one key per line");
						ps.println("# of course without leading #");
						ps.println("# e.g.");
						ps.println("# ssh-ed25519 AAAAC3NzaC1lZDI1N....");
						ps.println("# ssh-rsa AAAAB3NzaC1yc2EAAAAD....");
					} catch (Exception e) {
						logger.debug("creation of sample file failed ({}): {}, {}",
								new String[]{file.getAbsolutePath(), e.getClass().getName(), e.getMessage()});
					}
				} else {
					logger.debug("creation of dir failed ({})", dir.getAbsolutePath());
				}
			}
		}
	}
}
