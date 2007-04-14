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


public class Emulator {
	final protected TerminalWriter tw;

	final protected TtyChannel channel;

	protected final StateMachine sm;

	public Emulator(final Term term, final TtyChannel channel) {
		this.channel = channel;
		tw = new TerminalWriter(term);
		sm = new StateMachine(this.channel, tw);
	}

	public void sendBytes(final byte[] bytes) throws IOException {
		channel.sendBytes(bytes);
	}

	public void postResize(final Dimension dimension, final RequestOrigin origin){
		sm.postResize(dimension, origin);
	}

	public void start(){
		sm.go();
	}

	public byte[] getCode(final int key){
		return CharacterUtils.getCode(key);
	}

	public void reset(){
		if (tw != null)
			tw.reset();
	}

}
