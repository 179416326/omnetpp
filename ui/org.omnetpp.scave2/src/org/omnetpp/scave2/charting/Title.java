package org.omnetpp.scave2.charting;

import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.draw2d.geometry.Rectangle;


/**
 * This class layouts and draws a title on a vector chart.
 *
 * @author tomi
 */
public class Title {

	private String text;
	private Font font;
	private Rectangle bounds;
	
	public Title() {
	}
	
	public Title(String text, Font font) {
		this.text = text;
		this.font = font;
	}
	
	public String getText() {
		return text;
	}
	
	public void setText(String text) {
		this.text = text;
	}
	
	public Font getFont() {
		return font;
	}
	
	public void setFont(Font font) {
		this.font = font;
	}
	
	public Rectangle layout(GC gc, Rectangle parent) {
		if (text == null || text.length() == 0)
			return parent;
		
		gc.setFont(font);
		
		Point size = gc.stringExtent(text);
		bounds = new Rectangle(parent.x + (parent.width - size.x) / 2, parent.y, size.x, size.y);
		
		return new Rectangle(parent.x, Math.min(bounds.y + bounds.height, parent.y + parent.height),
				parent.width, Math.max(parent.height - bounds.height, 0));
	}
	
	public void draw(GC gc) {
		if (text == null || text.length() == 0)
			return;
		
		gc.setFont(font);
		gc.drawString(text, bounds.x, bounds.y, true);
	}
}
