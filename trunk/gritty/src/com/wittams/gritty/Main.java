package com.wittams.gritty;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.wittams.gritty.GrittyTerminal.BufferType;
import com.wittams.gritty.jsch.JSshTty;


public class Main {
	

	public static final Logger logger = Logger.getLogger(Main.class);
	JFrame bufferFrame;

	private TermPanel termPanel;

	private GrittyTerminal terminal;

	private AbstractAction openAction = new AbstractAction("Open SHELL Session..."){
		public void actionPerformed(final ActionEvent e) {
			openSession();
		}
	};
	
	private AbstractAction showBuffersAction = new AbstractAction("Show buffers") {
		public void actionPerformed(final ActionEvent e) {
			if(bufferFrame == null)
				showBuffers();
		}
	};
	
	private AbstractAction resetDamage = new AbstractAction("Reset damage") {
		public void actionPerformed(final ActionEvent e) {
			if(termPanel != null)
				termPanel.getBackBuffer().resetDamage();
		}
	};
	
	private AbstractAction drawDamage = new AbstractAction("Draw from damage") {
		public void actionPerformed(final ActionEvent e) {
			if(termPanel != null)
				termPanel.redrawFromDamage();
		}
	};
	
	private final JMenuBar getJMenuBar() {
		final JMenuBar mb = new JMenuBar();
		final JMenu m = new JMenu("File");
		
		m.add(openAction);
		mb.add(m);
		final JMenu dm = new JMenu("Debug");
		
		dm.add(showBuffersAction);
		dm.add(resetDamage);
		dm.add(drawDamage);
		mb.add(dm);

		return mb;
	}

	public void openSession() {
		if(!terminal.isSessionRunning()){
			terminal.setTty(new JSshTty());
			terminal.start();
		}
	}

	Main() {
		terminal = new GrittyTerminal();
		termPanel = terminal.getTermPanel();
		final JFrame frame = new JFrame("Gritty");

		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				System.exit(0);
			}
		});

		final JMenuBar mb = getJMenuBar();
		frame.setJMenuBar(mb);

		frame.setSize(termPanel.getPixelWidth(), termPanel.getPixelHeight());
		frame.getContentPane().add("Center", terminal);

		frame.pack();
		termPanel.setVisible(true);
		frame.setVisible(true);

		frame.setResizable(true);
		
		sizeFrameForTerm(frame);

		//term.setFrame(frame.getContentPane());
		
		termPanel.setResizePanelDelegate(new ResizePanelDelegate(){
			public void resizedPanel(final Dimension pixelDimension, final RequestOrigin origin) {
				if(origin == RequestOrigin.Remote)
					sizeFrameForTerm(frame);
			}
		});
		
	}

	private void sizeFrameForTerm(final JFrame frame) {
		int w = termPanel.getPixelWidth();
		int h = termPanel.getPixelHeight();
		w += frame.getWidth() - frame.getContentPane().getWidth();
		w += terminal.getScrollBar().getWidth();
		h += frame.getHeight() - frame.getContentPane().getHeight();
		frame.setSize(w, h);
	}

	public static void main(final String[] arg) {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		new Main();
	}
	
	private static final class BufferPanel extends JPanel {
		BufferPanel(final GrittyTerminal terminal){
			super(new BorderLayout());
			final JTextArea area = new JTextArea();
			add(area, BorderLayout.NORTH);
			
			final BufferType[] choices = BufferType.values(); 
			
			final JComboBox chooser = new JComboBox(choices);
			add(chooser, BorderLayout.NORTH);
			
			area.setFont(Font.decode("Monospaced-14"));
			add(new JScrollPane(area), BorderLayout.CENTER);
			
			class Updater implements ActionListener, ItemListener{
				void update(){
					final BufferType choice = (BufferType) chooser.getSelectedItem();
					final String text = choice.getValue(terminal);
					area.setText(text);
				}
				
				public void actionPerformed(final ActionEvent e) {
					update();
				}

				public void itemStateChanged(final ItemEvent e) {
					update();
				}
			};
			final Updater up = new Updater();
			chooser.addItemListener(up);
			final Timer timer = new Timer(1000, up);
			timer.setRepeats(true);
			timer.start();
			
		}
	}
	
	private void showBuffers() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				bufferFrame = new JFrame("buffers");
				final JPanel panel = new BufferPanel(terminal);
				
				bufferFrame.getContentPane().add(panel);
				bufferFrame.pack();
				bufferFrame.setVisible(true);
				bufferFrame.setSize(800, 600);
				
				bufferFrame.addWindowListener(new WindowAdapter(){
					@Override
					public void windowClosing(final WindowEvent e) {
						bufferFrame = null;
					}
				});
			}
		});
	}
}
