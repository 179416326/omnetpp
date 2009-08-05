package org.omnetpp.scave.charting;

import static org.omnetpp.scave.charting.ChartDefaults.DEFAULT_AXIS_TITLE_FONT;
import static org.omnetpp.scave.charting.ChartDefaults.DEFAULT_BAR_BASELINE;
import static org.omnetpp.scave.charting.ChartDefaults.DEFAULT_BAR_OUTLINE_COLOR;
import static org.omnetpp.scave.charting.ChartDefaults.DEFAULT_BAR_PLACEMENT;
import static org.omnetpp.scave.charting.ChartDefaults.DEFAULT_INVERT_XY;
import static org.omnetpp.scave.charting.ChartDefaults.DEFAULT_LABELS_FONT;
import static org.omnetpp.scave.charting.ChartDefaults.DEFAULT_SHOW_GRID;
import static org.omnetpp.scave.charting.ChartDefaults.DEFAULT_X_AXIS_TITLE;
import static org.omnetpp.scave.charting.ChartDefaults.DEFAULT_X_LABELS_ROTATED_BY;
import static org.omnetpp.scave.charting.ChartDefaults.DEFAULT_Y_AXIS_TITLE;
import static org.omnetpp.scave.charting.ChartProperties.PROP_AXIS_TITLE_FONT;
import static org.omnetpp.scave.charting.ChartProperties.PROP_BAR_BASELINE;
import static org.omnetpp.scave.charting.ChartProperties.PROP_BAR_COLOR;
import static org.omnetpp.scave.charting.ChartProperties.PROP_BAR_PLACEMENT;
import static org.omnetpp.scave.charting.ChartProperties.PROP_LABEL_FONT;
import static org.omnetpp.scave.charting.ChartProperties.PROP_XY_GRID;
import static org.omnetpp.scave.charting.ChartProperties.PROP_XY_INVERT;
import static org.omnetpp.scave.charting.ChartProperties.PROP_X_AXIS_TITLE;
import static org.omnetpp.scave.charting.ChartProperties.PROP_X_LABELS_ROTATE_BY;
import static org.omnetpp.scave.charting.ChartProperties.PROP_Y_AXIS_LOGARITHMIC;
import static org.omnetpp.scave.charting.ChartProperties.PROP_Y_AXIS_TITLE;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.omnetpp.common.canvas.RectangularArea;
import org.omnetpp.common.color.ColorFactory;
import org.omnetpp.common.util.Converter;
import org.omnetpp.common.util.GeomUtils;
import org.omnetpp.scave.ScavePlugin;
import org.omnetpp.scave.charting.ChartProperties.BarPlacement;
import org.omnetpp.scave.charting.ChartProperties.ShowGrid;
import org.omnetpp.scave.charting.dataset.IDataset;
import org.omnetpp.scave.charting.dataset.ScalarDataset;
import org.omnetpp.scave.charting.plotter.IChartSymbol;
import org.omnetpp.scave.charting.plotter.SquareSymbol;

/**
 * Bar chart.
 *
 * @author tomi
 */
public class ScalarChart extends ChartCanvas {
	private ScalarDataset dataset;

	private LinearAxis valueAxis = new LinearAxis(this, true);
	private DomainAxis domainAxis = new DomainAxis();
	private BarPlot plot = new BarPlot();

	private int layoutDepth = 0; // how many layoutChart() calls are on the stack
	private Map<String,BarProperties> barProperties = new HashMap<String,BarProperties>();
	private static final String KEY_ALL = null;
	
	static class BarProperties {
		RGB color;
	}
	
	static class BarSelection implements IChartSelection {
		// TODO
	}
	
