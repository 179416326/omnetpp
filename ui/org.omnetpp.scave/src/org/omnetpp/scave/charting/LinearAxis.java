package org.omnetpp.scave.charting;

import static org.omnetpp.scave.charting.ChartDefaults.DEFAULT_AXIS_COLOR;
import static org.omnetpp.scave.charting.ChartDefaults.DEFAULT_AXIS_TITLE_FONT;
import static org.omnetpp.scave.charting.ChartDefaults.DEFAULT_GRID_COLOR;
import static org.omnetpp.scave.charting.ChartDefaults.DEFAULT_SHOW_GRID;
import static org.omnetpp.scave.charting.ChartDefaults.DEFAULT_TICK_FONT;
import static org.omnetpp.scave.charting.ChartDefaults.DEFAULT_X_AXIS_TITLE;
import static org.omnetpp.scave.charting.ChartDefaults.DEFAULT_Y_AXIS_TITLE;

import java.math.BigDecimal;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Transform;
import org.omnetpp.common.canvas.ICoordsMapping;

/**
 * Draws a (horizontal or vertical) chart axis, with the corresponding axis 
 * title, ticks and tick labels, and (vertical or horizontal) grid lines.
 * 
 * @author andras
 */
public class LinearAxis {
	private boolean vertical; // horizontal or vertical axis
	private int gap = 2;  // space between axis line and plot area (usually 0)
	private int majorTickLength = 4;
	private int minorTickLength = 2;
	private boolean drawGrid = DEFAULT_SHOW_GRID;
	private boolean drawTickLabels = true;
	private boolean drawTitle = true;

	private Rectangle bounds;
	private Insets insets;  // plot area = bounds minus insets

	private ICoordsMapping mapping;
	
	private String title; 
	private Font titleFont = DEFAULT_AXIS_TITLE_FONT;
	private Font tickFont = DEFAULT_TICK_FONT;

	public LinearAxis(ICoordsMapping mapping, boolean vertical) {
		this.mapping = mapping;
		this.vertical = vertical;
		this.title = vertical ? DEFAULT_Y_AXIS_TITLE : DEFAULT_X_AXIS_TITLE;
	}

	/**
	 * Modifies insets to accomodate room for axis title, ticks, tick labels etc.
	 * Also returns insets for convenience. 
	 */
	public Insets layoutHint(GC gc, Rectangle bounds, Insets insets) {
		// invalidate: they will have to be set via setLayout()
		this.bounds = null;
		this.insets = null;
		
		gc.setFont(tickFont);
		if (vertical) {
			int labelWidth = calculateTickLabelLength(gc, bounds);
			int titleHeight = calculateTitleSize(gc).y;  // but will be drawn 90 deg rotated
			insets.left = Math.max(insets.left, gap + majorTickLength + labelWidth + titleHeight + 4);
			insets.right = Math.max(insets.right, gap + majorTickLength + labelWidth + 4);
		}
		else {
			int labelHeight = calculateTickLabelHeight(gc);
			int titleHeight = calculateTitleSize(gc).y;
			insets.top = Math.max(insets.top, gap + majorTickLength + labelHeight + 4);
			insets.bottom = Math.max(insets.bottom, gap + majorTickLength + labelHeight + titleHeight + 4);
		}
		return insets;
	}

	private Point calculateTitleSize(GC gc) {
		gc.setFont(titleFont);
		return (!drawTitle || title.equals("")) ? new Point(0,0) : gc.textExtent(title);
	}

	private int calculateTickLabelHeight(GC gc) {
		if (!drawTickLabels)
			return 0;
		gc.setFont(tickFont);
		int labelHeight = gc.textExtent("999").y;
		return labelHeight;
	}

	private int calculateTickLabelLength(GC gc, Rectangle bounds) {
		// calculate longest tick label length
		if (!drawTickLabels)
			return 0;
		Ticks ticks = createTicks(bounds);
		int labelWidth = 0;
		if (ticks != null) {
			gc.setFont(tickFont);
			for (BigDecimal tick : ticks) {
				if (ticks.isMajorTick(tick)) {
					labelWidth = Math.max(labelWidth, gc.textExtent(tick.toPlainString()).x);
				}						
			}
		}
		return labelWidth;
	}

	/**
	 * Sets geometry info used for drawing. Plot area = bounds minus insets.
	 */
	public void setLayout(Rectangle bounds, Insets insets) {
		this.bounds = bounds.getCopy();
		this.insets = new Insets(insets);  
	}
	
	public void drawGrid(GC gc) {
		if (!drawGrid)
			return;

		// Note: when canvas caching is on, gc is the cached image, so the grid must be drawn 
		// to the whole clipping region (cached image area) not just the plot area    
		Rectangle rect = new Rectangle(gc.getClipping());
		Ticks ticks = createTicks(rect);
		if (ticks != null) {
			gc.setLineStyle(Graphics.LINE_DOT);
			gc.setForeground(DEFAULT_GRID_COLOR);
			for (BigDecimal tick : ticks) {
				if (vertical) {
					int y = mapping.toCanvasY(tick.doubleValue()); 
					if (y >= rect.y && y <= rect.bottom()) {
						gc.drawLine(rect.x, y, rect.right(), y);
					}
				}
				else {
					int x = mapping.toCanvasX(tick.doubleValue()); 
					if (x >= rect.x && x <= rect.right()) {
						gc.drawLine(x, rect.y, x, rect.bottom());
					}
				}
			}
		}
	}

