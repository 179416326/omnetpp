package org.omnetpp.scave.panel;

import org.eclipse.jface.viewers.TableViewer;

import org.omnetpp.scave.engine.IDList;
import org.omnetpp.scave.engine.ResultFileManager;
import org.omnetpp.scave.plugin.ScavePlugin;

public class ScalarTableContentSorter extends TableContentSorter implements ScalarPanelConstants {

	public ScalarTableContentSorter() {
		super();
	}

	public void sort(TableViewer tableViewer, int column, boolean ascending) {
		IDList idList = (IDList) tableViewer.getInput();
		ResultFileManager resultFileManager = ScavePlugin.getDefault().resultFileManager;
		switch (column) {
		case COL_DIRECTORY: idList.sortByDirectory(resultFileManager, ascending); break;
		case COL_FILENAME: idList.sortByFileName(resultFileManager, ascending); break;
		case COL_RUN: idList.sortByRun(resultFileManager, ascending); break;
		case COL_MODULE: idList.sortByModule(resultFileManager, ascending); break;
		case COL_NAME: idList.sortByName(resultFileManager, ascending); break;
		case COL_VALUE: idList.sortScalarsByValue(resultFileManager, ascending); break;
		}
	}

	public void reverse(TableViewer tableViewer, int column) {
		IDList idList = (IDList) tableViewer.getInput();
		idList.reverse();
	}
}
