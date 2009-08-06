/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.
  
  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.test.gui.nededitor.graphical;


import junit.framework.Test;
import junit.framework.TestSuite;

public class GraphicalEditorTestSuite
	extends TestSuite
{
	public GraphicalEditorTestSuite() {
        addTestSuite(CreateWithGraphicalEditorTest.class);
		addTestSuite(DeleteWithGraphicalEditorTest.class);
        addTestSuite(CreateComplexModelWithGraphicalEditorTest.class);
        addTestSuite(AutoLayoutTest.class);
        addTestSuite(ConnectionChooserTest.class);
	}

    public static Test suite() {
        return new GraphicalEditorTestSuite();
    }        
}
