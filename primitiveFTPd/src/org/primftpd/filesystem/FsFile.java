package org.primftpd.filesystem;

import org.primftpd.events.ClientActionEvent;
import org.primftpd.services.PftpdService;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class FsFile<T> extends AbstractFile {

	protected final File file;
	protected final FsFileSystemView fileSystemView;
	protected final boolean injectedDirectory;

	private final static Map<String, String[]> DIRECTORY_INJECTIONS;
	static {
		Map<String, String[]> tmp = new HashMap<>();
		// more known directories might be added
		//tmp.put("/", new String[] {"dev", "etc", "mnt", "proc", "product", "sdcard", "storage", "system", "vendor"});
		tmp.put("/", new String[] {"storage"});
		tmp.put("/storage/emulated", new String[] {"0"});
		DIRECTORY_INJECTIONS = Collections.unmodifiableMap(tmp);
	}

	private final static Set<String> INJECTIONS_AND_CHILDREN;
	static {
		Set<String> tmp = new HashSet<>();
		for (Map.Entry<String, String[]> entry : DIRECTORY_INJECTIONS.entrySet()) {
			String k = entry.getKey();
			tmp.add(k);
			for (String v : entry.getValue()) {
				tmp.add(k + File.separator + v);
			}
		}
		INJECTIONS_AND_CHILDREN = Collections.unmodifiableSet(tmp);
	}

	public FsFile(File file, PftpdService pftpdService, FsFileSystemView fileSystemView) {
		super(
				file.getAbsolutePath(),
				file.getName(),
				correctTime(fileSystemView, file.getAbsolutePath(), file.lastModified()),
				file.length(),
				file.canRead(),
				file.exists(),
				file.isDirectory(),
				pftpdService);
		this.file = file;
		this.name = file.getName();
		this.fileSystemView = fileSystemView;
		this.injectedDirectory = file.isDirectory() && INJECTIONS_AND_CHILDREN.contains(file.getAbsolutePath());
	}

	private static long correctTime(FsFileSystemView fileSystemView, String abs, long time) {
		int timeResolution = fileSystemView.getTimeResolution(abs);
		return (time / timeResolution) * timeResolution;
	}

	protected abstract T createFile(File file, PftpdService pftpdService);

	@Override
	public ClientActionEvent.Storage getClientActionStorage() {
		return ClientActionEvent.Storage.FS;
	}

	public boolean isFile() {
		boolean isFile = file.isFile();
		logger.trace("[{}] isFile() -> {}", name, isFile);
		return isFile;
	}

	public boolean doesExist() {
		boolean exists = file.exists();
		boolean existsChecked = exists;
		if (!exists) {
			// exists may be false when we don't have read permission
			// try to figure out if it really does not exist
			File parentFile = file.getParentFile();
			File[] children = parentFile.listFiles();
			if (children != null) {
				for (File child : children) {
					if (file.equals(child)) {
						existsChecked = true;
						break;
					}
				}
			}
		}
		logger.trace(
			"[{}] doesExist(), ({}): orig val: {}, checked val: {}",
			new Object[]{
				name,
				file.getAbsolutePath(),
				Boolean.valueOf(exists),
				Boolean.valueOf(existsChecked)
			});
		return existsChecked;
	}

	public boolean isReadable() {
		boolean canRead = injectedDirectory || file.canRead();
		logger.trace("[{}] isReadable() -> {}", name, canRead);
		return canRead;
	}

	public boolean isWritable() {
		logger.trace("[{}] writable: {}, exists: {}, file: '{}'",
			new Object[]{
				name,
				file.canWrite(),
				file.exists(),
				file.getName()
			});

		if (file.exists()) {
			return file.canWrite();
		}

		// file does not exist, probably an upload of a new file, check parent
		// must be done in loop as some clients to not issue mkdir commands
		// like filezilla
		File parent = file.getParentFile();
		while (parent != null) {
			if (parent.exists()) {
				return parent.canWrite();
			}
			parent = parent.getParentFile();
		}
		return false;
	}

	public boolean isRemovable() {
		boolean result = file.canWrite();
		logger.trace("[{}] isRemovable() -> {}", name, result);
		return result;
	}

	public boolean setLastModified(long time) {
		logger.trace("[{}] setLastModified({})", name, Long.valueOf(time));
		long correctedTime = correctTime(fileSystemView, absPath, time);
		return file.setLastModified(correctedTime);
	}

	public boolean mkdir() {
		logger.trace("[{}] mkdir()", name);
		postClientAction(ClientActionEvent.ClientAction.CREATE_DIR);
		return file.mkdir();
	}

	public boolean delete() {
		logger.trace("[{}] delete()", name);
		postClientAction(ClientActionEvent.ClientAction.DELETE);
		return file.delete();
	}

	public boolean move(FsFile<T> destination) {
		logger.trace("[{}] move({})", name, destination.getAbsolutePath());
		postClientAction(ClientActionEvent.ClientAction.RENAME);
		boolean success = file.renameTo(new File(destination.getAbsolutePath()));
		if (success) {
			Utils.mediaScanFile(pftpdService.getContext(), getAbsolutePath());
		}
		return success;
	}

	public List<T> listFiles() {
		logger.trace("[{}] listFiles()", name);
		postClientAction(ClientActionEvent.ClientAction.LIST_DIR);
		File[] filesArray = file.listFiles();

		// if the OS did not provide child elements:
		if (filesArray == null) {
			// check if requested file is among injected ones
			String[] folders = DIRECTORY_INJECTIONS.get(file.getAbsolutePath());
			if (folders != null) {
				filesArray = new File[folders.length];
				for (int i = 0; i < folders.length; i++) {
					filesArray[i] = new File(file.getAbsolutePath() + File.separator + folders[i]);
				}
			}
		}

		if (filesArray != null) {
			List<T> files = new ArrayList<>(filesArray.length);
			for (File file : filesArray) {
				files.add(createFile(file, pftpdService));
			}
			return files;
		}
		logger.debug("file.listFiles() returned null. Path: {}", file.getAbsolutePath());
		return new ArrayList<>(0);
	}

	public OutputStream createOutputStream(long offset) throws IOException {
		logger.trace("[{}] createOutputStream({})", name, offset);
		postClientAction(ClientActionEvent.ClientAction.UPLOAD);

		// may be necessary to create dirs
		// see isWritable()
		File parent = file.getParentFile();
		if (!parent.exists()) {
			parent.mkdirs();
		}

		// now create out stream
		OutputStream os = null;
		if (offset == 0) {
			os = new FileOutputStream(file);
		} else if (offset == this.file.length()) {
			os = new FileOutputStream(file, true);
		} else {
			final RandomAccessFile raf = new RandomAccessFile(this.file, "rw");
			raf.seek(offset);
			os = new OutputStream() {
				@Override
				public void write(int oneByte) throws IOException {
					raf.write(oneByte);
				}
				@Override
				public void close() throws IOException {
					raf.close();
				}
			};
		}

		return new BufferedOutputStream(os) {
			@Override
			public void close() throws IOException {
				super.close();
				Utils.mediaScanFile(pftpdService.getContext(), getAbsolutePath());
			}
		};
	}

	public InputStream createInputStream(long offset) throws IOException {
		logger.trace("[{}] createInputStream(), offset: {}, file: {}",
				new Object []{
						name,
						offset,
						file.getAbsolutePath()
		});
		postClientAction(ClientActionEvent.ClientAction.DOWNLOAD);

		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file), TracingBufferedOutputStream.BUFFER_SIZE);
		bis.skip(offset);
		return bis;
	}
}
