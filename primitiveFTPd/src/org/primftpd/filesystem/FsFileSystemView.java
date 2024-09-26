package org.primftpd.filesystem;

import android.content.Context;
import android.net.Uri;

import org.primftpd.services.PftpdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public abstract class FsFileSystemView<T extends FsFile<X>, X> {

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	private final String safVolumePath;
	private final int safTimeResolution;
	protected final PftpdService pftpdService;

	protected abstract T createFile(File file, PftpdService pftpdService);

	protected abstract String absolute(String file);

	public FsFileSystemView(Context context, Uri safStartUrl, PftpdService pftpdService) {
        this.safTimeResolution = StorageManagerUtil.getFilesystemTimeResolutionForTreeUri(safStartUrl);
		this.safVolumePath = safTimeResolution != 1 ? StorageManagerUtil.getVolumePathFromTreeUri(safStartUrl, context) : null;
		this.pftpdService = pftpdService;
	}

	public int getTimeResolution(String abs) {
		return safVolumePath != null && abs.startsWith(safVolumePath) ? safTimeResolution : 1;
	}

	public T getFile(String file) {
		logger.trace("getFile({})", file);
		String abs = absolute(file);
		logger.trace("  getFile(abs: {})", abs);
		return createFile(new File(abs), pftpdService);
	}
}
