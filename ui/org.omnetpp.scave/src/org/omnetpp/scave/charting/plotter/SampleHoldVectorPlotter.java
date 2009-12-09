/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.charting.plotter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.omnetpp.common.canvas.ICoordsMapping;
import org.omnetpp.scave.charting.ILinePlot;
import org.omnetpp.scave.charting.dataset.IXYDataset;

/**
 * Sample-hold vector plotter
 *
 * @author Andras
 */
public class SampleHoldVectorPlotter extends VectorPlotter {

	boolean backward;

	public SampleHoldVectorPlotter(boolean backward) {
		this.backward = backward;
	}

	public void plot(ILinePlot plot, int series, GC gc, ICoordsMapping mapping, IChartSymbol symbol) {
		IXYDataset dataset = plot.getDataset();
		int n = dataset.getItemCount(series);
		if (n==0)
			return;

		// dataset index range to iterate over
		int[] range = indexRange(plot, series, gc, mapping);
		int first = range[0], last = range[1];

		//
		// Performance optimization: avoid painting the same pixels over and over,
		// by maintaining prevX, minY and maxY.
		//
		// Note that we could spare a few extra cycles by merging in symbol drawing
		// (instead of calling drawSymbols()), but since "pin" mode doesn't make much
		// sense (doesn't show much) for huge amounts of data points, we don't bother.
		//
		int prevX = mapping.toCanvasX(plot.transformX(dataset.getX(series, first)));
		int prevY = mapping.toCanvasY(plot.transformY(dataset.getY(series, first)));
		boolean prevIsNaN = Double.isNaN(plot.transformY(dataset.getY(series, first)));
		int maxY = prevY;
		int minY = prevY;

		// We are drawing vertical/horizontal lines, so turn off antialias (it is slow).
		int origAntialias = gc.getAntialias();
		gc.setAntialias(SWT.OFF);

		int[] dots = new int[] {1,2};
		gc.setLineDash(dots);

		if (!prevIsNaN && backward)
			gc.drawPoint(prevX, prevY);

		for (int i = first+1; i <= last; i++) {
			double value = plot.transformY(dataset.getY(series, i));

			// for testing:
			//if (i%5==0) value = 0.0/0.0; //NaN

			boolean isNaN = Double.isNaN(value); // see isNaN handling later

			int x = mapping.toCanvasX(plot.transformX(dataset.getX(series, i)));
			int y = mapping.toCanvasY(value); // note: this maps +-INF to +-MAXPIX, which works out just fine here

			// for testing:
			//if (i%5==1) x = prevX;

			if (x != prevX) {
				if (!isNaN && backward) {
					gc.setLineStyle(SWT.LINE_SOLID);
					gc.drawLine(prevX, y, x, y); // horizontal

					gc.setLineDash(dots);
					if (!prevIsNaN) {
						gc.drawLine(prevX, prevY, prevX, y); // vertical
					}
				}
				else if (!prevIsNaN && !backward) { // forward
					gc.setLineStyle(SWT.LINE_SOLID);
					gc.drawLine(prevX, prevY, x, prevY); // horizontal

					gc.setLineDash(dots);
					if (!isNaN) {
						gc.drawLine(x, prevY, x, y); // vertical
					}
				}

				minY = maxY = y;
			}
			else if (!isNaN) {
				// same x coord, only vertical line needs to be drawn
				if (y < minY) {
					gc.drawLine(x, minY, x, y);  // in lineDash(dots) mode
					minY = y;
				}
				else if (y > maxY) {
					gc.drawLine(x, maxY, x, y);  // in lineDash(dots) mode
					maxY = y;
				}
			}

			if (!isNaN) {  // condition is to handle case when first value on this x is NaN
				prevX = x;
				prevY = y;
			}
			prevIsNaN = isNaN;
		}

		if (!prevIsNaN && !backward)
			gc.drawPoint(prevX, prevY);

		// draw symbols
		gc.setAntialias(origAntialias);
		gc.setLineStyle(SWT.LINE_SOLID);
		plotSymbols(plot, series, gc, mapping, symbol);
	}
}
