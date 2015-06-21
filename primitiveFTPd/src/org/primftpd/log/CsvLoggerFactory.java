package org.primftpd.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import android.os.Environment;

public class CsvLoggerFactory implements ILoggerFactory
{
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
						file = openFile();
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

	private PrintStream openFile() throws FileNotFoundException {
		File dir = Environment.getExternalStorageDirectory();
		File file = new File(dir, "prim-ftpd-log.csv");
		// note: this is never closed
		return new PrintStream(new FileOutputStream(file, true));
	}
}
