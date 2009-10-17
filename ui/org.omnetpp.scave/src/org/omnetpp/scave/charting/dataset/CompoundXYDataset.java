/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.
  
  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.charting.dataset;

import org.omnetpp.common.engine.BigDecimal;
import org.omnetpp.scave.engine.Statistics;
import org.omnetpp.scave.model2.StatUtils;

public class CompoundXYDataset extends XYDatasetSupport implements IAveragedXYDataset {

	private IXYDataset[] seriesToDatasetMap;
	private int[] seriesToOffsetMap;
	
	public CompoundXYDataset(IXYDataset... datasets) {
		int seriesCount = 0;
		for (int i = 0; i < datasets.length; ++i)
			seriesCount += datasets[i].getSeriesCount();
		
		seriesToDatasetMap = new IXYDataset[seriesCount];
		seriesToOffsetMap = new int[seriesCount];
		int offset = 0;
		for (IXYDataset dataset : datasets) {
			int count = dataset.getSeriesCount();
			for (int series = 0; series < count; ++series) {
				seriesToDatasetMap[offset + series] = dataset;
				seriesToOffsetMap[offset + series] = offset;
			}
			offset += count;
		}
	}

	public String getTitle(String format) {
		return null;
	}

	public int getSeriesCount() {
		return seriesToDatasetMap.length;
	}

	public String getSeriesKey(int series) {
		return seriesToDatasetMap[series].getSeriesKey(series - seriesToOffsetMap[series]);
	}

	public int getItemCount(int series) {
		return seriesToDatasetMap[series].getItemCount(series - seriesToOffsetMap[series]);
	}

	public double getX(int series, int item) {
		return seriesToDatasetMap[series].getX(series - seriesToOffsetMap[series], item);
	}

	public BigDecimal getPreciseX(int series, int item) {
		return seriesToDatasetMap[series].getPreciseX(series - seriesToOffsetMap[series], item);
	}

	public Statistics getXStatistics(int series, int item) {
		IXYDataset dataset = seriesToDatasetMap[series];
		return dataset instanceof IAveragedXYDataset ?
				((IAveragedXYDataset)dataset).getXStatistics(series - seriesToOffsetMap[series], item) :
				StatUtils.singleValueStatistics(getX(series, item));
	}

	public double getY(int series, int item) {
		return seriesToDatasetMap[series].getY(series - seriesToOffsetMap[series], item);
	}

	public BigDecimal getPreciseY(int series, int item) {
		return seriesToDatasetMap[series].getPreciseY(series - seriesToOffsetMap[series], item);
	}

	public Statistics getYStatistics(int series, int item) {
		IXYDataset dataset = seriesToDatasetMap[series];
		return dataset instanceof IAveragedXYDataset ?
				((IAveragedXYDataset)dataset).getYStatistics(series - seriesToOffsetMap[series], item) :
				StatUtils.singleValueStatistics(getY(series, item));
	}
}
