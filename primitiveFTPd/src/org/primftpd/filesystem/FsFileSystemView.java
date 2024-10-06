package org.primftpd.filesystem;

import android.content.Context;
import android.net.Uri;

import org.primftpd.services.PftpdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public abstract class FsFileSystemView<TFile extends FsFile<TMina, ? extends FsFileSystemView>, TMina> extends AbstractFileSystemView {

	private final MediaScannerClient mediaScannerClient;
	private final String safVolumePath;
	private final int safTimeResolution;

	protected abstract TFile createFile(File file);

	protected abstract String absolute(String file);

	public FsFileSystemView(PftpdService pftpdService, Uri safStartUrl) {
		super(pftpdService);
		this.mediaScannerClient = new MediaScannerClient(pftpdService.getContext());
		this.safTimeResolution = StorageManagerUtil.getFilesystemTimeResolutionForTreeUri(safStartUrl);
		this.safVolumePath = safTimeResolution != 1 ? StorageManagerUtil.getVolumePathFromTreeUri(safStartUrl, pftpdService.getContext()) : null;
	}

	public final MediaScannerClient getMediaScannerClient() {
		return mediaScannerClient;
	}

	public long getCorrectedTime(String abs, long time) {
		int timeResolution = safVolumePath != null && abs.startsWith(safVolumePath) ? safTimeResolution : 1;
		return (time / timeResolution) * timeResolution;
	}

	public TFile getFile(String file) {
		logger.trace("getFile({})", file);
		String abs = absolute(file);
		logger.trace("  getFile(abs: {})", abs);
		return createFile(new File(abs));
	}
}
