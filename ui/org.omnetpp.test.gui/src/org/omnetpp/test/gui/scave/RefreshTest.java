/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.
  
  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.test.gui.scave;

import org.eclipse.swt.SWT;
import org.omnetpp.common.ui.GenericTreeNode;
import org.omnetpp.test.gui.access.BrowseDataPageAccess;
import org.omnetpp.test.gui.access.DatasetViewAccess;
import org.omnetpp.test.gui.access.InputsPageAccess;

import com.simulcraft.test.gui.util.WorkbenchUtils;

public class RefreshTest extends ScaveFileTestCase {
	
	DatasetViewAccess datasetView;
	
	@Override
	protected void setUpInternal() throws Exception {
		super.setUpInternal();
		createFiles();
		editor = ScaveEditorUtils.openAnalysisFile(projectName, fileName);
		datasetView = ScaveEditorUtils.ensureDatasetView();
	}
	
	public void testInitialContent() {
		assertFileRunViewContent(buildFileRunViewContent(1, 2));
		assertRunFileViewContent(buildRunFileViewContent(1, 2));
		assertLogicalViewContent(buildLogicalViewContent(1, 2));
		assertBrowseDataPageScalarsTableContent(buildScalarsTableContent(1, 2));
		assertBrowseDataPageVectorsTableContent(buildVectorsTableContent(1, 2));
		assertDatasetViewScalarsTableContent(buildScalarsTableContent(1, 2));
		assertDatasetViewVectorsTableContent(buildVectorsTableContent(1, 2));
	}
	
    public void testRemoveFileFromWorkspace() throws Exception {
    	removeFile("test-2.sca");
    	removeFile("test-2.vec");
    	WorkbenchUtils.refreshProjectFromProjectExplorerView(projectName);

    	assertFileRunViewContent(buildFileRunViewContent(1));
    	assertRunFileViewContent(buildRunFileViewContent(1));
    	assertLogicalViewContent(buildLogicalViewContent(1));
		assertBrowseDataPageScalarsTableContent(buildScalarsTableContent(1));
		assertBrowseDataPageVectorsTableContent(buildVectorsTableContent(1));
		assertDatasetViewScalarsTableContent(buildScalarsTableContent(1));
		assertDatasetViewVectorsTableContent(buildVectorsTableContent(1));
    }
    
    public void testAddFileToWorkspace() throws Exception {
    	createFile("test-3.sca", createScalarFileContent(3));
    	createFile("test-3.vec", createVectorFileContent(3));
    	WorkbenchUtils.refreshProjectFromProjectExplorerView(projectName);
    	
    	assertFileRunViewContent(buildFileRunViewContent(1, 2, 3));
    	assertRunFileViewContent(buildRunFileViewContent(1, 2, 3));
    	assertLogicalViewContent(buildLogicalViewContent(1, 2, 3));
		assertBrowseDataPageScalarsTableContent(buildScalarsTableContent(1, 2, 3));
		assertBrowseDataPageVectorsTableContent(buildVectorsTableContent(1, 2, 3));
		assertDatasetViewScalarsTableContent(buildScalarsTableContent(1, 2, 3));
		assertDatasetViewVectorsTableContent(buildVectorsTableContent(1, 2, 3));
    }
    
    public void testRemoveFileFromInputs() {
    	InputsPageAccess inputsPage = editor.ensureInputsPageActive();
        inputsPage.removeInputFile("/project/test-2\\.vec");
        inputsPage.removeInputFile("/project/test-2\\.sca");
        
    	assertFileRunViewContent(buildFileRunViewContent(1));
    	assertRunFileViewContent(buildRunFileViewContent(1));
    	assertLogicalViewContent(buildLogicalViewContent(1));
		assertBrowseDataPageScalarsTableContent(buildScalarsTableContent(1));
		assertBrowseDataPageVectorsTableContent(buildVectorsTableContent(1));
		assertDatasetViewScalarsTableContent(buildScalarsTableContent(1));
		assertDatasetViewVectorsTableContent(buildVectorsTableContent(1));
    }

