package org.primftpd.log;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

public class CsvLogger extends MarkerIgnoringBase
{
	private static final long serialVersionUID = 3245106428450758061L;

	private static final int MAX_MSG_LEN = 100;

	private static final DateFormat DATE_FORMAT =
		new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.S", Locale.GERMAN);

	private final String name;
	private final PrintStream file;

	public CsvLogger(String name, PrintStream file) {
		this.name = name;
		this.file = file;
	}

	private void writeLine(LogLevel logLevel, String msg) {
		writeLine(logLevel, msg, null);
	}
	private void writeLine(LogLevel logLevel, String msg, Throwable t) {
		StringBuilder sb = new StringBuilder();
		sb.append("\"");
		sb.append(DATE_FORMAT.format(new Date()));
		sb.append("\"");
		sb.append(";");
		sb.append("\"");
		sb.append(logLevel);
		sb.append("\"");
		sb.append(";");
		sb.append("\"");
		sb.append(name);
		sb.append("\"");
		sb.append(";");
		sb.append("\"");
		if (msg.length() > MAX_MSG_LEN) {
			sb.append(msg, 0, MAX_MSG_LEN - 3);
			sb.append("...");
		} else {
			sb.append(msg);
		}
		sb.append("\"");
		sb.append(";");
		sb.append("\"");
		if (t != null) {
			printThrowable(sb, t);
		}
		sb.append("\"");
		sb.append(";");
		file.println(sb.toString());
	}

	private void printThrowable(StringBuilder sb, Throwable t) {
		sb.append(t.getClass().getName());
		sb.append(": ");
		sb.append(t.getMessage());
		StackTraceElement[] stackTrace = t.getStackTrace();
		for (StackTraceElement elem : stackTrace) {
			sb.append("\n\t");
			sb.append(elem.getClassName());
			sb.append(".");
			sb.append(elem.getMethodName());
			sb.append("() line: ");
			sb.append(elem.getLineNumber());
		}
		Throwable cause = t.getCause();
        if (cause != null) {
        	sb.append("\n");
        	sb.append("Caused by:");
        	sb.append(" ");
        	printThrowable(sb, cause);
        }
	}

	@Override
	public boolean isTraceEnabled()
	{
		return true;
	}

	@Override
	public void trace(String msg)
	{
		writeLine(LogLevel.TRACE, msg);
	}

	@Override
	public void trace(String format, Object arg)
	{
		trace(format(format, arg, null));
	}

	@Override
	public void trace(String format, Object arg1, Object arg2)
	{
		trace(format(format, arg1, arg2));
	}

	@Override
	public void trace(String format, Object[] argArray)
	{
		trace(format(format, argArray));
	}

	@Override
	public void trace(String msg, Throwable t)
	{
		writeLine(LogLevel.TRACE, msg, t);
	}

	@Override
	public boolean isDebugEnabled()
	{
		return true;
	}

	@Override
	public void debug(String msg)
	{
		writeLine(LogLevel.DEBUG, msg);
	}

	@Override
	public void debug(String format, Object arg)
	{
		debug(format(format, arg, null));
	}

	@Override
	public void debug(String format, Object arg1, Object arg2)
	{
		debug(format(format, arg1, arg2));
	}

	@Override
	public void debug(String format, Object[] argArray)
	{
		debug(format(format, argArray));
	}

	@Override
	public void debug(String msg, Throwable t)
	{
		writeLine(LogLevel.DEBUG, msg, t);
	}

	@Override
	public boolean isInfoEnabled()
	{
		return true;
	}

	@Override
	public void info(String msg)
	{
		writeLine(LogLevel.INFO, msg);
	}

	@Override
	public void info(String format, Object arg)
	{
		info(format(format, arg, null));
	}

	@Override
	public void info(String format, Object arg1, Object arg2)
	{
		info(format(format, arg1, arg2));
	}

	@Override
	public void info(String format, Object[] argArray)
	{
		info(format(format, argArray));
	}

	@Override
	public void info(String msg, Throwable t)
	{
		writeLine(LogLevel.INFO, msg, t);
	}

	@Override
	public boolean isWarnEnabled()
	{
		return true;
	}

	@Override
	public void warn(String msg)
	{
		writeLine(LogLevel.WARN, msg);
	}

	@Override
	public void warn(String format, Object arg)
	{
		warn(format(format, arg, null));
	}

	@Override
	public void warn(String format, Object[] argArray)
	{
		warn(format(format, argArray));
	}

	@Override
	public void warn(String format, Object arg1, Object arg2)
	{
		warn(format(format, arg1, arg2));
	}

	@Override
	public void warn(String msg, Throwable t)
	{
		writeLine(LogLevel.WARN, msg, t);
	}

	@Override
	public boolean isErrorEnabled()
	{
		return true;
	}

	@Override
	public void error(String msg)
	{
		writeLine(LogLevel.ERROR, msg);
	}

	@Override
	public void error(String format, Object arg)
	{
		error(format(format, arg, null));
	}

	@Override
	public void error(String format, Object arg1, Object arg2)
	{
		error(format(format, arg1, arg2));
	}

	@Override
	public void error(String format, Object[] argArray)
	{
		error(format(format, argArray));
	}

	@Override
	public void error(String msg, Throwable t)
	{
		writeLine(LogLevel.ERROR, msg, t);
	}

	private String format(final String format, final Object arg1, final Object arg2)
	{
		return MessageFormatter.format(format, arg1, arg2).getMessage();
	}

	private String format(final String format, final Object[] args)
	{
		return MessageFormatter.arrayFormat(format, args).getMessage();
	}

	private static enum LogLevel {
		TRACE,
		DEBUG,
		INFO,
		WARN,
		ERROR
	}
}
