/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.
  
  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.charting.plotter;


/**
 * Interface for chart symbol drawing classes. Provides methods
 * for drawing the symbol, and getting/setting the size.
 * <p>
 * Note that size is not understood as a strict pixel size --
 * actual pixel size of the symbol is calculated so that symbols
 * of different shapes but same sizes contain roughly the same
 * number of pixels. That is, a triangle is roughly sqrt(2) times
 * higher than a square.
 * </p>
 *
 * @author andras
 */
public abstract class ChartSymbol implements IChartSymbol {
	protected int sizeHint = 5;

	public ChartSymbol() {
		setSizeHint(sizeHint);
	}

	public ChartSymbol(int sizeHint) {
		setSizeHint(sizeHint);
	}

	public int getSizeHint() {
		return sizeHint;
	}

	public void setSizeHint(int sizeHint) {
		this.sizeHint = sizeHint;
	}
	
}
