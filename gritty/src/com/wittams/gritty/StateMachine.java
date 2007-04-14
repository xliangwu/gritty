/**
 * 
 */
package com.wittams.gritty;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.EnumSet;

import org.apache.log4j.Logger;


import static com.wittams.gritty.CharacterUtils.*;

class StateMachine {
	private final static Logger logger = Logger.getLogger(StateMachine.class);
	private final TerminalWriter tw;
	private final EnumSet<Mode> modes = EnumSet.of(Mode.ANSI);
	private final TtyChannel channel;
	
	StateMachine(final TtyChannel channel, final TerminalWriter tw) {
		this.tw = tw;
		this.channel = channel;
	}

	void go() {
		try {
			while (true)
				singleIteration();	
		} catch (final InterruptedIOException e){
			logger.info("Terminal exiting");
		} catch (final Exception e) {
			logger.error("Caught exception in terminal thread", e);
		}
	}
	
	public void postResize(final Dimension dimension, final RequestOrigin origin) {
		Dimension pixelSize;
		synchronized (tw) {
			pixelSize = tw.resize(dimension, origin);
		}
		channel.postResize(dimension, pixelSize);
	}

	void singleIteration() throws IOException {
		byte b = channel.getChar();
		
		synchronized (tw) {
			tw.startIteration();
		}

		switch (b) {
		case 0:
			break;
		case ESC: // ESC
			b = channel.getChar();
			handleESC(b);
			break;
		case BEL:
			synchronized (tw) { tw.beep(); }
			break;
		case BS:
			synchronized (tw) { tw.backspace(); }
			break;
		case TAB: // ht(^I) TAB
			synchronized (tw) { tw.horizontalTab(); }
			break;
		case CR:
			synchronized (tw) {  tw.carriageReturn(); }
			break;
		case FF:
		case VT:
		case LF:
			// '\n'
			synchronized (tw) {
				tw.newLine();
				
			}
			break;
		default:
			synchronized(tw){
				if( b <= CharacterUtils.US ){
					if(logger.isInfoEnabled()){
						StringBuffer sb = new StringBuffer("Unhandled control character:");
						CharacterUtils.appendChar(sb, CharacterType.NONE, (char) b);
						logger.info(sb.toString());
					}
				} else if ( b > CharacterUtils.DEL ) { 
					//TODO: double byte character.. this is crap
					final byte[] bytesOfChar = new byte[2];
					bytesOfChar[0] = b;
					bytesOfChar[1] = channel.getChar();
					tw.writeDoubleByte(bytesOfChar);
				} else{
					channel.pushChar(b);
					final int availableChars = channel.advanceThroughASCII(tw.distanceToLineEnd());
					tw.writeASCII(channel.buf, channel.offset - availableChars, availableChars);
				}
			}
			break;
		}
	}

	private void handleESC(byte initByte) throws IOException {
		byte b = initByte;
		if (b == '['){
			doControlSequence();
		} else {
			final byte[] intermediate = new byte[10];
			int intCount = 0;
			while (b >= 0x20 && b <= 0x2F) {
				intCount++;
				intermediate[intCount - 1] = b;
				b = channel.getChar();
			}
			if (b >= 0x30 && b <= 0x7E)
				synchronized(tw){
					switch (b) {
					case 'M':
						// Reverse index ESC M
						tw.reverseIndex();
						break;
					case 'D':
						// Index ESC D
						tw.index();
						break;
					case 'E':
						tw.nextLine();
						break;
					case '7':
						saveCursor();
						break;
					case '8':
						if(intCount > 0 && intermediate[0] == '#' )
							tw.fillScreen('E');
						else
							restoreCursor();
						break;
					default:
						if(logger.isDebugEnabled()){
							logger.debug("Unhandled escape sequence : " +
						
							escapeSequenceToString(intermediate, intCount, b) );
						}
					}
				}
			else {
				if(logger.isDebugEnabled()){
					logger.debug("Malformed escape sequence, pushing back to buffer: " +
				
					escapeSequenceToString(intermediate, intCount, b) );
				}
				// Push backwards
				for (int i = intCount - 1; i >= 0 ; i--) {
					final byte ib = intermediate[i];
					channel.pushChar(ib);
				}
				channel.pushChar(b);
			}
		}
	}

