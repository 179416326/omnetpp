/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.editors.ui;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.omnetpp.common.canvas.LargeScrollableCanvas;
import org.omnetpp.common.canvas.RectangularArea;
import org.omnetpp.common.canvas.ZoomableCachingCanvas;
import org.omnetpp.common.ui.FocusManager;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.scave.actions.ClosePageAction;
import org.omnetpp.scave.charting.ChartCanvas;
import org.omnetpp.scave.charting.ChartFactory;
import org.omnetpp.scave.charting.ChartUpdater;
import org.omnetpp.scave.charting.IChartSelection;
import org.omnetpp.scave.charting.IChartSelectionListener;
import org.omnetpp.scave.charting.VectorChartSelection;
import org.omnetpp.scave.editors.ScaveEditor;
import org.omnetpp.scave.editors.ScaveEditorContributor;
import org.omnetpp.scave.editors.ScaveEditorMemento;
import org.omnetpp.scave.model.Chart;
import org.omnetpp.scave.model.ScaveModelPackage;
import org.omnetpp.scave.model2.ChartDataPoint;
import org.omnetpp.scave.model2.ChartLine;

public class ChartPage extends ScaveEditorPage {

    private Chart chart; // the underlying model
    private ChartCanvas chartView;
    private ChartUpdater updater;

    private IChartSelectionListener chartSelectionListener;
    private IPropertyChangeListener chartPropertyChangeListener;

    public ChartPage(Composite parent, ScaveEditor editor, Chart chart) {
        super(parent, SWT.NONE, editor);
        this.chart = chart;
        initialize();
        this.updater = new ChartUpdater(chart, chartView, scaveEditor.getResultFileManager());
        hookListeners();

        ScaveEditorContributor contributor = ScaveEditorContributor.getDefault();
        if (chart.isTemporary())
            addToToolbar(contributor.getSaveTempChartAction());
        else
            addToToolbar(contributor.getGotoChartDefinitionAction());
        addToToolbar(contributor.getCopyChartToClipboardAction());
        addToToolbar(contributor.getExportToSVGAction());
        addSeparatorToToolbar();
        addToToolbar(contributor.getSwitchChartToPanModeAction());
        addToToolbar(contributor.getSwitchChartToZoomModeAction());
        addSeparatorToToolbar();
        addToToolbar(contributor.getZoomToFitAction());
        addToToolbar(contributor.getHZoomInAction());
        addToToolbar(contributor.getHZoomOutAction());
        addToToolbar(contributor.getVZoomInAction());
        addToToolbar(contributor.getVZoomOutAction());
        addSeparatorToToolbar();
        addToToolbar(contributor.getRefreshChartAction());
        addSeparatorToToolbar();
        addToToolbar(new ClosePageAction());

    }

    @Override
    public void dispose() {
        unhookListeners();
        super.dispose();
    }

    public Chart getChart() {
        return chart;
    }

    public ChartCanvas getChartView() {
        return chartView;
    }

    public ChartUpdater getChartUpdater() {
        return updater;
    }

    public void updateChart() {
        if (updater != null)
            updater.updateDataset();
    }

    /**
     * The only chart on this page is always active.
     */
    @Override
    public ChartCanvas getActiveChartCanvas() {
        return chartView;
    }

    public void updatePage(Notification notification) {
        if (notification.isTouch() || !(notification.getNotifier() instanceof EObject))
            return;

        ScaveModelPackage pkg = ScaveModelPackage.eINSTANCE;
        if (pkg.getAnalysisItem_Name().equals(notification.getFeature())) {
            setPageTitle(getChartName(chart));
            setFormTitle(getChartName(chart));
        }
        updater.updateChart(notification);
    }

    private String getChartName(Chart chart) {
        return StringUtils.defaultIfBlank(chart.getName(), "<unnamed>");
    }

