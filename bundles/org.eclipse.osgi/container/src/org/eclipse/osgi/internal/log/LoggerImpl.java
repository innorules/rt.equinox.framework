/*******************************************************************************
 * Copyright (c) 2006, 2012 Cognos Incorporated, IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.osgi.internal.log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.equinox.log.Logger;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogLevel;

public class LoggerImpl implements Logger {

	protected final ExtendedLogServiceImpl logServiceImpl;
	protected final String name;

	public LoggerImpl(ExtendedLogServiceImpl logServiceImpl, String name) {
		this.logServiceImpl = logServiceImpl;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public boolean isLoggable(int level) {
		return logServiceImpl.isLoggable(name, level);
	}

	public void log(int level, String message) {
		log(null, level, message, null);
	}

	public void log(int level, String message, Throwable exception) {
		log(null, level, message, exception);
	}

	@SuppressWarnings("rawtypes")
	public void log(ServiceReference sr, int level, String message) {
		log(sr, level, message, null);
	}

	@SuppressWarnings("rawtypes")
	public void log(ServiceReference sr, int level, String message, Throwable exception) {
		logServiceImpl.log(name, sr, level, message, exception);
	}

	public void log(Object context, int level, String message) {
		log(context, level, message, null);
	}

	public void log(Object context, int level, String message, Throwable exception) {
		logServiceImpl.log(name, context, level, message, exception);
	}

	@Override
	public boolean isTraceEnabled() {
		return isLoggable(LogLevel.TRACE.ordinal());
	}

	@Override
	public void trace(String message) {
		trace(message, (Object) null);
	}

	@Override
	public void trace(String format, Object arg) {
		trace(format, arg, null);
	}

	@Override
	public void trace(String format, Object arg1, Object arg2) {
		trace(format, new Object[] {arg1, arg2});
	}

	@Override
	public void trace(String format, Object... arguments) {
		log(LogLevel.TRACE, format, arguments);
	}

	@Override
	public boolean isDebugEnabled() {
		return isLoggable(LogLevel.DEBUG.ordinal());
	}

	@Override
	public void debug(String message) {
		debug(message, (Object) null);
	}

	@Override
	public void debug(String format, Object arg) {
		debug(format, arg, null);
	}

	@Override
	public void debug(String format, Object arg1, Object arg2) {
		debug(format, new Object[] {arg1, arg2});
	}

	@Override
	public void debug(String format, Object... arguments) {
		log(LogLevel.DEBUG, format, arguments);
	}

	@Override
	public boolean isInfoEnabled() {
		return isLoggable(LogLevel.INFO.ordinal());
	}

	@Override
	public void info(String message) {
		info(message, (Object) null);
	}

	@Override
	public void info(String format, Object arg) {
		info(format, arg, null);
	}

	@Override
	public void info(String format, Object arg1, Object arg2) {
		info(format, new Object[] {arg1, arg2});
	}

	@Override
	public void info(String format, Object... arguments) {
		log(LogLevel.INFO, format, arguments);
	}

	@Override
	public boolean isWarnEnabled() {
		return isLoggable(LogLevel.WARN.ordinal());
	}

	@Override
	public void warn(String message) {
		warn(message, (Object) null);
	}

	@Override
	public void warn(String format, Object arg) {
		warn(format, arg, null);
	}

	@Override
	public void warn(String format, Object arg1, Object arg2) {
		warn(format, new Object[] {arg1, arg2});
	}

	@Override
	public void warn(String format, Object... arguments) {
		log(LogLevel.WARN, format, arguments);
	}

	@Override
	public boolean isErrorEnabled() {
		return isLoggable(LogLevel.ERROR.ordinal());
	}

	@Override
	public void error(String message) {
		error(message, (Object) null);
	}

	@Override
	public void error(String format, Object arg) {
		error(format, arg, null);
	}

	@Override
	public void error(String format, Object arg1, Object arg2) {
		error(format, new Object[] {arg1, arg2});
	}

	@Override
	public void error(String format, Object... arguments) {
		log(LogLevel.ERROR, format, arguments);
	}

	@Override
	public void audit(String message) {
		audit(message, (Object) null);
	}

	@Override
	public void audit(String format, Object arg) {
		audit(format, arg, null);
	}

	@Override
	public void audit(String format, Object arg1, Object arg2) {
		audit(format, new Object[] {arg1, arg2});
	}

	@Override
	public void audit(String format, Object... arguments) {
		log(LogLevel.AUDIT, format, arguments);
	}

	private static final Pattern pattern = Pattern.compile("(\\\\?)(\\\\?)(\\{\\})"); //$NON-NLS-1$

	protected void log(LogLevel level, String format, Object... arguments) {
		Arguments processedArguments = new Arguments(arguments);
		if (processedArguments.isEmpty()) {
			logServiceImpl.log(name, processedArguments.serviceReference(), level.ordinal(), format, processedArguments.throwable());
			return;
		}
		Matcher matcher = pattern.matcher(format);
		char[] chars = format.toCharArray();
		int offset = 0;
		StringBuilder message = new StringBuilder(format.length() * 2);
		for (Object argument : processedArguments.arguments()) {
			// By design, the message will continue to be formatted for as long as arguments remain.
			// Once all arguments have been processed, formatting stops. This means some escape characters
			// and place holders may remain unconsumed. This matches SLF4J behavior.
			while (matcher.find()) {
				if (matcher.group(2).isEmpty()) {
					if (matcher.group(1).isEmpty()) {
						// {} Handle curly brackets as normal.
						offset = append(message, matcher, chars, offset, matcher.start(3) - offset, argument);
						break;
					}
					// \{} Ignore curly brackets. Consume backslash.
					offset = append(message, matcher, chars, offset, matcher.start(3) - offset - 1, matcher.group(3));
				} else {
					// \\{} Handle curly brackets as normal. Consume one backslash.
					offset = append(message, matcher, chars, offset, matcher.start(3) - offset - 1, argument);
					break;
				}
			}
		}
		message.append(chars, offset, chars.length - offset);
		logServiceImpl.log(name, processedArguments.serviceReference(), level.ordinal(), message.toString(), processedArguments.throwable());
	}

	private static int append(StringBuilder builder, Matcher matcher, char[] chars, int offset, int length, Object argument) {
		builder.append(chars, offset, length);
		builder.append(argument);
		return matcher.end(3);
	}
}
