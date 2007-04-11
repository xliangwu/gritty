package com.wittams.gritty;

import java.awt.Dimension;

public interface ResizeTtyDelegate {
	void resize(Dimension termSize, Dimension pixelSize);
}
