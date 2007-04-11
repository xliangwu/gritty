/* -*-mode:java; c-basic-offset:2; -*- */
/* JCTerm
 * Copyright (C) 2002-2004 ymnk, JCraft,Inc.
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.log4j.Logger;

public class TermPanel extends JPanel implements KeyListener, Term, ClipboardOwner, StyledRunConsumer {
	private static final Logger logger = Logger.getLogger(TermPanel.class);
	private static final long serialVersionUID = -1048763516632093014L;

	private BufferedImage img;

	private Graphics2D selectGfx;

	private Graphics2D cursorGfx;

	private Graphics2D gfx;

	private final Color chosenBackground = Color.WHITE;

	private final Color chosenForeground = Color.BLACK;

	private Color currentBackground = chosenBackground;

	private Color currentForeground = chosenForeground;

	private final Component termComponent = this;

	private Font currentFont;

	private Font normalFont;

	private Font boldFont;

	private int descent = 0;

	private int lineSpace = -2;

	Dimension charSize = new Dimension();

	Dimension termSize = new Dimension(80, 24);

	protected Point cursor = new Point();

	private boolean antialiasing = false;

	private Emulator emulator = null;

	protected Point selectionStart;

	protected Point selectionEnd;

	protected boolean selectionInProgress;

	private CharacterTerm backBuffer;

	private Clipboard clipBoard;

	private ResizePanelDelegate resizePanelDelegate;

	private ScrollBuffer scrollBuffer;

	private final BoundedRangeModel brm = new DefaultBoundedRangeModel(0,80,0,80);

	protected int clientScrollOrigin;
	
	public TermPanel() {
		scrollBuffer = new ScrollBuffer();
		brm.setRangeProperties(0, termSize.height, - scrollBuffer.getLineCount() , termSize.height, false );
		
		backBuffer = new CharacterTerm(termSize.width, termSize.height);
		currentFont = Font.decode("Monospaced-14");
		normalFont = currentFont;
		boldFont = currentFont.deriveFont(Font.BOLD);
		
		establishFontMetrics();

		setUpImages();
		clear();
		setUpClipboard();
		setAntiAliasing(antialiasing);

		setPreferredSize(new Dimension(getPixelWidth(), getPixelHeight()));
		
		
		setSize(getPixelWidth(), getPixelHeight());
		setFocusable(true);
		enableInputMethods(true);

		setFocusTraversalKeysEnabled(false);

		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(final MouseEvent e) {
				final Point charCoords = panelToCharCoords(e.getPoint());

				clearSelection();
				if (!selectionInProgress) {
					selectionStart = charCoords;
					selectionInProgress = true;
				}
				selectionEnd = charCoords;
				drawSelection();
			}
		});

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(final MouseEvent e) {
				selectionInProgress = false;
				if ( selectionStart != null && selectionEnd != null)
					copySelection(selectionStart, selectionEnd);
			}

			@Override
			public void mouseClicked(final MouseEvent e) {
				clearSelection();
				selectionStart = null;
				selectionEnd = null;
				drawSelection();
				if(e.getButton() == MouseEvent.BUTTON3 )
					pasteSelection();
			}
		});

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(final ComponentEvent e) {
				sizeTerminalFromComponent();
			}
		});

		brm.addChangeListener(new ChangeListener() {
			public void stateChanged(final ChangeEvent e) {
			    final int newOrigin = brm.getValue();
			    if( clientScrollOrigin != newOrigin){
			    	drawCursor();
			    	drawSelection();
			    	clientScrollOrigin = newOrigin;
			    	redraw(0, 0, termSize.width, termSize.height );
			    	drawSelection();
			    	drawCursor();
			    }
			}
		});
	}
	
	private Point panelToCharCoords(final Point p) {
		return new Point(p.x / charSize.width, p.y / charSize.height + clientScrollOrigin);
	}

	void setUpClipboard() {
		clipBoard = Toolkit.getDefaultToolkit().getSystemSelection();
		if (clipBoard == null)
			clipBoard = Toolkit.getDefaultToolkit().getSystemClipboard();
	}
	
	private void copySelection(final Point selectionStart, final Point selectionEnd) {
		if (selectionStart == null || selectionEnd == null)
			return;
		
		Point top;
		Point bottom;

		if (selectionStart.y == selectionEnd.y) {
			/* same line */
			
			top = selectionStart.x < selectionEnd.x ? selectionStart
					: selectionEnd;
			bottom = selectionStart.x >= selectionEnd.x ? selectionStart
					: selectionEnd;
		} else {
			top = selectionStart.y < selectionEnd.y ? selectionStart
					: selectionEnd;
			bottom = selectionStart.y > selectionEnd.y ? selectionStart
					: selectionEnd;
		}
		
		final StringBuffer selection = new StringBuffer();
		if( top.y < 0 ){
			final Point scrollEnd = bottom.y >= 0 ? new Point(termSize.width, -1) : bottom;
			
			scrollBuffer.pumpRuns(top.y, scrollEnd.y - top.y, 
					new SelectionRunConsumer(selection, top, scrollEnd));
			
		}
		
		if( bottom.y >= 0 ){
			final Point backBegin = top.y < 0 ? new Point(0, 0) : top;
			backBuffer.pumpRuns( 0, backBegin.y, termSize.width, bottom.y -  backBegin.y + 1, 
					new SelectionRunConsumer(selection, backBegin, bottom));
		}
		
		if(selection.length() == 0) return;
		try {
			clipBoard.setContents(new StringSelection(selection.toString()), this);
		} catch (final IllegalStateException e) {
			logger.error("Could not set clipboard:" ,e);
		}
	}
	
	void pasteSelection(){
		try {
			final String selection = (String) clipBoard.getData( DataFlavor.stringFlavor );
			emulator.sendBytes(selection.getBytes());
		} catch (final UnsupportedFlavorException e) {
			
		} catch (final IOException e) {
			
		}
	}
	
	public void lostOwnership(final Clipboard clipboard, final Transferable contents) {
	}

	private void setUpImages() {
		final BufferedImage oldImage = img;
		img = new BufferedImage(getPixelWidth(), getPixelHeight(),
				BufferedImage.TYPE_INT_RGB);

		gfx = img.createGraphics();
		gfx.setFont(currentFont);
		
		if (oldImage != null)
			gfx.drawImage(oldImage, 0, img.getHeight() - oldImage.getHeight(),
					oldImage.getWidth(), oldImage.getHeight(), termComponent);

		cursorGfx = (Graphics2D) img.getGraphics();
		cursorGfx.setColor(currentForeground);
		cursorGfx.setXORMode(currentBackground);

		selectGfx = (Graphics2D) img.getGraphics();
		selectGfx.setColor(Color.GREEN);
		selectGfx.setXORMode(currentBackground);

	}

	private void sizeTerminalFromComponent() {
		if (emulator != null) {
			final int newWidth = getWidth() / getCharWidth();
			final int newHeight = getHeight() / getCharHeight();

			final Dimension newSize = new Dimension(newWidth, newHeight);

			emulator.postResize(newSize, ResizeOrigin.User);
		}
	}

	public void setEmulator(final Emulator emulator) {
		this.emulator = emulator;
		this.sizeTerminalFromComponent();
	}

	public Dimension doResize(final Dimension newSize, final ResizeOrigin origin) {
		if(!newSize.equals(termSize)){
			backBuffer.doResize(newSize, origin);
			termSize = (Dimension) newSize.clone();
			// resize images..
			setUpImages();
			redraw(0, 0, getColumnCount(), getRowCount());
	
			final Dimension pixelDimension = new Dimension(getPixelWidth(), getPixelHeight());
			
			setSize( pixelDimension );
			if(resizePanelDelegate != null) resizePanelDelegate.resizedPanel( pixelDimension, origin);
			brm.setRangeProperties(0, termSize.height, - scrollBuffer.getLineCount() , termSize.height, false );
		}
		return new Dimension(getPixelWidth(), getPixelHeight());
	}
	
	public void setResizePanelDelegate(final ResizePanelDelegate resizeDelegate) {
		resizePanelDelegate = resizeDelegate;
	}



	private void establishFontMetrics() {
		final BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
		final Graphics2D graphics = img.createGraphics();
		graphics.setFont(currentFont);
		{
			final FontMetrics fo = graphics.getFontMetrics();
			descent = fo.getDescent();
			charSize.width = fo.charWidth('@');
			charSize.height = fo.getHeight() + lineSpace * 2;
			descent += lineSpace;
		}
		img.flush();
		graphics.dispose();
	}

	@Override
	public void paintComponent(final Graphics g) {
		super.paintComponent(g);
		if (img != null)
			g.drawImage(img, 0, 0, termComponent);
	}

	@Override
	public void paint(final Graphics g) {
		super.paint(g);
	}

	@Override
	public void processKeyEvent(final KeyEvent e) {
		// System.out.println(e);
		final int id = e.getID();
		if (id == KeyEvent.KEY_PRESSED)
			keyPressed(e);
		else if (id == KeyEvent.KEY_RELEASED) {
			/* keyReleased(e); */
		} else if (id == KeyEvent.KEY_TYPED)
			keyTyped(e);
		e.consume();
	}

	public void keyPressed(final KeyEvent e) {
		try {
			final int keycode = e.getKeyCode();
			final byte[] code = emulator.getCode(keycode);
			if (code != null)
				emulator.sendBytes(code);
			else {
				final char keychar = e.getKeyChar();
				final byte[] obuffer = new byte[1];
				if ((keychar & 0xff00) == 0) {
					obuffer[0] = (byte) e.getKeyChar();
					emulator.sendBytes(obuffer);
				}
			}
		} catch (final IOException ex) {
			logger.error("Error sending key to emulator", ex);
		}
	}

	public void keyTyped(final KeyEvent e) {
		final char keychar = e.getKeyChar();
		if ((keychar & 0xff00) != 0) {
			final char[] foo = new char[1];
			foo[0] = keychar;
			try {
				final byte[] bytes = new String(foo).getBytes("EUC-JP");
				emulator.sendBytes(bytes);
			} catch (final IOException ex) {
				logger.error("Error sending key to emulator", ex);
			}
		}
	}
	
	/** Ignores key released events. */
	public void keyReleased(final KeyEvent event) {
	}

	public int getPixelWidth() {
		return charSize.width * termSize.width;
	}

	public int getPixelHeight() {
		return charSize.height * termSize.height;
	}

	public int getCharWidth() {
		return charSize.width;
	}

	public int getCharHeight() {
		return charSize.height;
	}

	public int getColumnCount() {
		return termSize.width;
	}

	public int getRowCount() {
		return termSize.height;
	}

	public void clear() {
		gfx.setColor(currentBackground);
		gfx.fillRect(0, 0, getPixelWidth(), getPixelHeight());
		gfx.setColor(currentForeground);
		backBuffer.clear();
	}

	public void drawCursor() {
		final int maxY = cursor.y  - clientScrollOrigin;
		final int amountOver = maxY - termSize.height;
		if(amountOver < 1){
		
			cursorGfx.fillRect(cursor.x * charSize.width, (cursor.y - 1 - clientScrollOrigin)
					* charSize.height, charSize.width, charSize.height);
			final Graphics g = getGraphics();
			g.setClip(cursor.x * charSize.width, (cursor.y - 1 - clientScrollOrigin) * charSize.height,
					charSize.width, charSize.height);
			g.drawImage(img, 0, 0, termComponent);
			backBuffer.drawCursor();
		}
	}

	public void clearSelection() {
		drawSelection();
	}

	public void drawSelection() {
		/* which is the top one */
		Point top;
		Point bottom;
		final Graphics g = getGraphics();
		if (selectionStart == null || selectionEnd == null)
			return;

		if (selectionStart.y == selectionEnd.y) {
			/* same line */
			if (selectionStart.x == selectionEnd.x)
				return;
			top = selectionStart.x < selectionEnd.x ? selectionStart
					: selectionEnd;
			bottom = selectionStart.x >= selectionEnd.x ? selectionStart
					: selectionEnd;

			selectGfx.fillRect(top.x * charSize.width, (top.y - clientScrollOrigin) * charSize.height,
					(bottom.x - top.x) * charSize.width, charSize.height);
			g.setClip(top.x * charSize.width, (top.y - clientScrollOrigin) * charSize.height,
					(bottom.x - top.x) * charSize.width, charSize.height);
			g.drawImage(img, 0, 0, termComponent);

		} else {
			top = selectionStart.y < selectionEnd.y ? selectionStart
					: selectionEnd;
			bottom = selectionStart.y > selectionEnd.y ? selectionStart
					: selectionEnd;
			/* to end of first line */
			selectGfx.fillRect(top.x * charSize.width, (top.y - clientScrollOrigin) * charSize.height,
					(termSize.width - top.x) * charSize.width, charSize.height);
			g.setClip(top.x * charSize.width, (top.y - clientScrollOrigin) * charSize.height,
					(termSize.width - top.x) * charSize.width, charSize.height);
			g.drawImage(img, 0, 0, termComponent);

			if (bottom.y - top.y > 1) {
				/* intermediate lines */
				selectGfx.fillRect(0, (top.y + 1 - clientScrollOrigin) * charSize.height,
						termSize.width * charSize.width, (bottom.y - top.y - 1)
								* charSize.height);
				g.setClip(0, (top.y + 1) * charSize.height, getPixelWidth(),
						(bottom.y - top.y - 1 - clientScrollOrigin) * charSize.height);
				g.drawImage(img, 0, 0, termComponent);
			}

			/* from beginning of last line */

			selectGfx.fillRect(0, (bottom.y  - clientScrollOrigin) * charSize.height, bottom.x
					* charSize.width, charSize.height);
			g.setClip(0, (bottom.y  - clientScrollOrigin) * charSize.height, bottom.x * charSize.width,
					charSize.height);
			g.drawImage(img, 0, 0, termComponent);

		}
	}

	public void run(final int x, final int y, final Style style, final char[] buf, final int start, final int len) {
		gfx.setColor(style.getBackground());
		gfx.fillRect(x * charSize.width, (y - clientScrollOrigin) * charSize.height, len * charSize.width, charSize.height);
		
		gfx.setFont( style.hasOption(Style.StyleOptions.BOLD) ? boldFont : normalFont );
		gfx.setColor(style.getForeground());
		
		gfx.drawChars(buf, start, len, x * charSize.width, (y + 1 - clientScrollOrigin) * charSize.height - descent);
	}
	
	public void redraw(final int x, final int y, final int width, final int height) {
		final int maxY = y + height - clientScrollOrigin;
		final int amountOver = Math.max(0, maxY - termSize.height);
		final int drawnHeight = height - amountOver;
		
		if(amountOver > 0)
			//gfx.setColor(Color.YELLOW);
			//gfx.fillRect(x * charSize.width, y * charSize.height, width * charSize.width, (height - drawnHeight) * charSize.height );
			//gfx.setClip(x * charSize.width, (y + clientScrollOrigin) * charSize.height, width * charSize.width, amountOver * charSize.height);
			scrollBuffer.pumpRuns(y + clientScrollOrigin, Math.min(amountOver, termSize.height), this);
		
		backBuffer.redraw(x, y, width, height);
		
		if(drawnHeight > 0)
			backBuffer.pumpRuns(x, y, width, height - amountOver, this);
		redrawImpl(x * charSize.width, 
				   y * charSize.height, 
				   width * charSize.width, 
				   height * charSize.height);
	}

	private void redrawImpl(final int x, final int y, final int width, final int height) {
		final Graphics g = getGraphics();
		g.setClip(x, y, width, height);
		g.drawImage(img, 0, 0, termComponent);
	}
	
	public void scrollArea(final int y, final int h, int dy) {
		if( dy < 0 ){ 
			//Moving lines off the top of the screen
			//TODO: Something to do with application keypad mode
			//TODO: Something to do with the scroll margins
			backBuffer.pumpRuns(0, y - 1, termSize.width, -dy, scrollBuffer);
			
			brm.setRangeProperties(0, termSize.height, - scrollBuffer.getLineCount() , termSize.height, false );
		}
		drawSelection();
		selectionStart = null;
		selectionEnd = null;
		scrollAreaImpl(y * charSize.height, 
				       h * charSize.height, 
				       dy * charSize.height);
		backBuffer.scrollArea(y, h, dy);
	}

	public void scrollAreaImpl(final int y, final int h, final int dy) {
		getGraphics().copyArea(0, y, getPixelWidth(), h, 0, dy);
		gfx.copyArea(0, y, getPixelWidth(), h, 0, dy);
	}
	
	public void clearArea(final int x1, final int y1, final int x2, final int y2) {
		backBuffer.clearArea(x1, y1, x2, y2);
	}

	public void drawBytes(final byte[] buf, final int s, final int len, final int x, final int y) {
		backBuffer.drawBytes(buf, s, len, x, y);
	}

	public void drawString(final String str, final int x, final int y) {
		backBuffer.drawString(str, x, y);
	}

	public void setCursor(final int x, final int y) {
		cursor.x = x;
		cursor.y = y;
		backBuffer.setCursor(x, y);
	}

	public void beep() {
		Toolkit.getDefaultToolkit().beep();
	}

	public void setLineSpace(final int foo) {
		lineSpace = foo;
	}

	public void setAntiAliasing(final boolean foo) {
		if (gfx == null)
			return;
		antialiasing = foo;
		final java.lang.Object mode = foo ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON
				: RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;
		final RenderingHints hints = new RenderingHints(
				RenderingHints.KEY_TEXT_ANTIALIASING, mode);
		gfx.setRenderingHints(hints);
	}

	public void setCurrentForeground(final Color fg) {
		currentForeground = fg;
		gfx.setColor(currentForeground);
		backBuffer.setCurrentForeground(fg);
	}

	public void setCurrentBackground(final Color bg) {
		currentBackground = bg;
		backBuffer.setCurrentBackground(bg);
	}

	public void setBold(final boolean val) {
		if (val)
			currentFont = boldFont;
		else
			currentFont = normalFont;
		gfx.setFont(currentFont);
		backBuffer.setBold(val);
	}

	public void setReverseVideo() {
		setCurrentBackground(chosenForeground);
		setCurrentForeground(chosenBackground);
		backBuffer.setReverseVideo();
	}

	public void resetColors() {
		setCurrentBackground(chosenBackground);
		setCurrentForeground(chosenForeground);
		backBuffer.resetColors();
	}

	public String getBackingLines() {
		return backBuffer.getLines();
	}

	public String getStyleLines() {
		return backBuffer.getStyleLines();
	}

	public String getScrollLines() {
		return scrollBuffer.getLines();
	}

	public BoundedRangeModel getBoundedRangeModel() {
		return brm ;
	}
	
}
