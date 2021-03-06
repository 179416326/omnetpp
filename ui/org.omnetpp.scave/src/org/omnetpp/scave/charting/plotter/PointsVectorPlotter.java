/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.charting.plotter;

import org.eclipse.draw2d.Graphics;
import org.omnetpp.common.canvas.ICoordsMapping;
import org.omnetpp.common.canvas.LargeGraphics;
import org.omnetpp.scave.charting.ILinePlot;

/**
 * Vector plotter simply drawing pixels (and no lines or symbols). Note that despite
 * common sense, LinesVectorPlotter is *faster* than this one, probably due to the
 * HashSet in plotSymbols().
 *
 * @author Andras
 */
public class PointsVectorPlotter extends VectorPlotter {

    public boolean plot(ILinePlot plot, int series, Graphics graphics, ICoordsMapping mapping, IChartSymbol symbol, int timeLimitMillis) {
        //
        // Note: profiling shows that substituting the gc.drawPoint() call
        // into the plotSymbols() would make no measurable difference in
        // performance, so we just invoke the stock plotSymbols() here.
        //
        ChartSymbol pixelSymbol = new ChartSymbol() {
            public void drawSymbol(Graphics graphics, long x, long y) {
                LargeGraphics.drawPoint(graphics, x, y);
            }
        };

        return plotSymbols(plot, series, graphics, mapping, pixelSymbol, timeLimitMillis);
    }
}
