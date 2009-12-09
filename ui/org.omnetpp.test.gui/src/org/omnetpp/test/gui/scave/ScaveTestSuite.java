/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.test.gui.scave;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ScaveTestSuite
    extends TestSuite
{
    public ScaveTestSuite() {
        addTestSuite(OpenFileTest.class);
        addTestSuite(InputsPageTest.class);
        addTestSuite(BrowseDataPageTest.class);
        addTestSuite(CreateDatasetsAndChartsTest.class);
        addTestSuite(DatasetsAndChartsPageTest.class);
        addTestSuite(DatasetViewTest.class);
        addTestSuite(ExportTest.class);
        addTestSuite(RefreshTest.class);
    }

    public static Test suite() {
        return new ScaveTestSuite();
    }
}
