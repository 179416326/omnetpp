package org.omnetpp.scave.editors.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.omnetpp.common.color.ColorFactory;
import org.omnetpp.scave.charting.ChartCanvas;
import org.omnetpp.scave.charting.ChartFactory;
import org.omnetpp.scave.charting.ChartProperties;
import org.omnetpp.scave.charting.ChartUpdater;
import org.omnetpp.scave.editors.ScaveEditor;
import org.omnetpp.scave.model.Chart;
import org.omnetpp.scave.model.ChartSheet;
import org.omnetpp.scave.model.ScaveModelPackage;

public class ChartSheetPage extends ScaveEditorPage {

	private ChartSheet chartsheet; // the underlying model

	private LiveTable chartsArea;
	private List<ChartUpdater> updaters = new ArrayList<ChartUpdater>();
	
	public ChartSheetPage(Composite parent, ScaveEditor editor, ChartSheet chartsheet) {
		super(parent, SWT.V_SCROLL | SWT.H_SCROLL, editor);
		this.chartsheet = chartsheet;
		initialize();
	}
	
	public void updatePage(Notification notification) {
		if (ScaveModelPackage.eINSTANCE.getChartSheet_Name().equals(notification.getFeature())) {
			setPageTitle("Charts: " + getChartSheetName(chartsheet));
			setFormTitle("Charts: " + getChartSheetName(chartsheet));
		}
		for (ChartUpdater updater : updaters)
			updater.updateChart(notification);
	}
	
	public Composite getChartSheetComposite() {
		//return getBody();
		return chartsArea;
	}
	
	private void addChart(Chart chart, ChartCanvas view) {
		view.setLayoutData(new GridData(320,200));
		chartsArea.configureChild(view);
		updaters.add(new ChartUpdater(chart, view, scaveEditor.getResultFileManager()));
	}
	
	@SuppressWarnings("unchecked")
	private void initialize() {
		// set up UI
		setPageTitle("Charts: " + getChartSheetName(chartsheet));
		setFormTitle("Charts: " + getChartSheetName(chartsheet));
		setBackground(ColorFactory.WHITE);
		//setExpandHorizontal(true);
		//setExpandVertical(true);
		GridLayout layout = new GridLayout();
		getBody().setLayout(layout);
		
		Button refreshButton = new Button(getBody(), SWT.NONE);
		refreshButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		refreshButton.setText("Refresh");
		refreshButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				for (ChartUpdater updater : updaters)
					updater.updateDataset();
			}
		});
		
		chartsArea = new LiveTable(getBody(), SWT.DOUBLE_BUFFERED);
		chartsArea.setLayoutData(new GridData(SWT.FILL,SWT.FILL, true, true));
		chartsArea.setBackground(ColorFactory.WHITE);

		GridLayout gridLayout = new GridLayout(2, true); //2 columns
		gridLayout.horizontalSpacing = 7;
		gridLayout.verticalSpacing = 7;
		chartsArea.setLayout(gridLayout);
		
		chartsArea.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				scaveEditor.setSelection(event.getSelection());
			}
		});
		
		// set up contents
		Collection<Chart> charts = chartsheet.getCharts();
		Composite parent = getChartSheetComposite();
		for (final Chart chart : charts) {
			ChartCanvas chartControl = ChartFactory.createChart(parent, chart, scaveEditor.getResultFileManager());
			if (chartControl != null) {
				addChart(chart, chartControl);
				configureChartView(chartControl, chart);
			}
		}
		
		getContent().setSize(getContent().computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}
	
	@Override
	public void configureChartView(ChartCanvas view, final Chart chart) {
		view.setProperty(ChartProperties.PROP_DISPLAY_LEGEND, "false");
		
		view.addMouseListener(new MouseAdapter() { //FIXME this is a hack to get chart opened by double-click; to be done properly (SelectionListener, ask chart from widget)
			public void mouseDoubleClick(MouseEvent e) {
				scaveEditor.openChart(chart);
			}
		});

		super.configureChartView(view, chart);
	}

	private static String getChartSheetName(ChartSheet chartSheet) {
		return chartSheet.getName() != null ? chartSheet.getName() : "<unnamed>";
	}

	@Override
	public boolean gotoObject(Object object) {
		if (object == chartsheet) {
			return true;
		}
		if (object instanceof EObject) {
			EObject eobject = (EObject)object;
			for (Chart chart : chartsheet.getCharts())
				if (chart == eobject) {
					// TODO scroll to chart
					return true;
				}
		}
		return false;
	}
}