	StoredCursor storedCursor = null;

	private void saveCursor() {

		if (storedCursor == null)
			storedCursor = new StoredCursor();
		tw.storeCursor(storedCursor);
	}

	private void restoreCursor() {
		tw.restoreCursor(storedCursor);
	}

	private String escapeSequenceToString(final byte[] intermediate,
			final int intCount, final byte b) {
		
		StringBuffer sb = new StringBuffer("ESC ");
		
		for (int i = 0; i < intCount; i++) {
			final byte ib = intermediate[i];
			sb.append(' ');
			sb.append((char) ib);
		}
		sb.append(' ');
		sb.append((char) b);
		return sb.toString();
	}

	private void doControlSequence() throws IOException {
		final ControlSequence cs = new ControlSequence(channel);
		
		if(logger.isDebugEnabled()){
			StringBuffer sb = new StringBuffer();
			sb.append("Control sequence\n");
			sb.append("parsed                        :");
			cs.appendToBuffer(sb);
			sb.append('\n');
			sb.append("bytes read                    :ESC[");
			cs.appendActualBytesRead(sb, channel);
			logger.debug(sb.toString());
		}
		if(cs.pushBackReordered(channel)) return;
		
		synchronized (tw) {

		switch (cs.getFinalChar()) {
			case 'm':
				tw.setCharacterAttributes(cs);
				break;
			case 'r':
				tw.setScrollingRegion(cs);
				break;
			case 'A':
				tw.cursorUp(cs);
				break;
			case 'B':
				tw.cursorDown(cs);
				break;
			case 'C':
				tw.cursorForward(cs);
				break;
			case 'D':
				tw.cursorBackward(cs);
				break;
			case 'f':
			case 'H':
				tw.cursorPosition(cs);
				break;
			case 'K':
				tw.eraseInLine(cs);
				break;
			case 'J':
				tw.eraseInDisplay(cs);
				break;
			case 'h':
				setModes(cs, true);
				break;
			case 'l':
				setModes(cs, false);
				break;
			case 'c':
				// What are you
				// ESC [ c or ESC [ 0 c
				// Response is ESC [ ? 6 c
				if(logger.isDebugEnabled()) {
					logger.debug("Identifying to remote system as VT102");
				}
				channel.sendBytes(deviceAttributesResponse);
				break;
			default:
				if(logger.isInfoEnabled()){
					StringBuffer sb = new StringBuffer();
					sb.append("Unhandled Control sequence\n");
					sb.append("parsed                        :");
					cs.appendToBuffer(sb);
					sb.append('\n');
					sb.append("bytes read                    :ESC[");
					cs.appendActualBytesRead(sb, channel);
					logger.info(sb.toString());
				}
				break;
			}
		
					
		}
	}

	private void setModes(final ControlSequence args, final boolean on) throws IOException {
		final int argCount = args.getCount();
		final Mode[] modeTable = args.getModeTable();
		for (int i = 0; i < argCount; i++) {
			final int num = args.getArg(i, -1);
			Mode mode = null;
			if (num >= 0 && num < modeTable.length){
				mode = modeTable[num];
			}
			
			
			if (mode == null){
				if(logger.isInfoEnabled()) logger.info("Unknown mode " + num);
			}else if (on) {
				if(logger.isInfoEnabled()) logger.info("Modes: adding " + mode);
				modes.add(mode);
				synchronized (tw) {
					switch(mode){
					case WideColumn:
						tw.resize(new Dimension(132, 24), RequestOrigin.Remote );
						tw.clearScreen();
						tw.restoreCursor(null);
						break;
					}
				}
			} else {
				if(logger.isInfoEnabled()) logger.info("Modes: removing " + mode);
				modes.remove(mode);
				synchronized (tw) {
					switch(mode){
					case WideColumn:
						tw.resize(new Dimension(80, 24), RequestOrigin.Remote);
						tw.clearScreen();
						tw.restoreCursor(null);
						break;
					}
				}
			}
		}
	}

	

}