    protected void initialize() {
        // set up UI
        setPageTitle(getChartName(chart));
        setFormTitle(getChartName(chart));
        //setBackground(ColorFactory.asColor("lightGray"));
        getContent().setLayout(new GridLayout(2,false));

        chartView = (ChartCanvas) ChartFactory.createChart(getContent(), this.chart, scaveEditor.getResultFileManager());
        chartView.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

        MenuManager menuManager = new ChartMenuManager(chart, scaveEditor);
        chartView.setMenu(menuManager.createContextMenu(chartView));

        // ensure that focus gets restored correctly after user goes somewhere else and then comes back
        setFocusManager(new FocusManager(this));
    }

    private void hookListeners() {
        if (chartSelectionListener == null) {
            chartSelectionListener = new IChartSelectionListener() {
                public void selectionChanged(IChartSelection selection) {
                    if (selection instanceof VectorChartSelection) {
                        VectorChartSelection chartSelection = (VectorChartSelection)selection;
                        if (chartSelection.getIndex() >= 0) {
                            ChartDataPoint point = new ChartDataPoint(
                                            chart,
                                            chartSelection.getSeries(),
                                            chartSelection.getSeriesKey(),
                                            chartSelection.getID(),
                                            chartSelection.getIndex(),
                                            chartSelection.getEventNum(),
                                            chartSelection.getPreciseX(),
                                            chartSelection.getX(),
                                            chartSelection.getY(),
                                            updater.getResultFileManager());
                            scaveEditor.setSelection(new TreeSelection(new TreePath(new Object[] {chart, point})));
                        }
                        else {
                            ChartLine line = new ChartLine(
                                    chart,
                                    chartSelection.getSeries(),
                                    chartSelection.getSeriesKey(),
                                    chartSelection.getID(),
                                    updater.getResultFileManager());
                            scaveEditor.setSelection(new TreeSelection(new TreePath(new Object[] {chart, line})));
                        }
                    }
                    else {
                        scaveEditor.setSelection(new StructuredSelection(chart));
                    }
                }
            };
            chartView.addChartSelectionListener(chartSelectionListener);
        }
        if (chartPropertyChangeListener == null) {
            chartPropertyChangeListener = new IPropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent event) {
                    if (event.getProperty() == LargeScrollableCanvas.PROP_VIEW_X ||
                            event.getProperty() == LargeScrollableCanvas.PROP_VIEW_Y ||
                            event.getProperty() == ZoomableCachingCanvas.PROP_ZOOM_X ||
                            event.getProperty() == ZoomableCachingCanvas.PROP_ZOOM_Y) {
                        scaveEditor.markNavigationLocation();
                    }
                }
            };
            chartView.addPropertyChangeListener(chartPropertyChangeListener);
        }
    }



    @Override
    public boolean setFocus() {
        if (chartView != null)
            return chartView.setFocus();
        else
            return super.setFocus();
    }

    @Override
    public void pageActivated() {
        if (chartSelectionListener != null)
            chartSelectionListener.selectionChanged(chartView.getSelection());
    }

    private void unhookListeners() {
        if (chartSelectionListener != null) {
            chartView.removeChartSelectionListener(chartSelectionListener);
            chartSelectionListener = null;
        }
        if (chartPropertyChangeListener != null) {
            chartView.removePropertyChangeListener(chartPropertyChangeListener);
            chartPropertyChangeListener = null;
        }
    }

    @Override
    public boolean gotoObject(Object object) {
        if (object instanceof EObject) {
            EObject eobject = (EObject)object;
            if (EcoreUtil.isAncestor(chart, eobject)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void saveState(IMemento memento) {
        memento.putString(ScaveEditorMemento.ZOOM, chartView.getZoomedArea().toString());
    }

    @Override
    public void restoreState(IMemento memento) {
        String areaStr = memento.getString("Zoom");
        RectangularArea area = areaStr != null ? RectangularArea.fromString(areaStr) : null;
        if (area != null)
            chartView.setZoomedArea(area);
    }
}
