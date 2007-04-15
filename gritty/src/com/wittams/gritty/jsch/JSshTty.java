/**
 * 
 */
package com.wittams.gritty.jsch;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.JOptionPane;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import com.wittams.gritty.Tty;
import com.wittams.gritty.swing.standalone.Main;

public class JSshTty implements Tty { 
	private InputStream in = null;
	private OutputStream out = null;
	private Session session;
	private ChannelShell channel;
	private int port = 22;
	
	private String user = System.getProperty("user.name").toLowerCase();
	private String host = "localhost";
	
	public void resize(Dimension termSize, Dimension pixelSize) {
		if(channel != null)
			channel.setPtySize(termSize.width,termSize.height, pixelSize.width, pixelSize.height);
	}
	
	public void close(){
		if (session != null) {
			session.disconnect();
			session = null;
			channel = null;
			in = null;
			out = null;
		}
	}

	public InputStream getInputStream(){
		return in;
	}

	public OutputStream getOuputStream(){
		return out;
	}
	
	public void init(){
		
		getAuthDetails();

		try {
			session = connectSession(port);
			channel = (ChannelShell) session.openChannel("shell");
			in = channel.getInputStream();
			out = channel.getOutputStream();
			channel.connect();
		} catch (final IOException e) {
			Main.logger.error("Error opening channel",e);
			return;
		} catch (final JSchException e) {
			Main.logger.error("Error opening session or channel",e);
			return;
		}

		
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

	private void getAuthDetails() {
		while (true){
			final String userAtHost = JOptionPane.showInputDialog(null,
					"Enter username@hostname", user + "@"
							+ host);
			if (userAtHost == null)
				return;
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
					Main.logger.error("parsing port", eee);
				}
				host = host.substring(0, host.indexOf(':'));
			}
			this.user = user;
			this.host = host;
			break;
		}	
	}

	public String getName(){
		return "ConnectRunnable";
	}

	public int read(byte[] buf, int offset, int length) throws IOException{
		return in.read(buf, offset, length);
	}

	public void write(byte[] bytes) throws IOException{
		out.write(bytes);
		out.flush();
	}

}