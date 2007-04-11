package com.wittams.gritty;

import java.awt.Dimension;

import com.wittams.gritty.Term.ResizeOrigin;

public interface ResizePanelDelegate {

	void resizedPanel(Dimension pixelDimension, ResizeOrigin origin);

}
