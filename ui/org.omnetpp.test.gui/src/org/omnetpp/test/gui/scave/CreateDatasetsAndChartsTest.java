/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.test.gui.scave;

import org.omnetpp.test.gui.access.DatasetsAndChartsPageAccess;

public class CreateDatasetsAndChartsTest extends ScaveFileTestCase {


	DatasetsAndChartsPageAccess datasetsPage;

	@Override
	protected void setUpInternal() throws Exception {
		super.setUpInternal();
		createFiles(2);
		editor = ScaveEditorUtils.openAnalysisFile(projectName, fileName);
		datasetsPage = editor.ensureDatasetsPageActive();
	}

	@Override
	protected void tearDownInternal() throws Exception {
		super.tearDownInternal();
	}

	public void testCreateEmptyDataset() {
		datasetsPage.createDataset("test-dataset");

		datasetsPage.getDatasetsTree().assertContent(
				n("dataset test-dataset"));
	}

	public void testCreateEmptyChartsheet() {
		datasetsPage.createChartsheet("test-chartsheet");

		datasetsPage.getChartsheetsTree().assertContent(
				n("chart sheet test-chartsheet (0 charts)"));
	}

	public void testCreateDatasetWithContent() {
		String dataset = "test-dataset";
		datasetsPage.createDataset(dataset)
					.createAdd(dataset, "vector")
					.createDiscard(dataset, "scalar")
					.createApply(dataset, "mean")
					.createCompute(dataset, null)
					.createBarChart(dataset, "test-barchart")
					.createLineChart(dataset, "test-linechart")
					.createHistogramChart(dataset, "test-histogramchart")
					.createScatterChart(dataset, "test-scatterchart")
					.createChartsheet("test-chartsheet");

		datasetsPage.getDatasetsTree().assertContent(
				n("dataset test-dataset",
					n("add vectors: all"),
					n("discard scalars: all"),
					n("apply mean"),
					n("compute <undefined>"),
					n("bar chart test-barchart"),
					n("line chart test-linechart"),
					n("histogram chart test-histogramchart"),
					n("scatter chart test-scatterchart")));
		datasetsPage.getChartsheetsTree().assertContent(
				forest(
					n("chart sheet default (4 charts)",
						n("bar chart test-barchart"),
						n("line chart test-linechart"),
						n("histogram chart test-histogramchart"),
						n("scatter chart test-scatterchart")),
					n("chart sheet test-chartsheet (0 charts)")));
	}
}
