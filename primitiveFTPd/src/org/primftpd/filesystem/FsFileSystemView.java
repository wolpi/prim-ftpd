package org.primftpd.filesystem;

import org.primftpd.events.ClientActionPoster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public abstract class FsFileSystemView<T extends FsFile<X>, X> {

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	protected final ClientActionPoster clientActionPoster;

	protected abstract T createFile(File file, ClientActionPoster clientActionPoster);

	protected abstract String absolute(String file);

	public FsFileSystemView(ClientActionPoster clientActionPoster) {
		this.clientActionPoster = clientActionPoster;
	}

	public T getFile(String file) {
		logger.trace("getFile({})", file);
		String abs = absolute(file);
		logger.trace("  getFile(abs: {})", file);
		return createFile(new File(abs), clientActionPoster);
	}
}
