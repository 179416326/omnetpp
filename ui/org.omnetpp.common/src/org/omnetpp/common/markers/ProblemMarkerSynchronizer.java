package org.omnetpp.common.markers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.omnetpp.common.CommonPlugin;

/**
 * Solves the following problem: when an editor continuously analyzes
 * its contents (for example from a reconciler job) and converts
 * errors/warnings into markers, a common problem is that most markers
 * get deleted and added back each time the analyzer runs. This 
 * triggers excessive refreshes in the UI (in text editor margin
 * and the Problems view).\
 * 
 * This class collects would-be markers in a table, and then 
 * synchronizes this table to the actual IFile markers. This way,
 * only *real* marker changes reach the workspace, and excessive
 * updates are prevented. 
 * 
 * @author Andras
 */
public class ProblemMarkerSynchronizer {
	private static class MarkerData {
		String type;
		Map<String, Object> attrs;
	}

	// data for markers to synchronize
	private HashMap<IFile, List<MarkerData>> markerTable = new HashMap<IFile, List<MarkerData>>();
	private String markerBaseType;

	// statistics
	private int markersAdded = 0;
	private int markersRemoved = 0;

	public ProblemMarkerSynchronizer() {
		this(IMarker.PROBLEM);
	}

	public ProblemMarkerSynchronizer(String markerBaseType) {
		this.markerBaseType = markerBaseType; 
	}

	/**
	 * Include the given file in the synchronization process. This is not needed when
	 * you call addMarker() for the file; however if there's no addMarker() for that file,
	 * that file will be ignored (existing markers left untouched) unless you register them
	 * with registerFile(). 
	 */
	public void registerFile(IFile file) {
		if (!markerTable.containsKey(file))
			markerTable.put(file, new ArrayList<MarkerData>());
	}
	
	/**
	 * Stores data for a marker to be added to the given file. Implies registerFile().
	 */
	public void addMarker(IFile file, String markerType, Map<String, Object> markerAttrs) {
		registerFile(file);

		MarkerData markerData = new MarkerData();
		markerData.type = markerType;
		markerData.attrs = markerAttrs;
		markerTable.get(file).add(markerData);
	}

	/**
	 * Performs the marker synchronization.
	 */
	public void run() {
		try {
			ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					addRemoveMarkers();
				}
			}, null);
		} catch (CoreException e) {
			CommonPlugin.logError(e);
		}
	}

	protected void addRemoveMarkers() throws CoreException {
		// process each file registered
		for (IFile file : markerTable.keySet()) {
			List<MarkerData> list = markerTable.get(file);

			// add markers that aren't on IFile yet
			for (MarkerData markerData : list)
				if (!fileContainsMarker(file, markerData))
					createMarker(file, markerData);

			// remove IFile markers which aren't in our table
			IMarker[] markers = file.findMarkers(markerBaseType, true, 0);
			for (IMarker marker : markers)
				if (!listContainsMarker(list, marker))
					{marker.delete(); markersRemoved++;}
		}
		
		// debug
		if (markersAdded==0 && markersRemoved==0)
			System.out.println("markerSychronizer: no marker change");
		else
			System.out.println("markerSychronizer: added "+markersAdded+", removed "+markersRemoved+" markers");
	}

	protected boolean fileContainsMarker(IFile file, MarkerData markerData) throws CoreException {
		IMarker[] markers = file.findMarkers(markerData.type, false, 0);
		for (IMarker marker : markers)
			if (markerAttributesAreEqual(marker.getAttributes(), markerData))
				return true;
		return false;
	}

	@SuppressWarnings("unchecked")
	protected boolean listContainsMarker(List<MarkerData> list, IMarker marker) throws CoreException {
		Map markerAttributes = marker.getAttributes();
		for (MarkerData markerData : list)
			if (markerAttributesAreEqual(markerAttributes, markerData))
				return true;
		return false;
	}

	@SuppressWarnings("unchecked")
	protected boolean markerAttributesAreEqual(Map markerAttributes, MarkerData markerData) {
		return mapElementsEqual(markerAttributes, markerData.attrs, IMarker.LINE_NUMBER) &&
		mapElementsEqual(markerAttributes, markerData.attrs, IMarker.SEVERITY) &&
		mapElementsEqual(markerAttributes, markerData.attrs, IMarker.MESSAGE) &&
		mapElementsEqual(markerAttributes, markerData.attrs, IMarker.CHAR_START) &&
		mapElementsEqual(markerAttributes, markerData.attrs, IMarker.CHAR_END) &&
		mapElementsEqual(markerAttributes, markerData.attrs, IMarker.LOCATION);
	}

	protected void createMarker(IFile file, MarkerData markerData) throws CoreException {
		IMarker marker = file.createMarker(markerData.type);
		marker.setAttributes(markerData.attrs);
		markersAdded++;
	}

	@SuppressWarnings("unchecked")
	protected boolean mapElementsEqual(Map map1, Map map2, String key) {
		Object obj1 = map1.get(key);
		Object obj2 = map2.get(key);
		return obj1==null ? obj2==null : obj1.equals(obj2);
	}

}