    public void testAddFileToInputs() {
    	InputsPageAccess inputsPage = editor.ensureInputsPageActive();
    	inputsPage.addFileWithWildcard("/project/test-4.vec");
    	inputsPage.addFileWithWildcard("/project/test-4.sca");

    	assertFileRunViewContent(buildFileRunViewContent(1, 2, 4));
    	assertRunFileViewContent(buildRunFileViewContent(1, 2, 4));
    	assertLogicalViewContent(buildLogicalViewContent(1, 2, 4));
		assertBrowseDataPageScalarsTableContent(buildScalarsTableContent(1, 2, 4));
		assertBrowseDataPageVectorsTableContent(buildVectorsTableContent(1, 2, 4));
		assertDatasetViewScalarsTableContent(buildScalarsTableContent(1, 2, 4));
		assertDatasetViewVectorsTableContent(buildVectorsTableContent(1, 2, 4));
    }
    
    
	@Override
	protected String createAnalysisFileContent() {
		return 
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			"<scave:Analysis xmi:version=\"2.0\" xmlns:xmi=\"http://www.omg.org/XMI\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:scave=\"http://www.omnetpp.org/omnetpp/scave\">\n" +
			"  <inputs>\n" +
			"    <inputs name=\"/project/test-1.vec\"/>\n" +
			"    <inputs name=\"/project/test-1.sca\"/>\n" +
			"    <inputs name=\"/project/test-2.vec\"/>\n" +
			"    <inputs name=\"/project/test-2.sca\"/>\n" +
			"    <inputs name=\"/project/test-3.vec\"/>\n" +
			"    <inputs name=\"/project/test-3.sca\"/>\n" +
			"  </inputs>\n" +
			"  <datasets>\n" +
			"    <datasets name=\"test-dataset\">\n" +
			"      <items xsi:type=\"scave:Add\" filterPattern=\"\" type=\"VECTOR\"/>\n" +
			"      <items xsi:type=\"scave:Add\" filterPattern=\"\" type=\"SCALAR\"/>\n" +
			"      <items xsi:type=\"scave:LineChart\" name=\"test-linechart\" lineNameFormat=\"\"/>\n" +
			"      <items xsi:type=\"scave:BarChart\" name=\"test-barchart\"/>\n" +
			"      <items xsi:type=\"scave:HistogramChart\" name=\"test-histogramchart\"/>\n" +
			"      <items xsi:type=\"scave:ScatterChart\" name=\"test-scatterchart\" xDataPattern=\"module(module-1) AND name(&quot;mean(vector-1)&quot;)\"/>\n" +
			"    </datasets>\n" +
			"  </datasets>\n" +
			"  <chartSheets>\n" +
			"    <chartSheets name=\"default\" charts=\"//@datasets/@datasets.0/@items.3 //@datasets/@datasets.0/@items.2 //@datasets/@datasets.0/@items.4 //@datasets/@datasets.0/@items.5\"/>\n" +
			"    <chartSheets name=\"test-chartsheet\"/>\n" +
			"  </chartSheets>\n" +
			"</scave:Analysis>\n";
	}

	protected void createFiles() throws Exception {
		createAnalysisFile();
		
		for (int runNumber = 1; runNumber <= 4; ++runNumber) {
			if (runNumber == 3)
				continue;
			createScalarFile(runNumber);
			createVectorFile(runNumber);
		}
	}
	
	protected GenericTreeNode[] buildFileRunViewContent(int... runNumbers) {
		GenericTreeNode[] content = new GenericTreeNode[runNumbers.length * 2];
		for (int i = 0; i < runNumbers.length; i++) {
			content[2*i] = n(String.format("/project/test-%d.sca", runNumbers[i]),
								n(String.format("run \"run-%d\"", runNumbers[i])));
			content[2*i+1] = n(String.format("/project/test-%d.vec", runNumbers[i]),
								n(String.format("run \"run-%d\"", runNumbers[i])));
		}
		return content;
	}
	
	protected GenericTreeNode[] buildRunFileViewContent(int... runNumbers) {
		GenericTreeNode[] content = new GenericTreeNode[runNumbers.length];
		for (int i = 0; i < runNumbers.length; i++) {
			content[i] = n(String.format("run \"run-%d\"", runNumbers[i]),
								n(String.format("/project/test-%d.sca", runNumbers[i])),
								n(String.format("/project/test-%d.vec", runNumbers[i])));
		}
		return content;
	}
	
