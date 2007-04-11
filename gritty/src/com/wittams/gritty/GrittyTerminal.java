package com.wittams.gritty;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollBar;

public class GrittyTerminal extends JPanel{
	/**
	 * 
	 */
	private static final long serialVersionUID = -8213232075937432833L;
	private final TermPanel termPanel ;
	private final JScrollBar scrollBar;
	
	GrittyTerminal(){
		super(new BorderLayout());
		termPanel = new TermPanel();
		add(termPanel, BorderLayout.CENTER );
		scrollBar = new JScrollBar();
		add(scrollBar, BorderLayout.EAST );
		
		scrollBar.setModel(termPanel.getBoundedRangeModel() );
	}
	
	public TermPanel getTermPanel(){
		return termPanel;
	}

	public JScrollBar getScrollBar() {
		return scrollBar;
	}

}
