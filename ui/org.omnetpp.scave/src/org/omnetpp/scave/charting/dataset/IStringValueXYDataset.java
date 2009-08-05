/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.
  
  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.charting.dataset;

/**
 * Implement this interface if custom formatting of the 
 * values is needed (e.g. enum values).
 *
 * @author tomi
 */
public interface IStringValueXYDataset extends IXYDataset {
	
	public String getXAsString(int series, int item);

	public String getYAsString(int series, int item);
}
