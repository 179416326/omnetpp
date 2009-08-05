package org.omnetpp.scave.charting.plotter;

import org.eclipse.draw2d.Graphics;

/**
 * Draws a square symbol.
 * 
 * @author andras
 */
public class SquareSymbol extends ChartSymbol {
	private int size;
	
	public SquareSymbol() {
	}

	public SquareSymbol(int size) {
		super(size);
	}

	@Override
	public void setSizeHint(int sizeHint) {
		super.setSizeHint(sizeHint);
		size = sizeHint|1; // make odd number
	}
	
	
	public void drawSymbol(Graphics graphics, int x, int y) {
		if (size<=0) {
			// nothing
		}
		else if (size==1) {
			graphics.drawPoint(x, y);
		}
		else {
			graphics.setBackgroundColor(graphics.getForegroundColor());
			graphics.fillRectangle(x-size/2, y-size/2, size, size); //XXX make filled/unfilled version
		}
	}
}
