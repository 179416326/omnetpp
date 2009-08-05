/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.
  
  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.engineext;

import org.eclipse.core.runtime.ListenerList;
import org.omnetpp.scave.engine.FileRun;
import org.omnetpp.scave.engine.FileRunList;
import org.omnetpp.scave.engine.HistogramResult;
import org.omnetpp.scave.engine.IDList;
import org.omnetpp.scave.engine.ResultFile;
import org.omnetpp.scave.engine.ResultFileList;
import org.omnetpp.scave.engine.ResultFileManager;
import org.omnetpp.scave.engine.ResultItem;
import org.omnetpp.scave.engine.Run;
import org.omnetpp.scave.engine.RunList;
import org.omnetpp.scave.engine.ScalarResult;
import org.omnetpp.scave.engine.StringMap;
import org.omnetpp.scave.engine.StringSet;
import org.omnetpp.scave.engine.StringVector;
import org.omnetpp.scave.engine.VectorResult;

/**
 * ResultFileManager with notification capability. Also re-wraps
 * all returned IDLists into IDListEx (IDList with notification 
 * capability.)
 * 
 * @author andras
 */
public class ResultFileManagerEx extends ResultFileManager {

	private ListenerList changeListeners = new ListenerList();
	private ListenerList disposeListeners = new ListenerList();
	
	private void checkDeleted() {
		if (getCPtr(this) == 0)
			throw new IllegalStateException("Tried to access a deleted ResultFileManagerEx.");
	}
	
	public boolean isDisposed() {
		return getCPtr(this) == 0;
	}
	
	public void dispose() {
		delete();
		notifyDisposeListeners();
	}
	
	public void addChangeListener(IResultFilesChangeListener listener) {
		changeListeners.add(listener);
	}

	public void removeChangeListener(IResultFilesChangeListener listener) {
		changeListeners.remove(listener);
	}
	
	protected void notifyChangeListeners() {
		for (Object listener : changeListeners.getListeners())
			((IResultFilesChangeListener)listener).resultFileManagerChanged(this);
	}
	
	public void addDisposeListener(IResultFileManagerDisposeListener listener) {
		disposeListeners.add(listener);
	}
	
	public void removeDisposeListener(IResultFileManagerDisposeListener listener) {
		disposeListeners.remove(listener);
	}
	
	protected void notifyDisposeListeners() {
		for (Object listener : disposeListeners.getListeners())
			((IResultFileManagerDisposeListener)listener).resultFileManagerDisposed(this);
	}

	@Override
	public ResultFile loadFile(String filename) {
		checkDeleted();
		ResultFile file = super.loadFile(filename);
		notifyChangeListeners();
		return file;
	}

	@Override
	public ResultFile loadFile(String filename, String osFileName) {
		checkDeleted();
		ResultFile file = super.loadFile(filename, osFileName);
		notifyChangeListeners();
		return file;
	}
	
	@Override
	public void unloadFile(ResultFile file) {
		checkDeleted();
		super.unloadFile(file);
		notifyChangeListeners();
	}

	private IDListEx wrap(IDList obj) {
		return new IDListEx(obj); // re-wrap C++ object to "Ex" class 
	}
	
	@Override
	public IDListEx getAllScalars() {
		checkDeleted();
		return wrap(super.getAllScalars());
	}

	@Override
	public IDListEx getAllVectors() {
		checkDeleted();
		return wrap(super.getAllVectors());
	}
	
	@Override
	public IDList getAllItems() {
		checkDeleted();
		return wrap(super.getAllItems());
	}

	@Override
	public IDList getScalarsInFileRun(FileRun fileRun) {
		checkDeleted();
		return wrap(super.getScalarsInFileRun(fileRun));
	}

	@Override
	public IDList getVectorsInFileRun(FileRun fileRun) {
		checkDeleted();
		return wrap(super.getVectorsInFileRun(fileRun));
	}

	@Override
	public IDListEx filterIDList(IDList idlist, FileRunList fileAndRunFilter, String moduleFilter, String nameFilter) {
		checkDeleted();
		return wrap(super.filterIDList(idlist, fileAndRunFilter, moduleFilter, nameFilter));
	}
	
	/*
	 * The rest adds check() calls only.
	 */

	@Override
	public ResultItem _getItem(long id) {
		checkDeleted();
		return super._getItem(id);
	}

	@Override
	public ResultFileList filterFileList(ResultFileList fileList,
			String filePathPattern) {
		checkDeleted();
		return super.filterFileList(fileList, filePathPattern);
	}

	@Override
	public IDList filterIDList(IDList idlist, String pattern) {
		checkDeleted();
		return super.filterIDList(idlist, pattern);
	}

	@Override
	public RunList filterRunList(RunList runList, String runNameFilter,
			StringMap attrFilter) {
		checkDeleted();
		return super.filterRunList(runList, runNameFilter, attrFilter);
	}

	@Override
	public IDList getAllHistograms() {
		checkDeleted();
		return super.getAllHistograms();
	}

	@Override
	public ResultFile getFile(String fileName) {
		checkDeleted();
		return super.getFile(fileName);
	}

	@Override
	public StringVector getFileAndRunNumberFilterHints(IDList idlist) {
		checkDeleted();
		return super.getFileAndRunNumberFilterHints(idlist);
	}

	@Override
	public FileRun getFileRun(ResultFile file, Run run) {
		checkDeleted();
		return super.getFileRun(file, run);
	}

	@Override
	public FileRunList getFileRuns(ResultFileList fileList, RunList runList) {
		checkDeleted();
		return super.getFileRuns(fileList, runList);
	}