	public ScalarChart(Composite parent, int style) {
		super(parent, style);
		new Tooltip();
		
		this.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				setSelection(new BarSelection());
			}
		});
	}
	
	@Override
	public void dispose() {
		domainAxis.dispose();
		super.dispose();
	}

	@Override
	public void doSetDataset(IDataset dataset) {
		if (dataset != null && !(dataset instanceof ScalarDataset))
			throw new IllegalArgumentException("must be an ScalarDataset");
		
		this.dataset = (ScalarDataset)dataset;
		updateLegend();
		chartArea = plot.calculatePlotArea();
		updateArea();
		chartChanged();
	}
	
	public ScalarDataset getDataset() {
		return dataset;
	}

	private void updateLegend() {
		legend.clearLegendItems();
		legendTooltip.clearItems();
		IChartSymbol symbol = new SquareSymbol();
		if (dataset != null) {
			for (int i = 0; i < dataset.getColumnCount(); ++i) {
				legend.addLegendItem(plot.getBarColor(i), dataset.getColumnKey(i).toString(), symbol, false);
				legendTooltip.addItem(plot.getBarColor(i), dataset.getColumnKey(i).toString(), symbol, false);
			}
		}
	}
	
	/*=============================================
	 *               Properties
	 *=============================================*/
	public void setProperty(String name, String value) {
		// Titles
		if (PROP_X_AXIS_TITLE.equals(name))
			setXAxisTitle(value);
		else if (PROP_Y_AXIS_TITLE.equals(name))
			setYAxisTitle(value);
		else if (PROP_AXIS_TITLE_FONT.equals(name))
			setAxisTitleFont(Converter.stringToSwtfont(value));
		else if (PROP_LABEL_FONT.equals(name))
			setLabelFont(Converter.stringToSwtfont(value));
		else if (PROP_X_LABELS_ROTATE_BY.equals(name))
			setXAxisLabelsRotatedBy(Converter.stringToDouble(value));
		// Bars
		else if (PROP_BAR_BASELINE.equals(name))
			setBarBaseline(Converter.stringToDouble(value));
		else if (PROP_BAR_PLACEMENT.equals(name))
			setBarPlacement(Converter.stringToEnum(value, BarPlacement.class));
		else if (name.startsWith(PROP_BAR_COLOR))
			setBarColor(getKeyFrom(name), ColorFactory.asRGB(value));
		// Axes
		else if (PROP_XY_INVERT.equals(name))
			setInvertXY(Converter.stringToBoolean(value));
		else if (PROP_XY_GRID.equals(name))
			setShowGrid(Converter.stringToEnum(value, ShowGrid.class));
		else if (PROP_Y_AXIS_LOGARITHMIC.equals(name))
			throw new IllegalArgumentException("Logarithmic axis not yet supported"); //TODO
		else
			super.setProperty(name, value);
	}

	public String getTitle() {
		return title.getText();
	}

	public Font getTitleFont() {
		return title.getFont();
	}

	public String getXAxisTitle() {
		return domainAxis.title;
	}

	public void setXAxisTitle(String title) {
		if (title == null)
			title = DEFAULT_X_AXIS_TITLE;

		domainAxis.title = title;
		chartChanged();
	}

	public String getYAxisTitle() {
		return valueAxis.getTitle();
	}

	public void setYAxisTitle(String title) {
		valueAxis.setTitle(title==null ? DEFAULT_Y_AXIS_TITLE : title);
		chartChanged();
	}

	public Font getAxisTitleFont() {
		return domainAxis.titleFont;
	}

	public void setAxisTitleFont(Font font) {
		if (font != null) {
			domainAxis.titleFont = font;
			valueAxis.setTitleFont(font);
			chartChanged();
		}
	}

	public void setLabelFont(Font font) {
		if (font == null)
			font = DEFAULT_LABELS_FONT;
		domainAxis.labelsFont = font;
		valueAxis.setTickFont(font);
		chartChanged();
	}

	public void setXAxisLabelsRotatedBy(Double angle) {
		if (angle == null)
			angle = DEFAULT_X_LABELS_ROTATED_BY;
		domainAxis.rotation = Math.max(0, Math.min(90, angle));
		chartChanged();
	}

	public Double getBarBaseline() {
		return plot.barBaseline;
	}

	public void setBarBaseline(Double value) {
		if (value == null)
			value = DEFAULT_BAR_BASELINE;

		plot.barBaseline = value;
		chartChanged();
	}

	public BarPlacement getBarPlacement() {
		return plot.barPlacement;
	}

	public void setBarPlacement(BarPlacement value) {
		if (value == null)
			value = DEFAULT_BAR_PLACEMENT;

		plot.barPlacement = value;
		chartChanged();
	}

	public void setInvertXY(Boolean value) {
		if (value == null)
			value = DEFAULT_INVERT_XY;

		plot.invertXY = value;
		chartChanged();
	}

	public void setShowGrid(ShowGrid value) {
		if (value == null)
			value = DEFAULT_SHOW_GRID;

		valueAxis.setShowGrid(value);
		chartChanged();
	}
	
	public RGB getBarColor(String key) {
		BarProperties barProps = getBarProperties(key);
		if (barProps == null || barProps.color == null)
			barProps = getDefaultBarProperties();
		return barProps != null ? barProps.color : null;
	}
	
	public void setBarColor(String key, RGB color) {
		BarProperties barProps = getOrCreateBarProperties(key);
		barProps.color = color;
		updateLegend();
		chartChanged();
	}
	
	private String getKeyFrom(String propertyKey) {
		int index = propertyKey.indexOf('/');
		return index >= 0 ? propertyKey.substring(index + 1) : KEY_ALL;
	}
	
	public String getKeyFor(int columnIndex) {
		if (columnIndex >= 0 && columnIndex < dataset.getColumnCount())
			return dataset.getColumnKey(columnIndex);
		else
			return null;
	}
	
	public BarProperties getBarProperties(String key) {
		return (key != null ? barProperties.get(key) : null);
	}
	
	public BarProperties getDefaultBarProperties() {
		return barProperties.get(KEY_ALL);
	}
	
	private BarProperties getOrCreateBarProperties(String key) {
		BarProperties barProps = getBarProperties(key);
		if (barProps == null) {
			barProps = new BarProperties();
			barProperties.put(key, barProps);
		}
		return barProps;
	}

	/*=============================================
	 *               Drawing
	 *=============================================*/

	@Override
	protected void doLayoutChart() {
		// prevent nasty infinite layout recursions
		if (layoutDepth>0)
			return; 
		
		// ignore initial invalid layout request
		if (getClientArea().width==0 && getClientArea().height==0)
			return;
		
		layoutDepth++;
		GC gc = new GC(Display.getCurrent());
		System.out.println("layoutChart(), level "+layoutDepth);

		try {
			// preserve zoomed-out state while resizing
			boolean shouldZoomOutX = getZoomX()==0 || isZoomedOutX();
			boolean shouldZoomOutY = getZoomY()==0 || isZoomedOutY();

			// Calculate space occupied by title and legend and set insets accordingly
			Rectangle area = new Rectangle(getClientArea());
			Rectangle remaining = legendTooltip.layout(gc, area);
			remaining = title.layout(gc, area);
			remaining = legend.layout(gc, remaining);

			Rectangle mainArea = remaining.getCopy();
			Insets insetsToMainArea = new Insets();
			domainAxis.layoutHint(gc, mainArea, insetsToMainArea);
			// postpone valueAxis.layoutHint() as it wants to use coordinate mapping which is not yet set up (to calculate ticks)
			insetsToMainArea.left = 50; insetsToMainArea.right = 30; // initial estimate for y axis

			// tentative plotArea calculation (y axis ticks width missing from the picture yet)
			Rectangle plotArea = mainArea.getCopy().crop(insetsToMainArea);
			setViewportRectangle(new org.eclipse.swt.graphics.Rectangle(plotArea.x, plotArea.y, plotArea.width, plotArea.height));

			if (shouldZoomOutX)
				zoomToFitX();
			if (shouldZoomOutY)
				zoomToFitY();
			validateZoom(); //Note: scrollbar.setVisible() triggers Resize too

			// now the coordinate mapping is set up, so the y axis knows what tick labels
			// will appear, and can calculate the occupied space from the longest tick label.
			valueAxis.layoutHint(gc, mainArea, insetsToMainArea);

			// now we have the final insets, set it everywhere again 
			domainAxis.setLayout(mainArea, insetsToMainArea);
			valueAxis.setLayout(mainArea, insetsToMainArea);
			plotArea = mainArea.getCopy().crop(insetsToMainArea);
			legend.layoutSecondPass(plotArea);
			//FIXME how to handle it when plotArea.height/width comes out negative??
			plot.layout(gc, plotArea);
			setViewportRectangle(new org.eclipse.swt.graphics.Rectangle(plotArea.x, plotArea.y, plotArea.width, plotArea.height));

			if (shouldZoomOutX)
				zoomToFitX();
			if (shouldZoomOutY)
				zoomToFitY();
			validateZoom(); //Note: scrollbar.setVisible() triggers Resize too
		} 
		catch (Exception e) {
			ScavePlugin.logError(e);
		}
		finally {
			gc.dispose();
			layoutDepth--;
		}
	}
	
	@Override
	protected void paintCachableLayer(GC gc) {
		resetDrawingStylesAndColors(gc);
		gc.setAntialias(antialias ? SWT.ON : SWT.OFF);
		
		valueAxis.drawGrid(gc);
		plot.draw(gc);
	}
	
	private Rectangle getPlotRectangle() {
		return plot.getRectangle();
	}

	@Override
	protected void paintNoncachableLayer(GC gc) {
		resetDrawingStylesAndColors(gc);
		gc.setAntialias(antialias ? SWT.ON : SWT.OFF);
		
		paintInsets(gc);
		title.draw(gc);
		legend.draw(gc);
		valueAxis.drawAxis(gc);
		domainAxis.draw(gc);
		drawRubberband(gc);
		legendTooltip.draw(gc);
		drawStatusText(gc);
	}
	
	

	@Override
	public void setZoomX(double zoomX) {
		super.setZoomX(zoomX);
		chartChanged();
	}

	@Override
	public void setZoomY(double zoomY) {
		super.setZoomY(zoomY);
		chartChanged();
	}



	/**
	 * Draws the bars of the bar chart. 
	 */
	class BarPlot {
		private Rectangle rect = new Rectangle(0,0,1,1);
		private int widthBar = 10;
		private int hgapMinor = 5;
		private int hgapMajor = 20;
		private double horizontalInset = 1.0;   // left/right inset relative to the bars' width 
		private double verticalInset = 0.1; // top inset relative to the height of the highest bar
		
		private double barBaseline = DEFAULT_BAR_BASELINE;
		private BarPlacement barPlacement = DEFAULT_BAR_PLACEMENT;
		private Color barOutlineColor = DEFAULT_BAR_OUTLINE_COLOR;
		private Boolean invertXY = DEFAULT_INVERT_XY;
		
		public Rectangle getRectangle() {
			return rect;
		}
		
		public Rectangle layout(GC gc, Rectangle rect) {
			this.rect = rect.getCopy();
			return rect;
		}
		
		public void draw(GC gc) {
			if (dataset != null) {
				resetDrawingStylesAndColors(gc);
				Graphics graphics = new SWTGraphics(gc);
				graphics.pushState();

				Rectangle clip = graphics.getClip(new Rectangle());

				int cColumns = dataset.getColumnCount();
				int[] indices = getRowColumnsInRectangle(clip);
				for (int i = indices[0]; i <= indices[1]; ++i) {
					int row = i / cColumns;
					int column = i % cColumns;
					drawBar(graphics, row, column);
				}
				graphics.popState();
				graphics.dispose();
			}
		}
		
		protected void drawBar(Graphics graphics, int row, int column) {
			Rectangle rect = getBarRectangle(row, column);
			rect.width = Math.max(rect.width, 1);
			rect.height = Math.max(rect.height, 1);
			graphics.setBackgroundColor(getBarColor(column));
			graphics.fillRectangle(rect);
			if (rect.width >= 4 && rect.height >= 3) {
				graphics.setForegroundColor(barOutlineColor);
				graphics.drawRectangle(rect.getCropped(new Insets(0,0,0,0)));
			}
		}
		
		protected int[] getRowColumnsInRectangle(org.eclipse.draw2d.geometry.Rectangle rect) {
			int[] result = new int[2];
			result[0] = getRowColumn(rect.x, true);
			result[1] = getRowColumn(rect.x + rect.width, false);
			return result;
		}
		
		private int getRowColumn(double x, boolean before) {
			int cRows = dataset.getRowCount();
			int cColumns = dataset.getColumnCount();
			return before ? 0 : (cRows*cColumns-1);
		}

		public int findRowColumn(double x, double y) {
			if (dataset == null)
				return -1;
			int cRows = dataset.getRowCount();
			int cColumns = dataset.getColumnCount();
			x -= horizontalInset * widthBar;
			if (x < 0)
				return -1;
			double rowWidth = cColumns * widthBar + (cColumns - 1) * hgapMinor;
			int row = (int) Math.floor(x / (rowWidth+hgapMajor));
			if (row >= cRows)
				return -1;  // x too big
			x -= row * (rowWidth+hgapMajor);
			if (x > rowWidth)
				return -1;  // x falls in a major gap
			int column = (int) Math.floor(x / (widthBar + hgapMinor));
			x -= column * (widthBar+hgapMinor);
			if (x > widthBar)
				return -1;  // x falls in a minor gap
			double value = dataset.getValue(row, column);
			if (value >= barBaseline ? (y < barBaseline || y > value) : (y > barBaseline || y < value))
				return -1;  // above or below actual bar 
			return row * cColumns + column; 
		}
		
		protected Color getBarColor(int column) {
			RGB color = ScalarChart.this.getBarColor(getKeyFor(column));
			if (color != null)
				return new Color(null, color);
			else
				return ColorFactory.getGoodDarkColor(column);
		}
		
		protected Rectangle getBarRectangle(int row, int column) {
			int x = toCanvasX(getLeftX(row, column));
			int y = toCanvasY(getTopY(row, column));
			int width = toCanvasDistX(getRightX(row,column) - getLeftX(row, column));
			int height = toCanvasDistY(getTopY(row, column) - getBottomY(row, column));
			return new Rectangle(x, y, width, height);
		}
		
		public RectangularArea calculatePlotArea() {
			if (dataset == null)
				return new RectangularArea(0, 0, 1, 1);
			
			int cRows = dataset.getRowCount();
			int cColumns = dataset.getColumnCount();
			double minX = getLeftX(0, 0);
			double maxX = getRightX(cRows - 1, cColumns - 1);
			double minY = Double.POSITIVE_INFINITY;
			double maxY = Double.NEGATIVE_INFINITY;
			for (int row = 0; row < cRows; ++row)
				for (int column = 0; column < cColumns; ++column) {
					minY = Math.min(minY, plot.getBottomY(row, column));
					maxY = Math.max(maxY, plot.getTopY(row, column));
				}
			if (minY > maxY) { // no data points
				minY = 0.0;
				maxY = 1.0;
			}
			double height = maxY - minY;
			return new RectangularArea(minX - horizontalInset * widthBar, minY,
									   maxX + horizontalInset * widthBar, maxY + verticalInset * height);
		}
		
		protected double getLeftX(int row, int column) {
			int cColumns = dataset.getColumnCount();
			double rowWidth = cColumns * widthBar + (cColumns - 1) * hgapMinor;
			return horizontalInset * widthBar + row * (rowWidth + hgapMajor) + column * (widthBar + hgapMinor); 
		}
		
		protected double getRightX(int row, int column) {
			return getLeftX(row, column) + widthBar;
		}
		
		protected double getTopY(int row, int column) {
			double value = dataset.getValue(row, column);
			return (value > barBaseline ? value : barBaseline);
		}
		
		protected double getBottomY(int row, int column) {
			double value = dataset.getValue(row, column);
			return (value < barBaseline ? value : barBaseline);
		}
	}


	static class GroupLabelLayoutData {
		TextLayout textLayout;
		Dimension size;
		Dimension rotatedSize;
	}

	/**
	 * Domain axis for bar chart.
	 */
	class DomainAxis {
		private Rectangle rect; // strip below the plotArea where the axis text etc goes
		private GroupLabelLayoutData[] layoutData;
		private int labelsHeight;
		private String title = DEFAULT_X_AXIS_TITLE;
		private Font titleFont = DEFAULT_AXIS_TITLE_FONT;
		private Font labelsFont = DEFAULT_LABELS_FONT;
		private double rotation = DEFAULT_X_LABELS_ROTATED_BY;
		private int gap = 4;  // between chart and axis 
		
		public void dispose() {
			for (GroupLabelLayoutData data : layoutData)
				if (data != null && data.textLayout != null)
					data.textLayout.dispose();
		}
		
		public Ticks getTicks() {
			return new Ticks(1.0, 0.0, 1.0); // TODO
		}

		/**
		 * Modifies insets to accomodate room for axis title, ticks, tick labels etc.
		 * Also returns insets for convenience. 
		 */
		public Insets layoutHint(GC gc, Rectangle rect, Insets insets) {

			// measure title height and labels height
			gc.setFont(titleFont);
			int titleHeight = title.equals("") ? 0 : gc.textExtent(title).y;
			gc.setFont(labelsFont);
			labelsHeight = 0;
			if (dataset != null) {
				int cColumns = dataset.getColumnCount();
				int cRows = dataset.getRowCount();
				if (layoutData != null) {
					for (GroupLabelLayoutData data : layoutData)
						data.textLayout.dispose();
				}
				layoutData = new GroupLabelLayoutData[cRows];
				for (int row = 0; row < cRows; ++row) {
					int left = plot.getBarRectangle(row, 0).x;
					int right = plot.getBarRectangle(row, cColumns - 1).right();
					int width = right - left;
					layoutData[row] = layoutGroupLabel(dataset.getRowKey(row), labelsFont, rotation, gc, width, 0);
					labelsHeight = Math.max(labelsHeight, layoutData[row].rotatedSize.height);
					//System.out.println("labelsheight: "+labelsHeight);
				}
			}
			
			// modify insets with space required
			insets.top = Math.max(insets.top, 10); // leave a few pixels at the top
			insets.bottom = Math.max(insets.bottom, gap + labelsHeight + titleHeight + 8);
			
			return insets;
		}
		
		private GroupLabelLayoutData layoutGroupLabel(String label, Font font, double rotation , GC gc, int maxWidth, int maxHeight) {
			GroupLabelLayoutData data = new GroupLabelLayoutData();
			data.textLayout = new TextLayout(gc.getDevice());
			data.textLayout.setText(label);
			data.textLayout.setFont(font);
			data.textLayout.setAlignment(SWT.CENTER);
			data.textLayout.setWidth(Math.max(maxWidth, 10));
			System.out.format("width=%s%n", maxWidth);
			if (data.textLayout.getLineCount() > 1) {
				// TODO soft hyphens are visible even when no break at them 
				data.textLayout.setText(label.replace(';', '\u00ad'));
			}
			org.eclipse.swt.graphics.Rectangle bounds = data.textLayout.getBounds(); 
			data.size = new Dimension(bounds.width, bounds.height); //new Dimension(gc.textExtent(data.label));
			data.rotatedSize = GeomUtils.rotatedSize(data.size, rotation);
			return data;
		}

		/**
		 * Sets geometry info used for drawing. Plot area = bounds minus insets.
		 */
		public void setLayout(Rectangle bounds, Insets insets) {
			rect = bounds.getCopy();
			int bottom = rect.bottom();
			rect.height = insets.bottom;
			rect.y = bottom - rect.height;
			rect.x += insets.left;
			rect.width -= insets.getWidth();
		}
		
		public void draw(GC gc) {
			org.eclipse.swt.graphics.Rectangle oldClip = gc.getClipping(); // graphics.popState() doesn't restore it!
			Graphics graphics = new SWTGraphics(gc);
			graphics.pushState();

			graphics.setClip(rect);
			
			graphics.setLineStyle(SWT.LINE_SOLID);
			graphics.setLineWidth(1);
			graphics.setForegroundColor(ColorFactory.BLACK);

			Rectangle plotRect = getPlotRectangle();

			// draw labels
			if (dataset != null) {
				int cColumns = dataset.getColumnCount();
				graphics.setFont(labelsFont);
				graphics.drawText("", 0, 0); // force Graphics push the font setting into GC
				graphics.pushState();
				for (int row = 0; row < dataset.getRowCount(); ++row) {
					int left = plot.getBarRectangle(row, 0).x;
					int right = plot.getBarRectangle(row, cColumns - 1).right();

					graphics.restoreState();
					graphics.drawLine(left, rect.y + gap, right, rect.y + gap);
					
					GroupLabelLayoutData data = layoutData[row];
					//String label = data.label;
					Dimension size = data.size;
					Dimension rotatedSize = data.rotatedSize;
					
					graphics.translate((left + right) / 2 - rotatedSize.width / 2, rect.y + gap + 1 + size.height/2);
					graphics.rotate((float)rotation);
					graphics.drawTextLayout(data.textLayout, 0, -size.height/2);
					//graphics.drawText(label, 0, -size.height/2);
				}
				graphics.popState();
			}
			
			// draw axis title
			graphics.setFont(titleFont);
			graphics.drawText("", 0, 0); // force Graphics push the font setting into GC
			Point size = gc.textExtent(title);
			graphics.drawText(title, plotRect.x + (plotRect.width - size.x) / 2, rect.bottom() - size.y - 1);

			graphics.popState();
			graphics.dispose();
			gc.setClipping(oldClip); // graphics.popState() doesn't restore it!
		}
	}
	
	/**
	 * Bar chart tooltip
	 * @author Andras
	 */
	class Tooltip {
		private DefaultInformationControl tooltipWidget; // the current tooltip (Note: SWT's Tooltip cannot be used as it wraps lines)
		
		public Tooltip() {
			addMouseTrackListener(new MouseTrackAdapter() {
				public void mouseHover(MouseEvent e) {
					showTooltip(e.x, e.y);
				}
			});
			addMouseMoveListener(new MouseMoveListener() {
				public void mouseMove(MouseEvent e) {
					if (tooltipWidget != null) {
						tooltipWidget.dispose();
						tooltipWidget = null;
					}
				}
			});
		}
		
		private void showTooltip(int x, int y) {
			int rowColumn = plot.findRowColumn(fromCanvasX(x), fromCanvasY(y));
			if (rowColumn != -1) {
				int numColumns = dataset.getColumnCount();
				int row = rowColumn / numColumns;
				int column = rowColumn % numColumns;
				ScalarDataset dataset = getDataset();

				String tooltipText = (String) dataset.getColumnKey(column) + "\nvalue: " + dataset.getValue(row, column);
				tooltipWidget = new DefaultInformationControl(getShell());
				tooltipWidget.setInformation(tooltipText);
				tooltipWidget.setLocation(toDisplay(x,y+20));
				Point size = tooltipWidget.computeSizeHint();
				tooltipWidget.setSize(size.x, size.y);
				tooltipWidget.setVisible(true);
			}
		}
	}
}
