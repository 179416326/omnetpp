/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.
  
  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.cdt;

import java.util.LinkedHashMap;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.make.core.scannerconfig.IDiscoveredPathManager.IDiscoveredPathInfo;
import org.eclipse.cdt.make.core.scannerconfig.IDiscoveredPathManager.IPerProjectDiscoveredPathInfo;
import org.eclipse.cdt.make.internal.core.scannerconfig2.PerProjectSICollector;
import org.eclipse.cdt.managedbuilder.scannerconfig.IManagedScannerInfoCollector;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.omnetpp.cdt.makefile.MakefileTools;

/**
 * Special InfoCollector based on the CDT's GCC per project collector, but adds
 * the omnetpp/include directory to the includes.
 */
@SuppressWarnings("restriction")
public class OmnetppGCCScannerInfoCollector extends PerProjectSICollector
		implements IManagedScannerInfoCollector {

	@SuppressWarnings("unchecked")
	@Override
	public IDiscoveredPathInfo createPathInfoObject() {
		IPerProjectDiscoveredPathInfo pathInfoObject = (IPerProjectDiscoveredPathInfo)super.createPathInfoObject();

		// add include dirs needed for OMNeT++
		LinkedHashMap includeMap = pathInfoObject.getIncludeMap();
		try {
            ICProjectDescription projectDescription = CoreModel.getDefault().getProjectDescription(getContext().getProject());
            Assert.isNotNull(projectDescription);
            for (IPath path : MakefileTools.getOmnetppIncludeLocationsForProject(projectDescription))
                includeMap.put(path.toOSString(), false);
            pathInfoObject.setIncludeMap(includeMap);
        }
        catch (CoreException e) {
            Activator.logError(e);
        }
		return pathInfoObject;
	}
}
