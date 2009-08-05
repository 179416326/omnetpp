/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.
  
  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.actions;

/**
 * Opens a new dataset window
 */
public class NewVectorDatasetActionDelegate extends NewDocActionDelegate {

	private static int counter = 0;

	protected String getNewFileName() {
		return "Untitled vector dataset "+(++counter)+".vdataset";
	}
}
