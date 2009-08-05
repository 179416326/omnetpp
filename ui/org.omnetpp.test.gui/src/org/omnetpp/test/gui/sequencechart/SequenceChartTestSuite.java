/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.
  
  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.test.gui.sequencechart;

import junit.framework.Test;
import junit.framework.TestSuite;

public class SequenceChartTestSuite
    extends TestSuite
{
    public SequenceChartTestSuite() {
        addTestSuite(OpenFileTest.class);
    }

    public static Test suite() {
        return new SequenceChartTestSuite();
    }        
}
