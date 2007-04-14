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

import java.awt.Dimension;
import java.io.IOException;


public abstract class Emulator {
	final protected ITerminalWriter tw;

	final protected TermIOBuffer channel;

	public Emulator(final Term term, final TermIOBuffer channel) {
		final ITerminalWriter wrapped = new TerminalWriter(term);

		if (false)
			tw = ITerminalWriter.Util.wrapWithLogging(wrapped);
		else
			tw = wrapped;
		
		this.channel = channel;
	}

	public abstract void start();

	public abstract byte[] getCode(int key);

	public abstract void reset();
	
	public abstract void postResize(Dimension dimension, RequestOrigin origin);

	public void sendBytes(final byte[] bytes) throws IOException {
		channel.sendBytes(bytes);
	}

	

}
