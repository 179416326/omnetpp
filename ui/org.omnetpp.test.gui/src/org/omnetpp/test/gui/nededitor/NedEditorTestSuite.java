/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.test.gui.nededitor;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.omnetpp.test.gui.nededitor.graphical.GraphicalEditorTestSuite;
import org.omnetpp.test.gui.nededitor.main.MultipageNedEditorTestSuite;
import org.omnetpp.test.gui.nededitor.textual.TextuallEditorTestSuite;
import org.omnetpp.test.gui.nededitor.view.NedViewEditorTestSuite;

public class NedEditorTestSuite
    extends TestSuite
{
    public NedEditorTestSuite() {
        addTestSuite(NedFileWizardTest.class);
        addTestSuite(NedPathTest.class);
        addTest(MultipageNedEditorTestSuite.suite());
        addTest(GraphicalEditorTestSuite.suite());
        addTest(TextuallEditorTestSuite.suite());
        addTest(NedViewEditorTestSuite.suite());
    }

    public static Test suite() {
        return new NedEditorTestSuite();
    }
}
