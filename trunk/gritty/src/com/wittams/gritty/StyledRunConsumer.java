package com.wittams.gritty;

public interface StyledRunConsumer {
	void run(int x, int y, Style style, char[] buf, int start, int len);
//	void blank(int x, int y, int len);
//	void newLine(int x, int y);
}
