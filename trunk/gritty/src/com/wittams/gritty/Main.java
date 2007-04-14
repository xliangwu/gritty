package com.wittams.gritty;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

public class Main {
	public static final Logger logger = Logger.getLogger(Main.class);
	Thread connectThread;
	JFrame bufferFrame;

	private TermPanel termPanel;

	private String user = System.getProperty("user.name").toLowerCase();

	private String host = "localhost";

	private GrittyTerminal terminal;

	private AbstractAction openAction = new AbstractAction("Open SHELL Session..."){
		public void actionPerformed(final ActionEvent e) {
			if (connectThread == null)
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
		connectThread = new Thread(new ConnectRunnable());
		connectThread.start();
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
	
	private void showBuffers() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				bufferFrame = new JFrame("buffers");
				final JTextArea area = new JTextArea();
				final JPanel panel = new JPanel(new BorderLayout());
				
				panel.add(area, BorderLayout.NORTH);
				
				final String[] choices = {"Back", "Back style", "Back damage", "Scroll"}; 
				
				final JComboBox chooser = new JComboBox(choices);
				panel.add(chooser, BorderLayout.NORTH);
				
				area.setFont(Font.decode("Monospaced-14"));
				panel.add(new JScrollPane(area), BorderLayout.CENTER);
				
				
				bufferFrame.getContentPane().add(panel);
				bufferFrame.pack();
				bufferFrame.setVisible(true);
				bufferFrame.setSize(800, 600);
				
				class Updater implements ActionListener, ItemListener{
					void update(){
						final int choice = chooser.getSelectedIndex();
						final String text = 
							          choice == 0 ? termPanel.getBackBuffer().getLines() 
								    : choice == 1 ? termPanel.getBackBuffer().getStyleLines()
								    : choice == 2 ? termPanel.getBackBuffer().getDamageLines() 
								    :               termPanel.getScrollBuffer().getLines() ;
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
				
				bufferFrame.addWindowListener(new WindowAdapter(){
					@Override
					public void windowClosing(final WindowEvent e) {
						bufferFrame = null;
					}
				});
			}
		});
	}

	class ConnectRunnable implements Runnable { 
		InputStream in = null;
		OutputStream out = null;
		Session session;
		ChannelShell channel;
		
		public void run() {
			
			int port = 22;
			port = getAuthDetails(port);

			try {
				session = connectSession(port);
				channel = (ChannelShell) session.openChannel("shell");
				in = channel.getInputStream();
				out = channel.getOutputStream();
				channel.connect();
			} catch (final IOException e) {
				logger.error("Error opening channel",e);
				return;
			} catch (final JSchException e) {
				logger.error("Error opening session or channel",e);
				return;
			}

			
			final ResizeTtyDelegate resizer = 
				new ResizeTtyDelegate(){
					public void resize(Dimension termSize, Dimension pixelSize) {
						channel.setPtySize(termSize.width,termSize.height, pixelSize.width, pixelSize.height);
					}
				};
				
			final TermIOBuffer termIOBuffer = new TermIOBuffer(in, out, resizer);
			final Emulator emulator = new EmulatorVT102(termPanel, termIOBuffer);
			
			termPanel.setEmulator(emulator);
			emulator.start();
			//termPanel.requestFocus();
			
			
			if (session != null) {
				session.disconnect();
				session = null;
			}

			termPanel.clear();

		}

		private Session connectSession(int port) throws JSchException {
			JSch jsch  = new JSch();
			Session session = null;
			
				session = jsch.getSession(user, host, port);

				final UserInfo ui = new SwingUserInfo();
				session.setUserInfo(ui);

				final java.util.Properties config = new java.util.Properties();

				config.put("compression.s2c", "zlib,none");
				config.put("compression.c2s", "zlib,none");

				session.setConfig(config);

				session.setTimeout(5000);
				session.connect();
				session.setTimeout(0);
			
			return session;
		}

		private int getAuthDetails(int port) {
			while (connectThread != null)
				try {
					final String userAtHost = JOptionPane.showInputDialog(termPanel,
							"Enter username@hostname", user + "@"
									+ host);
					if (userAtHost == null)
						return 0;
					final String user = userAtHost.substring(0, userAtHost
							.indexOf('@'));
					String host = userAtHost
							.substring(userAtHost.indexOf('@') + 1);
					if (host == null || host.length() == 0)
						continue;
					if (host.indexOf(':') != -1) {
						try {
							final String portString = host.substring(host
									.indexOf(':') + 1);
							port = Integer.parseInt(portString);
						} catch (final NumberFormatException eee) {
							logger.error("parsing port", eee);
						}
						host = host.substring(0, host.indexOf(':'));
					}
					Main.this.user = user;
					Main.this.host = host;
					break;
				} catch (final Exception ee) {
					logger.error("connecting ", ee);
					continue;
				}
			return port;
		}

	}
}
