/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.cdt.msvc;


import java.util.List;
import java.util.Map;

import org.eclipse.cdt.make.core.scannerconfig.IScannerInfoCollector3;
import org.eclipse.cdt.make.core.scannerconfig.InfoContext;
import org.eclipse.cdt.make.core.scannerconfig.ScannerInfoTypes;
import org.eclipse.cdt.make.core.scannerconfig.IDiscoveredPathManager.IDiscoveredPathInfo;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author Doug Schaefer
 *
 */
public class MSVCScannerInfoCollector implements IScannerInfoCollector3 {
    private InfoContext context;

	@SuppressWarnings("unchecked")
    public void contributeToScannerConfig(Object resource, Map scannerInfo) {
	}

	@SuppressWarnings("unchecked")
    public List getCollectedScannerInfo(Object resource, ScannerInfoTypes type) {
		return null;
	}

	public IDiscoveredPathInfo createPathInfoObject() {
	    Assert.isTrue(context != null);
		return new MSVCDiscoveredPathInfo(context.getProject());
	}

	public void setInfoContext(InfoContext context) {
	    this.context = context;
	}

	public void setProject(IProject project) {
	    // CDT apparently never invokes this
	}

	public void updateScannerConfiguration(IProgressMonitor monitor) throws CoreException {
	}

}
