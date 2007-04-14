/**
 * 
 */
package com.wittams.gritty;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.apache.log4j.Logger;


class TerminalWriter implements ITerminalWriter {
	private static final Logger logger = Logger.getLogger(TerminalWriter.class);

	private int scrollRegionTop;

	private int scrollRegionBottom;

	private int drawStartX = 0;

	private int drawnStartY = 0;

	private int drawnWidth = 0;

	private int drawnHeight = 0;

	private int cursorX = 0;

	private int cursorY = 1;

	private final int tab = 8;

	private int termWidth = 80;

	private int termHeight = 24;

	private Term term;

	public TerminalWriter(final Term term) {
		this.term = term;

		termWidth = term.getColumnCount();
		termHeight = term.getRowCount();

		scrollRegionTop = 1;
		scrollRegionBottom = termHeight;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.jcraft.jcterm.ITerminalWriter#startIteration()
	 */
	public void startIteration() {
		drawnStartY = cursorY;
		drawStartX = cursorX;
	}

	private void startText() {
		wrapLines();
	}

	private void wrapLines() {
		if (cursorX >= termWidth) {
			cursorX = 0;
			cursorY += 1;

			// scrollY();

			drawStartX = cursorX;
			drawnStartY = cursorY;
		}
	}

	private void finishText() {
		if (drawnWidth > 0 && drawnHeight > 0)
			term.redraw(drawStartX, drawnStartY - 1, drawnWidth, drawnHeight);
		term.setCursor(cursorX, cursorY);
		scrollY();
	}

	public void writeASCII(final byte[] chosenBuffer, final int start,
			final int length) throws IOException {
		term.lock();
		try {
			startText();
			if (length != 0) {
				term.clearArea(cursorX, cursorY - 1, cursorX + length, cursorY);
				term.drawBytes(chosenBuffer, start, length, cursorX, cursorY);
			}
			cursorX += length;
			drawnWidth = length;
			drawnHeight = 1;
			finishText();
		} finally {
			term.unlock();
		}
	}

	public void writeDoubleByte(final byte[] bytesOfChar) throws IOException,
			UnsupportedEncodingException {

		term.lock();
		try {
			startText();
			term.clearArea(cursorX, cursorY - 1, cursorX + 2, cursorY);
			term.drawString(new String(bytesOfChar, 0, 2, "EUC-JP"), cursorX,
					cursorY);
			cursorX += 2;
			drawnWidth = 2;
			drawnHeight = 1;
			finishText();
		} finally {
			term.unlock();
		}
	}

	public void scrollY() {
		term.lock();
		try {
			if (cursorY > scrollRegionBottom) {
				final int dy = scrollRegionBottom - cursorY;
				cursorY = scrollRegionBottom;
				term.scrollArea(scrollRegionTop, scrollRegionBottom
						- scrollRegionTop, dy);
				term.clearArea(0, cursorY - 1, termWidth, cursorY);
				term.redraw(0, 0, termWidth, scrollRegionBottom);
				term.setCursor(cursorX, cursorY);
			}
		} finally {
			term.unlock();
		}
	}

	public void newLine() {
		term.lock();
		try {
			cursorY += 1;
			term.setCursor(cursorX, cursorY);
			scrollY(); 
		} finally {
			term.unlock();
		}
	}

	public void backspace() {
		term.lock();
		try {
			cursorX -= 1;
			if (cursorX < 0) {
				cursorY -= 1;
				cursorX = termWidth - 1;
			}
			term.setCursor(cursorX, cursorY);
		} finally {
			term.unlock();
		}
	}

	public void carriageReturn() {
		term.lock();
		try {
			cursorX = 0;
			term.setCursor(cursorX, cursorY);
		} finally {
			term.unlock();
		}
	}

	public void horizontalTab() {
		term.lock();
		try {
			cursorX = (cursorX / tab + 1) * tab;
			if (cursorX >= termWidth) {
				cursorX = 0;
				cursorY += 1;
			}
			term.setCursor(cursorX, cursorY);
		} finally {
			term.unlock();
		}
	}

	public void eraseInDisplay(final ControlSequence args) {
		// ESC [ Ps J
		term.lock();
		try {
			final int arg = args.getArg(0, 0);
			int beginY;
			int endY;

			switch (arg) {
			case 0:
				// Initial line
				if (cursorX < termWidth) {
					term.clearArea(cursorX, cursorY - 1, termWidth, cursorY);
					term.redraw(cursorX, cursorY - 1, termWidth - cursorX, 1);
				}
				// Rest
				beginY = cursorY;
				endY = termHeight;

				break;
			case 1:
				// initial line
				term.clearArea(0, cursorY - 1, cursorX + 1, cursorY);
				term.redraw(0, cursorY - 1, cursorX + 1, 1);

				beginY = 0;
				endY = cursorY - 1;
				break;
			case 2:
				beginY = 0;
				endY = termHeight;
				break;
			default:
				logger.error("Unsupported erase in display mode:" + arg);
				beginY = 1;
				endY = 1;
				break;
			}
			// Rest of lines
			if (beginY != endY)
				clearLines(beginY, endY);
			term.drawCursor();
		} finally {
			term.unlock();
		}
	}

	public void clearLines(final int beginY, final int endY) {
		term.lock();
		try {
			term.clearArea(0, beginY, termWidth, endY);
			term.redraw(0, beginY, termWidth, endY - beginY);
		} finally {
			term.unlock();
		}
	}

	public void clearScreen() {
		clearLines(0, termHeight);
	}

	public void eraseInLine(final ControlSequence args) {
		// ESC [ Ps K
		term.lock();
		try {
			final int arg = args.getArg(0, 0);

			term.drawCursor();

			switch (arg) {
			case 0:
				if (cursorX < termWidth) {
					term.clearArea(cursorX, cursorY - 1, termWidth, cursorY);
					term.redraw(cursorX, cursorY - 1, termWidth - cursorX, 1);
				}
				break;
			case 1:
				final int extent = Math.min(cursorX + 1, termWidth);
				term.clearArea(0, cursorY - 1, extent, cursorY);
				term.redraw(0, cursorY - 1, extent, 1);
				break;
			case 2:
				term.clearArea(0, cursorY - 1, termWidth, cursorY);
				term.redraw(0, cursorY - 1, termWidth, 1);
				break;
			default:
				logger.error("Unsupported erase in line mode:" + arg);
				break;
			}
			term.drawCursor();
		} finally {
			term.unlock();
		}
	}

	public void cursorUp(final ControlSequence args) {
		term.lock();
		try {

			int arg = args.getArg(0, 0);
			arg = arg == 0 ? 1 : arg;

			term.drawCursor();
			cursorY -= arg;
			cursorY = Math.max(cursorY, 1);
			term.setCursor(cursorX, cursorY);
			term.drawCursor();
		} finally {
			term.unlock();
		}
	}

	public void cursorDown(final ControlSequence args) {
		term.lock();
		try {

			int arg = args.getArg(0, 0);
			arg = arg == 0 ? 1 : arg;

			term.drawCursor();
			cursorY += arg;
			cursorY = Math.min(cursorY, termHeight);
			term.setCursor(cursorX, cursorY);
			term.drawCursor();
		} finally {
			term.unlock();
		}
	}

	public void index() {
		term.lock();
		try {

			term.drawCursor();
			if (cursorY == termHeight) {
				term.scrollArea(scrollRegionTop, scrollRegionBottom
						- scrollRegionTop, -1);
				term.clearArea(0, scrollRegionBottom - 1, termWidth,
						scrollRegionBottom);
				term.redraw(0, scrollRegionTop, termWidth, scrollRegionBottom
						- scrollRegionTop);
			} else {
				cursorY += 1;
				term.setCursor(cursorX, cursorY);
			}

			term.drawCursor();
		} finally {
			term.unlock();
		}
	}

	public void nextLine() {
		term.lock();
		try {

			term.drawCursor();
			cursorX = 0;
			if (cursorY == termHeight) {
				term.scrollArea(scrollRegionTop - 1, scrollRegionBottom
						- scrollRegionTop + 1, -1);
				term.clearArea(0, scrollRegionBottom - 1, termWidth,
						scrollRegionBottom);
				term.redraw(0, scrollRegionTop - 1, termWidth,
						scrollRegionBottom - scrollRegionTop + 1);
			} else
				cursorY += 1;
			term.setCursor(0, cursorY);

			term.drawCursor();
		} finally {
			term.unlock();
		}
	}

	public void reverseIndex() {
		term.lock();
		try {

			term.drawCursor();
			if (cursorY == 1) {
				term.scrollArea(scrollRegionTop - 1, scrollRegionBottom
						- scrollRegionTop, 1);
				term.clearArea(cursorX, cursorY - 1, termWidth, cursorY);
				term.redraw(0, 0, termWidth, termHeight - 1);
			} else {
				cursorY -= 1;
				term.setCursor(cursorX, cursorY);
			}
			term.drawCursor();
		} finally {
			term.unlock();
		}
	}

	public void cursorForward(final ControlSequence args) {
		term.lock();
		try {

			term.drawCursor();
			int arg = args.getArg(0, 1);
			arg = arg == 0 ? 1 : arg;
			cursorX += arg;
			cursorX = Math.min(cursorX, termWidth - 1);
			term.setCursor(cursorX, cursorY);
			term.drawCursor();
		} finally {
			term.unlock();
		}
	}

	public void cursorBackward(final ControlSequence args) {
		term.lock();
		try {

			term.drawCursor();
			int arg = args.getArg(0, 1);
			arg = arg == 0 ? 1 : arg;
			cursorX -= arg;
			cursorX = Math.max(cursorX, 0);
			term.setCursor(cursorX, cursorY);
			term.drawCursor();
		} finally {
			term.unlock();
		}
	}

	public void cursorPosition(final ControlSequence args) {
		term.lock();
		try {
			final int argy = args.getArg(0, 1);
			final int argx = args.getArg(1, 1);
			term.drawCursor();
			cursorX = argx - 1;
			cursorY = argy;
			term.setCursor(cursorX, cursorY);
			term.drawCursor();
		} finally {
			term.unlock();
		}
	}

	public void setScrollingRegion(final ControlSequence args) {
		final int y1 = args.getArg(0, 1);
		final int y2 = args.getArg(1, termHeight);

		scrollRegionTop = y1;
		scrollRegionBottom = y2;
	}

	/*
	 * Character Attributes
	 * 
	 * ESC [ Ps;Ps;Ps;...;Ps m
	 * 
	 * Ps refers to a selective parameter. Multiple parameters are separated by
	 * the semicolon character (0738). The parameters are executed in order and
	 * have the following meanings: 0 or None All Attributes Off 1 Bold on 4
	 * Underscore on 5 Blink on 7 Reverse video on
	 * 
	 * Any other parameter values are ignored.
	 */

	static Color[] colors = { Color.BLACK, Color.RED, Color.GREEN,
			Color.YELLOW, Color.BLUE, Color.MAGENTA, Color.CYAN, Color.WHITE };

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.jcraft.jcterm.ITerminalWriter#setCharacterAttributes(int[], int)
	 */
	public void setCharacterAttributes(final ControlSequence args) {
		final int argCount = args.getCount();
		if (argCount == 0)
			clearCharacterAttributes();
		Color fg = null;
		Color bg = null;

		for (int i = 0; i < argCount; i++) {
			final int arg = args.getArg(i, -1);
			if (arg == -1) {
				logger.error("Error in processing char attributes, arg " + i);
				continue;
			}

			switch (arg) {
			case 0:
				clearCharacterAttributes();
				break;
			case 1:// Bright
				term.setBold(true);
				break;
			case 2:// Dim
				break;
			case 4:// Underscore on
				break;
			case 5:// Blink on
				break;
			case 7:// Reverse video on
				term.setReverseVideo();
				break;
			case 8: // Hidden
				break;
			}

			if (arg >= 30 && arg <= 37)
				fg = colors[arg - 30];

			if (arg >= 40 && arg <= 47)
				bg = colors[arg - 40];

			if (fg != null)
				term.setCurrentForeground(fg);
			if (bg != null)
				term.setCurrentBackground(bg);

		}
	}

	private void clearCharacterAttributes() {
		term.setBold(false);
		term.resetColors();
	}

	public void reset() {
		termWidth = term.getColumnCount();
		termHeight = term.getRowCount();
		clearCharacterAttributes();
	}

	public void beep() {
		term.beep();
	}

	public int distanceToLineEnd() {
		return termWidth - cursorX;
	}

	public void storeCursor(final StoredCursor storedCursor) {
		storedCursor.x = cursorX;
		storedCursor.y = cursorY;
	}

	public void restoreCursor(final StoredCursor storedCursor) {
		term.drawCursor();
		cursorX = 0;
		cursorY = 1;
		if (storedCursor != null) {
			// TODO: something with origin modes
			cursorX = storedCursor.x;
			cursorY = storedCursor.y;
		}
		term.setCursor(cursorX, cursorY);
		term.drawCursor();
	}

	public Dimension resize(final Dimension pendingResize,
			final RequestOrigin origin) {
		term.lock();
		try {
			final int oldHeight = termHeight;
			term.drawCursor();
			final Dimension pixelSize = term.doResize(pendingResize, origin);

			termWidth = term.getColumnCount();
			termHeight = term.getRowCount();

			scrollRegionBottom += termHeight - oldHeight;
			cursorY += termHeight - oldHeight;

			term.drawCursor();
			return pixelSize;
		} finally {
			term.unlock();
		}
	}

	public void fillScreen(final char c) {
		term.lock();
		try {

			final char[] chars = new char[termWidth];
			Arrays.fill(chars, c);
			final String str = new String(chars);

			for (int row = 1; row <= termHeight; row++)
				term.drawString(str, 0, row);
			term.redraw(0, 0, termWidth, termHeight);
		} finally {
			term.unlock();
		}
	}
}