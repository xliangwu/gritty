/**
 * 
 */
package com.wittams.gritty;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class TermIOBuffer {
	private InputStream in = null;
	private OutputStream out = null;
	private ResizeTtyDelegate resizeTtyDelegate = null;
	
	byte[] buf = new byte[1024];

	int bufs = 0;

	int buflen = 0;

	int serial;

	TermIOBuffer(final InputStream in, final OutputStream out, final ResizeTtyDelegate resizeTtyDelegate) {
		this.in = in;
		this.out = out;
		this.resizeTtyDelegate = resizeTtyDelegate;
		serial = 0;
	}

	byte getChar() throws java.io.IOException {
		if (buflen == 0)
			fillBuf();
		buflen--;

		return buf[bufs++];
	}

	public void appendBuf(final StringBuffer sb, final int begin, final int length) {
		CharacterUtils.appendBuf(sb, buf, begin, length);
	}

	private void fillBuf() throws java.io.IOException {
		buflen = bufs = 0;
		buflen = in.read(buf, bufs, buf.length - bufs);
		serial++;

		if (buflen <= 0) {
			buflen = 0;
			throw new IOException("fillBuf");
		}
	}

	void pushChar(final byte foo) throws java.io.IOException {
		buflen++;
		buf[--bufs] = foo;
	}

	int advanceThroughASCII(int toLineEnd) throws java.io.IOException {
		if (buflen == 0)
			fillBuf();

		int len = toLineEnd > buflen ? buflen : toLineEnd;
		
		final int origLen = len;
		byte tmp;
		while (len > 0) {
			tmp = buf[bufs++];
			if (0x20 <= tmp && tmp <= 0x7f) {
				buflen--;
				len--;
				continue;
			}
			bufs--;
			break;
		}		
		return origLen - len;
	}

	void sendBytes(final byte[] bytes) throws IOException {
		out.write(bytes);
		out.flush();
		/*
		 * System.out.print("Written " + bytes.length + " bytes.");
		 * printBuf(System.out, bytes, 0, bytes.length); System.out.println();
		 */
	}

	public void postResize(final Dimension termSize, final Dimension pixelSize) {
		resizeTtyDelegate.resize(termSize, pixelSize);
	}

	public void pushBackBuffer(final byte[] bytes, final int len) throws IOException {
		for(int i = len - 1; i >= 0; i--)
			pushChar(bytes[i]);
	}
}