	protected GenericTreeNode[] buildLogicalViewContent(int... runNumbers) {
		GenericTreeNode[] content = new GenericTreeNode[runNumbers.length];
		for (int i = 0; i < runNumbers.length; i++) {
			content[i] = n(String.format("experiment \"%d\"", runNumbers[i]),
								n(String.format("measurement \"%d\"", runNumbers[i]),
									n(String.format("replication \"%1$d\" (seedset=#%1$d)", runNumbers[i]),
										n(String.format("run \"run-%d\"", runNumbers[i]),
											n(String.format("/project/test-%d.sca", runNumbers[i])),
											n(String.format("/project/test-%d.vec", runNumbers[i]))))));
		}
		return content;
	}
	
	protected String[][] buildScalarsTableContent(int... runNumbers) {
		String[][] table = new String[runNumbers.length][];
		for (int i = 0; i < runNumbers.length; ++i) {
			table[i] = buildScalarsTableRow(runNumbers[i]);
		}
		return table;
	}
	
	protected String[][] buildVectorsTableContent(int... runNumbers) {
		String[][] table = new String[runNumbers.length][];
		for (int i = 0; i < runNumbers.length; ++i) {
			table[i] = buildVectorsTableRow(runNumbers[i]);
		}
		return table;
	}
	
	protected GenericTreeNode buildDatasetsTreeContent() {
		return	n("dataset test-dataset",
					n("add vectors: all"),
					n("add scalars: all"),
					n("line chart test-linechart"),
					n("bar chart test-barchart"),
					n("histogram chart test-histogramchart"),
					n("scatter chart test-scatterchart"));
	}
	
	protected void assertFileRunViewContent(GenericTreeNode... content) {
		InputsPageAccess inputsPage = editor.ensureInputsPageActive();
		inputsPage.ensureFileRunViewVisible();
		inputsPage.getFileRunViewTree().assertContent(content);
	}
	
	protected void assertRunFileViewContent(GenericTreeNode... content) {
		InputsPageAccess inputsPage = editor.ensureInputsPageActive();
		inputsPage.ensureRunFileViewVisible();
		inputsPage.getRunFileViewTree().assertContent(content);
	}
	
	protected void assertLogicalViewContent(GenericTreeNode... content) {
		InputsPageAccess inputsPage = editor.ensureInputsPageActive();
		inputsPage.ensureLogicalViewVisible();
		inputsPage.getLogicalViewTree().assertContent(content);
	}
	
	protected void assertBrowseDataPageScalarsTableContent(String[]... content) {
		BrowseDataPageAccess browseDataPage = editor.ensureBrowseDataPageActive();
		browseDataPage.ensureScalarsSelected();
		browseDataPage.sortByTableColumn(BrowseDataPageAccess.FILE_NAME, SWT.UP);
		browseDataPage.getScalarsTable().assertContent(content);
	}
	
	protected void assertBrowseDataPageVectorsTableContent(String[]... content) {
		BrowseDataPageAccess browseDataPage = editor.ensureBrowseDataPageActive();
		browseDataPage.ensureVectorsSelected();
		browseDataPage.sortByTableColumn(BrowseDataPageAccess.FILE_NAME, SWT.UP);
		browseDataPage.getVectorsTable().assertContent(content);
	}

	protected void assertDatasetViewScalarsTableContent(String[]... content) {
		editor.ensureDatasetsPageActive().getDatasetsTree().findTreeItemByContent("dataset.*test-dataset.*").click();
		datasetView.activateWithMouseClick();
		datasetView.ensureScalarsPanelActivated();
		datasetView.sortByTableColumn(DatasetViewAccess.FILE_NAME, SWT.UP);
		datasetView.getScalarsTable().assertContent(content);
	}
	
	protected void assertDatasetViewVectorsTableContent(String[]... content) {
		editor.ensureDatasetsPageActive().getDatasetsTree().findTreeItemByContent("dataset.*test-dataset.*").click();
		datasetView.activateWithMouseClick();
		datasetView.ensureVectorsPanelActivated();
		datasetView.sortByTableColumn(DatasetViewAccess.FILE_NAME, SWT.UP);
		datasetView.getVectorsTable().assertContent(content);
	}
}
