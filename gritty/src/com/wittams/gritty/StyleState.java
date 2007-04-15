package com.wittams.gritty;

import java.awt.Color;

public class StyleState {
	private  Style currentStyle = Style.EMPTY;
	
	private void rollStyle() {
		currentStyle = currentStyle.clone();		
	}
	
	public Style getCurrent() {
		return Style.getCanonicalStyle(currentStyle);
	}
	
	public void setBold(final boolean val) {
		rollStyle();
		currentStyle.setOption(Style.StyleOptions.BOLD, val);
	}

	public void resetColors() {
		rollStyle();
		currentStyle.setForeground(Style.FOREGROUND);
		currentStyle.setBackground(Style.BACKGROUND);
	}

	public void setCurrentBackground(final Color bg) {
		rollStyle();
		currentStyle.setBackground(bg);
	}

	public void setCurrentForeground(final Color fg) {
		rollStyle();
		currentStyle.setForeground(fg);
	}

	public void setReverseVideo() {
		rollStyle();
		currentStyle.setOption(Style.StyleOptions.REVERSE, true);
	}
	
}
