/*
 * Created 21.10.2009
 *
 * Copyright (c) 2009 SLF4J.ORG
 *
 * All rights reserved.
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.slf4j.impl;

import org.slf4j.Marker;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

import android.util.Log;

/**
 * A simple implementation that delegates all log requests to the Google Android
 * logging facilities. Note that this logger does not support {@link Marker}.
 * That is, methods taking marker data simply invoke the corresponding method
 * without the Marker argument, discarding any marker data passed as argument.
 * <p>
 * The logging levels specified for SLF4J can be almost directly mapped to
 * the levels that exist in the Google Android platform. The following table
 * shows the mapping implemented by this logger.
 * <p>
 * <table border="1">
 * 	<tr><th><b>SLF4J<b></th><th><b>Android</b></th></tr>
 * 	<tr><td>TRACE</td><td>{@link Log#VERBOSE}</td></tr>
 * 	<tr><td>DEBUG</td><td>{@link Log#DEBUG}</td></tr>
 * 	<tr><td>INFO</td><td>{@link Log#INFO}</td></tr>
 * 	<tr><td>WARN</td><td>{@link Log#WARN}</td></tr>
 * 	<tr><td>ERROR</td><td>{@link Log#ERROR}</td></tr>
 * </table>
 *
 * @author Thorsten M&ouml;ller
 * @version $Rev:$; $Author:$; $Date:$
 */
public class AndroidLogger extends MarkerIgnoringBase
{
	private static final long serialVersionUID = -1227274521521287937L;

	/**
	 * Package access allows only {@link AndroidLoggerFactory} to instantiate
	 * SimpleLogger instances.
	 */
	AndroidLogger(final String name)
	{
		this.name = name;
	}

	/* @see org.slf4j.Logger#isTraceEnabled() */
	public boolean isTraceEnabled()
	{
		return Log.isLoggable(name, Log.VERBOSE);
	}

	/* @see org.slf4j.Logger#trace(java.lang.String) */
	public void trace(final String msg)
	{
		Log.v(name, msg);
	}

	/* @see org.slf4j.Logger#trace(java.lang.String, java.lang.Object) */
	public void trace(final String format, final Object param1)
	{
		Log.v(name, format(format, param1, null));
	}

	/* @see org.slf4j.Logger#trace(java.lang.String, java.lang.Object, java.lang.Object) */
	public void trace(final String format, final Object param1, final Object param2)
	{
		Log.v(name, format(format, param1, param2));
	}

	/* @see org.slf4j.Logger#trace(java.lang.String, java.lang.Object[]) */
	public void trace(final String format, final Object[] argArray)
	{
		Log.v(name, format(format, argArray));
	}

	/* @see org.slf4j.Logger#trace(java.lang.String, java.lang.Throwable) */
	public void trace(final String msg, final Throwable t)
	{
		Log.v(name, msg, t);
	}

	/* @see org.slf4j.Logger#isDebugEnabled() */
	public boolean isDebugEnabled()
	{
		return Log.isLoggable(name, Log.DEBUG);
	}

	/* @see org.slf4j.Logger#debug(java.lang.String) */
	public void debug(final String msg)
	{
		Log.d(name, msg);
	}

	/* @see org.slf4j.Logger#debug(java.lang.String, java.lang.Object) */
	public void debug(final String format, final Object arg1)
	{
		Log.d(name, format(format, arg1, null));
	}

	/* @see org.slf4j.Logger#debug(java.lang.String, java.lang.Object, java.lang.Object) */
	public void debug(final String format, final Object param1, final Object param2)
	{
		Log.d(name, format(format, param1, param2));
	}

	/* @see org.slf4j.Logger#debug(java.lang.String, java.lang.Object[]) */
	public void debug(final String format, final Object[] argArray)
	{
		Log.d(name, format(format, argArray));
	}

	/* @see org.slf4j.Logger#debug(java.lang.String, java.lang.Throwable) */
	public void debug(final String msg, final Throwable t)
	{
		Log.d(name, msg, t);
	}

