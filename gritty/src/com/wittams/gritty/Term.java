/* -*-mode:java; c-basic-offset:2; -*- */
/* JCTerm
 * Copyright (C) 2002 ymnk, JCraft,Inc.
 *  
 * Written by: 2002 ymnk<ymnk@jcaft.com>
 *   
 *   
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * as published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package com.wittams.gritty;

import java.awt.Color;
import java.awt.Dimension;

public interface Term {
	int getRowCount();

	int getColumnCount();

	int getCharWidth();

	int getCharHeight();

	void clear();

	void drawCursor();

	void setCursor(int x, int y);

	void redraw(int x, int y, int width, int height);

	// void redraw();
	void clearArea(int x1, int y1, int x2, int y2);

	void scrollArea(int y, int h, int dy);

	void drawBytes(byte[] buf, int s, int len, int x, int y);

	void drawString(String str, int x, int y);

	void beep();

	void setReverseVideo();

	void setBold(boolean val);

	void setCurrentForeground(Color fg);

	void setCurrentBackground(Color fg);

	void resetColors();

	static enum ResizeOrigin{
		User,
		Remote;
	}
	
	Dimension doResize(Dimension pendingResize, ResizeOrigin origin);

}
