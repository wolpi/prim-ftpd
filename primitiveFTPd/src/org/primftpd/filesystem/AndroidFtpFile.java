package org.primftpd.filesystem;

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
import java.util.List;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AndroidFtpFile implements FtpFile {

	private static final Logger logger = LoggerFactory.getLogger(AndroidFtpFile.class);

	private final File file;
	private final User user;

	public AndroidFtpFile(File file, User user) {
		super();
		this.file = file;
		this.user = user;
	}

	@Override
	public String getAbsolutePath() {
		logger.debug("getAbsolutePath()");
		return file.getAbsolutePath();
	}

	@Override
	public String getName() {
		logger.debug("getName()");
		return file.getName();
	}

	@Override
	public boolean isHidden() {
		logger.debug("isHidden()");
		return file.isHidden();
	}

	@Override
	public boolean isDirectory() {
		logger.debug("isDirectory()");
		return file.isDirectory();
	}

	@Override
	public boolean isFile() {
		logger.debug("isFile()");
		return file.isFile();
	}

	@Override
	public boolean doesExist() {
		logger.debug("doesExist(), ({})", file.getAbsolutePath());
		return file.exists();
	}

	@Override
	public boolean isReadable() {
		logger.debug("isReadable()");
		return file.canRead();
	}

	@Override
	public boolean isWritable() {
		logger.debug(
			"writable: {}, exists: {}, file: '{}'",
			new Object[]{
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

	@Override
	public boolean isRemovable() {
		logger.debug("isRemovable()");
		return file.canWrite();
	}

	@Override
	public String getOwnerName() {
		logger.debug("getOwnerName()");
		return user.getName();
	}

	@Override
	public String getGroupName() {
		logger.debug("getGroupName()");
		return user.getName();
	}

	@Override
	public int getLinkCount() {
		logger.debug("getLinkCount()");
		return 0;
	}

	@Override
	public long getLastModified() {
		logger.debug("getLastModified()");
		return file.lastModified();
	}

	@Override
	public boolean setLastModified(long time) {
		logger.debug("setLastModified({})", time);
		return file.setLastModified(time);
	}

	@Override
	public long getSize() {
		logger.debug("getSize()");
		return file.length();
	}

	@Override
	public boolean mkdir() {
		logger.debug("mkdir()");
		return file.mkdir();
	}

	@Override
	public boolean delete() {
		logger.debug("delete()");
		return file.delete();
	}

	@Override
	public boolean move(FtpFile destination) {
		logger.debug("move({})", destination.getAbsolutePath());
		file.renameTo(new File(destination.getAbsolutePath()));
		return true;
	}

	@Override
	public List<FtpFile> listFiles() {
		logger.debug("listFiles()");
		File[] filesArray = file.listFiles();
		if (filesArray != null) {
			List<FtpFile> files = new ArrayList<FtpFile>(filesArray.length);
			for (File file : filesArray) {
				files.add(new AndroidFtpFile(file, user));
			}
			return files;
		}
		logger.debug("file.listFiles() returned null. Path: {}", file.getAbsolutePath());
		return new ArrayList<FtpFile>(0);
	}

	public static final int BUFFER_SIZE = 1024 * 1024;

	@Override
	public OutputStream createOutputStream(long offset) throws IOException {
		logger.debug("createOutputStream({})", offset);

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

		BufferedOutputStream bos = new BufferedOutputStream(os, BUFFER_SIZE);
		return bos;
	}

	@Override
	public InputStream createInputStream(long offset) throws IOException {
		logger.debug("createInputStream(), offset: {}, file: {}", offset, file.getAbsolutePath());
		FileInputStream fis = new FileInputStream(file);
		fis.skip(offset);
		BufferedInputStream bis = new BufferedInputStream(fis, BUFFER_SIZE);
		return bis;
	}

	public File getFile() {
		return file;
	}

	public User getUser() {
		return user;
	}

}