	/* @see org.slf4j.Logger#isInfoEnabled() */
	public boolean isInfoEnabled()
	{
		return Log.isLoggable(name, Log.INFO);
	}

	/* @see org.slf4j.Logger#info(java.lang.String) */
	public void info(final String msg)
	{
		Log.i(name, msg);
	}

	/* @see org.slf4j.Logger#info(java.lang.String, java.lang.Object) */
	public void info(final String format, final Object arg)
	{
		Log.i(name, format(format, arg, null));
	}

	/* @see org.slf4j.Logger#info(java.lang.String, java.lang.Object, java.lang.Object) */
	public void info(final String format, final Object arg1, final Object arg2)
	{
		Log.i(name, format(format, arg1, arg2));
	}

	/* @see org.slf4j.Logger#info(java.lang.String, java.lang.Object[]) */
	public void info(final String format, final Object[] argArray)
	{
		Log.i(name, format(format, argArray));
	}

	/* @see org.slf4j.Logger#info(java.lang.String, java.lang.Throwable) */
	public void info(final String msg, final Throwable t)
	{
		Log.i(name, msg, t);
	}

	/* @see org.slf4j.Logger#isWarnEnabled() */
	public boolean isWarnEnabled()
	{
		return Log.isLoggable(name, Log.WARN);
	}

	/* @see org.slf4j.Logger#warn(java.lang.String) */
	public void warn(final String msg)
	{
		Log.w(name, msg);
	}

	/* @see org.slf4j.Logger#warn(java.lang.String, java.lang.Object) */
	public void warn(final String format, final Object arg)
	{
		Log.w(name, format(format, arg, null));
	}

	/* @see org.slf4j.Logger#warn(java.lang.String, java.lang.Object, java.lang.Object) */
	public void warn(final String format, final Object arg1, final Object arg2)
	{
		Log.w(name, format(format, arg1, arg2));
	}

	/* @see org.slf4j.Logger#warn(java.lang.String, java.lang.Object[]) */
	public void warn(final String format, final Object[] argArray)
	{
		Log.w(name, format(format, argArray));
	}

	/* @see org.slf4j.Logger#warn(java.lang.String, java.lang.Throwable) */
	public void warn(final String msg, final Throwable t)
	{
		Log.w(name, msg, t);
	}

	/* @see org.slf4j.Logger#isErrorEnabled() */
	public boolean isErrorEnabled()
	{
		return Log.isLoggable(name, Log.ERROR);
	}

	/* @see org.slf4j.Logger#error(java.lang.String) */
	public void error(final String msg)
	{
		Log.e(name, msg);
	}

	/* @see org.slf4j.Logger#error(java.lang.String, java.lang.Object) */
	public void error(final String format, final Object arg)
	{
		Log.e(name, format(format, arg, null));
	}

	/* @see org.slf4j.Logger#error(java.lang.String, java.lang.Object, java.lang.Object) */
	public void error(final String format, final Object arg1, final Object arg2)
	{
		Log.e(name, format(format, arg1, arg2));
	}

	/* @see org.slf4j.Logger#error(java.lang.String, java.lang.Object[]) */
	public void error(final String format, final Object[] argArray)
	{
		Log.e(name, format(format, argArray));
	}

	/* @see org.slf4j.Logger#error(java.lang.String, java.lang.Throwable) */
	public void error(final String msg, final Throwable t)
	{
		Log.e(name, msg, t);
	}

	/**
	 * For formatted messages substitute arguments.
	 *
	 * @param format
	 * @param arg1
	 * @param arg2
	 */
	private String format(final String format, final Object arg1, final Object arg2)
	{
		return MessageFormatter.format(format, arg1, arg2).getMessage();
	}

	/**
	 * For formatted messages substitute arguments.
	 *
	 * @param format
	 * @param args
	 */
	private String format(final String format, final Object[] args)
	{
		return MessageFormatter.arrayFormat(format, args).getMessage();
	}
}
