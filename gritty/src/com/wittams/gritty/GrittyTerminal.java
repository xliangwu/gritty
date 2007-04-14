package com.wittams.gritty;

import java.awt.BorderLayout;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JPanel;
import javax.swing.JScrollBar;

import org.apache.log4j.Logger;

public class GrittyTerminal extends JPanel{
	private static final Logger logger = Logger.getLogger(GrittyTerminal.class);
	private static final long serialVersionUID = -8213232075937432833L;
	
	private final TermPanel termPanel ;
	private final JScrollBar scrollBar;
	private Emulator emulator;
	private TtyChannel termIOBuffer;
	private Thread emuThread;
	private Tty tty;
	
	private AtomicBoolean sessionRunning = new AtomicBoolean();
	
	static enum BufferType{
		Back(){ 
			String getValue(GrittyTerminal term ){
				return term.getTermPanel().getBackBuffer().getLines();
			}
		},
		BackStyle(){ 
			String getValue(GrittyTerminal term ){
				return term.getTermPanel().getBackBuffer().getStyleLines();
			}
		},
		Damage(){ 
			String getValue(GrittyTerminal term ){
				return term.getTermPanel().getBackBuffer().getDamageLines();
			}
		},
		Scroll(){ 
			String getValue(GrittyTerminal term ){
				return term.getTermPanel().getScrollBuffer().getLines();
			}
		};
		
		abstract String getValue(GrittyTerminal term );
	};
	
	public GrittyTerminal(){
		super(new BorderLayout());
		termPanel = new TermPanel();
		add(termPanel, BorderLayout.CENTER );
		scrollBar = new JScrollBar();
		add(scrollBar, BorderLayout.EAST );
		
		scrollBar.setModel(termPanel.getBoundedRangeModel() );
		sessionRunning.set(false);
	}
	
	public TermPanel getTermPanel(){
		return termPanel;
	}

	public JScrollBar getScrollBar() {
		return scrollBar;
	}

	public void setTty(Tty tty){
		this.tty = tty;
		termIOBuffer = new TtyChannel(tty);
		emulator = new Emulator(termPanel, termIOBuffer);
		this.termPanel.setEmulator(emulator);
	}

	public void start(){
		if(!sessionRunning.get()){
			emuThread = new Thread(new EmulatorTask() );
			emuThread.start();
		}else{
			logger.error("Should not try to start session again at this point... ");
		}
	}
	
	public boolean isSessionRunning(){
		return sessionRunning.get();
	}
	
	class EmulatorTask implements Runnable{
		public void run(){
			try{
				sessionRunning.set(true);
				Thread.currentThread().setName(tty.getName());
				tty.init();
				Thread.currentThread().setName(tty.getName());
				emulator.start();
				
			}finally{
				tty.close();
				sessionRunning.set(false);
			}
		}		
	}
}
