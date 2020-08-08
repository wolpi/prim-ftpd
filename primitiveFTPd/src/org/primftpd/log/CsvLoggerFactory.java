package org.primftpd.log;

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.primftpd.util.Defaults;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import android.content.Context;

public class CsvLoggerFactory implements ILoggerFactory
{
	public static Context CONTEXT;

	public static final String LOGFILE_BASENAME = "prim-ftpd-log-";
	private static final int NUM_LOGFILES_TO_KEEP = 3;
	private static DateFormat FILENAME_DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

	private final Map<String, Logger> loggerMap = new HashMap<String, Logger>();

	private final ILoggerFactory nopLoggerFactory;

	private PrintStream file;

	public CsvLoggerFactory(ILoggerFactory nopLoggerFactory) {
		this.nopLoggerFactory = nopLoggerFactory;
	}

	@Override
	public Logger getLogger(String name)
	{
		Logger logger = null;
		try {
			// protect against concurrent access of the loggerMap
			synchronized (this)
			{
				logger = loggerMap.get(name);
				if (logger == null)
				{
					if (file == null) {
						file = findFile();
					}

					logger = new CsvLogger(name, file);
					loggerMap.put(name, logger);
				}
			}
		} catch (FileNotFoundException e) {
			logger = nopLoggerFactory.getLogger(name);
		}
		return logger;
	}

	private PrintStream findFile() throws FileNotFoundException {
		File dir = CONTEXT != null
				? Defaults.homeDirScoped(CONTEXT)
				: Defaults.HOME_DIR;
		cleanOldFiles(dir);

		String filename = LOGFILE_BASENAME + FILENAME_DATEFORMAT.format(new Date()) + ".csv";
		File file = new File(dir, filename);
		// note: this is never closed
		return new PrintStream(new FileOutputStream(file, true));
	}

	private void cleanOldFiles(File dir) {
		File[] currentFiles = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(LOGFILE_BASENAME);
			}
		});
		if (currentFiles == null) {
			return;
		}
		if (currentFiles.length >= NUM_LOGFILES_TO_KEEP) {
			SortedSet<File> sorted = new TreeSet<File>(new Comparator<File>() {
				@Override
				public int compare(File file1, File file2) {
					Long time1 = Long.valueOf(file1.lastModified());
					Long time2 = Long.valueOf(file2.lastModified());
					return time1.compareTo(time2);
				}
			});
			for (File file : currentFiles) {
				sorted.add(file);
			}

			int i = currentFiles.length;
			Iterator<File> iterator = sorted.iterator();
			while (iterator.hasNext()) {
				if (i >= NUM_LOGFILES_TO_KEEP) {
					iterator.next().delete();
				} else {
					break;
				}
				i--;
			}
		}
	}
}