	@Override
	public ResultFileList getFiles() {
		checkDeleted();
		return super.getFiles();
	}

	@Override
	public ResultFileList getFilesForRun(Run run) {
		checkDeleted();
		return super.getFilesForRun(run);
	}

	@Override
	public HistogramResult getHistogram(long id) {
		checkDeleted();
		return super.getHistogram(id);
	}

	@Override
	public IDList getHistogramsInFileRun(FileRun fileRun) {
		checkDeleted();
		return super.getHistogramsInFileRun(fileRun);
	}

	@Override
	public ResultItem getItem(long id) {
		checkDeleted();
		return super.getItem(id);
	}

	@Override
	public long getItemByName(FileRun fileRun, String module, String name) {
		checkDeleted();
		return super.getItemByName(fileRun, module, name);
	}

	@Override
	public StringVector getModuleFilterHints(IDList idlist) {
		checkDeleted();
		return super.getModuleFilterHints(idlist);
	}
	
	

	@Override
	public StringVector getFilePathFilterHints(ResultFileList fileList) {
		checkDeleted();
		return super.getFilePathFilterHints(fileList);
	}

	@Override
	public StringVector getRunNameFilterHints(RunList runList) {
		checkDeleted();
		return super.getRunNameFilterHints(runList);
	}

	@Override
	public StringVector getNameFilterHints(IDList idlist) {
		checkDeleted();
		return super.getNameFilterHints(idlist);
	}
	
	@Override
	public StringVector getModuleParamFilterHints(RunList runList,
			String paramName) {
		checkDeleted();
		return super.getModuleParamFilterHints(runList, paramName);
	}

	@Override
	public StringVector getResultItemAttributeFilterHints(IDList idlist,
			String attrName) {
		checkDeleted();
		return super.getResultItemAttributeFilterHints(idlist, attrName);
	}

	@Override
	public StringVector getRunAttributeFilterHints(RunList runList,
			String attrName) {
		checkDeleted();
		return super.getRunAttributeFilterHints(runList, attrName);
	}

	@Override
	public Run getRunByName(String runName) {
		checkDeleted();
		return super.getRunByName(runName);
	}

	@Override
	public RunList getRuns() {
		checkDeleted();
		return super.getRuns();
	}

	@Override
	public RunList getRunsInFile(ResultFile file) {
		checkDeleted();
		return super.getRunsInFile(file);
	}

	@Override
	public ScalarResult getScalar(long id) {
		checkDeleted();
		return super.getScalar(id);
	}

	@Override
	public StringSet getUniqueRunAttributeValues(RunList runList,
			String attrName) {
		checkDeleted();
		return super.getUniqueRunAttributeValues(runList, attrName);
	}

	@Override
	public FileRunList getUniqueFileRuns(IDList ids) {
		checkDeleted();
		return super.getUniqueFileRuns(ids);
	}

	@Override
	public ResultFileList getUniqueFiles(IDList ids) {
		checkDeleted();
		return super.getUniqueFiles(ids);
	}

	@Override
	public StringSet getUniqueModuleNames(IDList ids) {
		checkDeleted();
		return super.getUniqueModuleNames(ids);
	}

	@Override
	public StringSet getUniqueNames(IDList ids) {
		checkDeleted();
		return super.getUniqueNames(ids);
	}

	@Override
	public RunList getUniqueRuns(IDList ids) {
		checkDeleted();
		return super.getUniqueRuns(ids);
	}
	
	@Override
	public StringSet getUniqueAttributeNames(IDList ids) {
		checkDeleted();
		return super.getUniqueAttributeNames(ids);
	}

	@Override
	public StringSet getUniqueAttributeValues(IDList ids, String attrName) {
		checkDeleted();
		return super.getUniqueAttributeValues(ids, attrName);
	}

	@Override
	public StringSet getUniqueModuleParamNames(RunList runList) {
		checkDeleted();
		return super.getUniqueModuleParamNames(runList);
	}

	@Override
	public StringSet getUniqueModuleParamValues(RunList runList,
			String paramName) {
		checkDeleted();
		return super.getUniqueModuleParamValues(runList, paramName);
	}

	@Override
	public StringSet getUniqueRunAttributeNames(RunList runList) {
		checkDeleted();
		return super.getUniqueRunAttributeNames(runList);
	}

	@Override
	public VectorResult getVector(long id) {
		checkDeleted();
		return super.getVector(id);
	}

	@Override
	public boolean isFileLoaded(String fileName) {
		checkDeleted();
		return super.isFileLoaded(fileName);
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		checkDeleted();
		return super.clone();
	}

	@Override
	public boolean equals(Object obj) {
		checkDeleted();
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		checkDeleted();
		return super.hashCode();
	}

	@Override
	public String toString() {
		checkDeleted();
		return super.toString();
	}

	@Override
	public long addComputedVector(int vectorId, String name, String file, StringMap attributes, long computationID, long input, Object processingOp) {
		checkDeleted();
		long id = super.addComputedVector(vectorId, name, file, attributes, computationID, input, processingOp);
		notifyChangeListeners();
		return id;
	}

	@Override
	public long getComputedID(long computationID, long inputID) {
		checkDeleted();
		return super.getComputedID(computationID, inputID);
	}

	@Override
	public boolean hasStaleID(IDList ids) {
		checkDeleted();
		return super.hasStaleID(ids);
	}

	@Override
	public boolean isStaleID(long id) {
		checkDeleted();
		return super.isStaleID(id);
	}
}
