/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.
  
  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.launch;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

import org.omnetpp.launch.tabs.OmnetppMainTab;

/**
 * Defines the tab group for OMNeT simulation specific launches
 *
 * @author rhornig
 */
public class SimulationLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {

    public SimulationLaunchConfigurationTabGroup() {
    }

    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
                new OmnetppMainTab(),
                new EnvironmentTab(),
                new CommonTab()
            };
            setTabs(tabs);
    }

}
