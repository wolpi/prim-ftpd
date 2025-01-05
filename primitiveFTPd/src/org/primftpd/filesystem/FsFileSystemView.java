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

	private final static Pattern getVolumeIdRegex;
	static {
		getVolumeIdRegex = Pattern.compile("^\\/storage\\/([\\dA-F]{4}-[\\dA-F]{4})");
	}

	public long getCorrectedTime(String abs, long time) {
		Matcher matcher = getVolumeIdRegex.matcher(abs);
		if (matcher.matches()) {
			String volumeId = matcher.group(1);
			int timeResolution = StorageManagerUtil.getFilesystemTimeResolutionForVolumeId(volumeId);
			return (time / timeResolution) * timeResolution;
		} else {
			return time;
		}
	}

	public TFile getFile(String file) {
		logger.trace("getFile({})", file);
		String abs = absolute(file);
		logger.trace("  getFile(abs: {})", abs);
		return createFile(new File(abs));
	}
}
