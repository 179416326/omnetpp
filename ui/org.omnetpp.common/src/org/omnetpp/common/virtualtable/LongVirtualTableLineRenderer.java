package org.omnetpp.common.virtualtable;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;

/**
 * This class serves debugging purposes.
 */
public class LongVirtualTableLineRenderer extends LabelProvider implements IVirtualTableLineRenderer<Long> {
	protected Font font = JFaceResources.getDefaultFont();

	protected int fontHeight;

	public void setInput(Object input) {
		// void
	}

	public void drawCell(GC gc, Long element, int index) {
		gc.drawText(element.toString(), 0, 0);
	}

	public int getLineHeight(GC gc) {
		if (fontHeight == 0) {
			Font oldFont = gc.getFont();
			gc.setFont(font);
			fontHeight = gc.getFontMetrics().getHeight();
			gc.setFont(oldFont);
		}

		return fontHeight + 2;
	}
}