	public void drawAxis(GC gc) {
		Rectangle plotArea = bounds.getCopy().crop(insets);
		
		// draw axis line and title
		gc.setLineWidth(1);
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setForeground(DEFAULT_AXIS_COLOR);
		gc.setFont(titleFont); 

		Point titleSize = gc.textExtent(title);
		if (vertical) {
			if (mapping.fromCanvasY(plotArea.bottom()) < 0 && mapping.fromCanvasY(plotArea.y) > 0)
				gc.drawLine(plotArea.x, mapping.toCanvasY(0), plotArea.right(), mapping.toCanvasY(0)); // x axis
			gc.drawLine(plotArea.x - gap, plotArea.y, plotArea.x - gap, plotArea.bottom());
			gc.drawLine(plotArea.right() + gap, plotArea.y, plotArea.right() + gap, plotArea.bottom());
			if (drawTitle) {
				Transform transform = new Transform(gc.getDevice());
				transform.rotate(-90);
				gc.setTransform(transform);
				gc.drawText(title, -(plotArea.y + plotArea.height / 2 + titleSize.x / 2), bounds.x, true);
				gc.setTransform(null);
				transform.dispose();
			}
		}
		else {
			if (mapping.fromCanvasX(plotArea.x) < 0 && mapping.fromCanvasX(plotArea.right()) > 0)
				gc.drawLine(mapping.toCanvasX(0), plotArea.y, mapping.toCanvasX(0), plotArea.bottom()); // y axis
			gc.drawLine(plotArea.x, plotArea.y - gap, plotArea.right(), plotArea.y - gap);
			gc.drawLine(plotArea.x, plotArea.bottom() + gap, plotArea.right(), plotArea.bottom() + gap);
			if (drawTitle)
				gc.drawText(title, plotArea.x + plotArea.width / 2 - titleSize.x / 2, bounds.bottom() - titleSize.y, true);
		}

		// draw ticks and labels
		Ticks ticks = createTicks(plotArea);
		if (ticks != null) {
			gc.setFont(tickFont);
			for (BigDecimal tick : ticks) {
				String label = tick.toPlainString();
				Point size = gc.textExtent(label);
				int tickLen = ticks.isMajorTick(tick) ? majorTickLength : minorTickLength; 
				if (vertical) {
					int y = mapping.toCanvasY(tick.doubleValue()); 
					if (y >= plotArea.y && y <= plotArea.bottom()) {
						gc.drawLine(plotArea.x - gap - tickLen, y, plotArea.x - gap, y);
						gc.drawLine(plotArea.right() + gap + tickLen, y, plotArea.right() + gap, y);
						if (drawTickLabels && ticks.isMajorTick(tick)) {
							gc.drawText(label, plotArea.x - gap - tickLen - size.x - 1, y - size.y / 2, true);
							gc.drawText(label, plotArea.right() + gap + tickLen + 3, y - size.y / 2, true);
						}
					}
				}
				else {
					int x = mapping.toCanvasX(tick.doubleValue()); 
					if (x >= plotArea.x && x <= plotArea.right()) {
						gc.drawLine(x, plotArea.y - gap - tickLen, x, plotArea.y - gap);
						gc.drawLine(x, plotArea.bottom() + gap + tickLen, x, plotArea.bottom() + gap);
						if (drawTickLabels && ticks.isMajorTick(tick)) {
							gc.drawText(label, x - size.x / 2 + 1, plotArea.y - gap - tickLen - size.y - 1, true);
							gc.drawText(label, x - size.x / 2 + 1, plotArea.bottom() + gap + tickLen + 1, true);
						}
					}
				}
			}
		}
	}

	protected Ticks createTicks(Rectangle plotArea) {
		if (vertical)
			return createTicks(mapping.fromCanvasY(plotArea.bottom()), mapping.fromCanvasY(plotArea.y), plotArea.height);
		else
			return createTicks(mapping.fromCanvasX(plotArea.x), mapping.fromCanvasX(plotArea.right()), plotArea.width);
	}

	protected static Ticks createTicks(double start, double end, int pixels) {
		return pixels != 0 ? new Ticks(start, end, 50 * (end-start) / pixels ) : null;
	}

	public Rectangle getBounds() {
		return bounds;  // note: no setter! use setLayout()
	}

	public Insets getInsets() {
		return insets;  // note: no setter! use setLayout()
	}

	public int getGap() {
		return gap;
	}

	public void setGap(int gap) {
		this.gap = gap;
	}

	public Font getTickFont() {
		return tickFont;
	}

	public void setTickFont(Font tickFont) {
		this.tickFont = tickFont;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Font getTitleFont() {
		return titleFont;
	}

	public void setTitleFont(Font titleFont) {
		this.titleFont = titleFont;
	}

	public boolean isGridVisible() {
		return drawGrid;
	}

	public void setGridVisible(boolean gridEnabled) {
		this.drawGrid = gridEnabled;
	}

	public boolean isDrawTickLabels() {
		return drawTickLabels;
	}

	public void setDrawTickLabels(boolean drawTickLabels) {
		this.drawTickLabels = drawTickLabels;
	}

	public boolean isDrawTitle() {
		return drawTitle;
	}

	public void setDrawTitle(boolean drawTitle) {
		this.drawTitle = drawTitle;
	}
}
