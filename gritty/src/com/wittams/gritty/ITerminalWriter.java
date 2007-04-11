package com.wittams.gritty;

import java.awt.Dimension;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.log4j.Logger;

import com.wittams.gritty.Term.ResizeOrigin;

interface ITerminalWriter {

	void startIteration();

	void writeDoubleByte(byte[] bytesOfChar) throws IOException,
			UnsupportedEncodingException;

	void writeASCII(byte[] bytes, int off, int length)
			throws IOException;

	void scrollY();

	void newLine();

	void backspace();

	void carriageReturn();

	void horizontalTab();

	void eraseInDisplay(ControlSequence args);

	void eraseInLine(ControlSequence args);

	void cursorUp(ControlSequence args);

	void cursorDown(ControlSequence args);

	void cursorForward(ControlSequence args);

	void cursorBackward(ControlSequence args);

	void cursorPosition(ControlSequence args);

	void fillScreen(char c);
	void clearScreen();
	
	void index();

	void nextLine();

	void reverseIndex();

	void setScrollingRegion(ControlSequence args);

	void setCharacterAttributes(ControlSequence args);

	void reset();

	void beep();

	int distanceToLineEnd();

	void storeCursor(StoredCursor storedCursor);

	void restoreCursor(StoredCursor storedCursor);
	
	Dimension resize(Dimension pendingResize, ResizeOrigin origin);

	static class Util {
		private Util(){}
		
		public static ITerminalWriter wrapWithLogging(final ITerminalWriter wrapped) {
			final Logger logger = Logger.getLogger(wrapped.getClass());
			return (ITerminalWriter) Proxy.newProxyInstance(
					Thread.currentThread().getContextClassLoader(),
					new Class[] { ITerminalWriter.class },
					new InvocationHandler() {
						public Object invoke(final Object proxy, final Method method,
								final Object[] args) throws Throwable {
							Object result;
							try {
								StringBuffer sb = new StringBuffer();
								sb.append(String.format("%27s:", method.getName()));
								if (args != null && args.length > 0)
									if (args[0] instanceof ControlSequence)
										((ControlSequence) args[0]).appendToBuffer(sb);
									else if (method.getName().equals(
											"writeASCII")) {
										final byte[] bytes = (byte[]) args[0];
										final Integer off = (Integer) args[1];
										final Integer len = (Integer) args[2];
										
										sb
										.append("*|")
										.append(new String(bytes, off, len))
										.append("|*");
									}
								
								logger.info(sb);
								
								result = method.invoke(wrapped, args);
							} catch (final InvocationTargetException e) {
								throw e.getTargetException();
							} 
							return result;
						}
					});
		}
	}



}