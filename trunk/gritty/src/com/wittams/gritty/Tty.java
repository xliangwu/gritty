package com.wittams.gritty;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Tty {
	void init();
	OutputStream getOuputStream();
	InputStream getInputStream();
	void resize(Dimension termSize, Dimension pixelSize);
	void close();
	String getName();
	int read(byte[] buf, int offset, int length) throws IOException;
	void write(byte[] bytes) throws IOException;
}
