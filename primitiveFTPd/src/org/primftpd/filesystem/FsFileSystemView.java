package org.primftpd.filesystem;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.primftpd.services.PftpdService;

public abstract class FsFileSystemView<
			TFile extends FsFile<TMina, ? extends FsFileSystemView>, TMina>
		extends AbstractFileSystemView {

	private final MediaScannerClient mediaScannerClient;

	protected abstract TFile createFile(File file);

	protected abstract String absolute(String file);

	public FsFileSystemView(PftpdService pftpdService) {
		super(pftpdService);
		this.mediaScannerClient = new MediaScannerClient(pftpdService.getContext());
	}

	public final MediaScannerClient getMediaScannerClient() {
		return mediaScannerClient;
	}

	private final static Pattern isVolumeIdRegex;
	static {
		// Android regex is completely broken, the below pattern is not matched if the string is longer, this is against even the ICU regex spec
		// getVolumeIdRegex = Pattern.compile("^\\/storage\\/([\\dA-F]{4}-[\\dA-F]{4})");
		isVolumeIdRegex = Pattern.compile("^[\\dA-F]{4}-[\\dA-F]{4}$");
	}

	public long getCorrectedTime(String abs, long time) {
		if (18 <= abs.length() && abs.startsWith("/storage/")) {
			String volumeId = abs.substring(9, 18);
			if (isVolumeIdRegex.matcher(volumeId).matches()) {
				int timeResolution = StorageManagerUtil.getFilesystemTimeResolutionForVolumeId(volumeId);
				return (time / timeResolution) * timeResolution;
			}
		}
		return time;
	}

	public TFile getFile(String file) {
		logger.trace("getFile({})", file);
		String abs = absolute(file);
		logger.trace("  getFile(abs: {})", abs);
		return createFile(new File(abs));
	}
}
