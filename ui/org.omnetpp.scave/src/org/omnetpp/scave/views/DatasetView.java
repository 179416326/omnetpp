/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.views;

import static org.omnetpp.scave.TestSupport.DATASET_VIEW_ALL_PANEL_ID;
import static org.omnetpp.scave.TestSupport.DATASET_VIEW_HISTOGRAMS_PANEL_ID;
import static org.omnetpp.scave.TestSupport.DATASET_VIEW_SCALARS_PANEL_ID;
import static org.omnetpp.scave.TestSupport.DATASET_VIEW_VECTORS_PANEL_ID;
import static org.omnetpp.scave.TestSupport.WIDGET_ID;
import static org.omnetpp.scave.TestSupport.enableGuiTest;

import java.util.concurrent.Callable;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.provider.IChangeNotifier;
import org.eclipse.emf.edit.provider.INotifyChangedListener;
import org.eclipse.emf.edit.ui.provider.AdapterFactoryLabelProvider;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.omnetpp.common.image.ImageFactory;
import org.omnetpp.common.ui.ViewWithMessagePart;
import org.omnetpp.scave.actions.CreateTempChartAction;
import org.omnetpp.scave.actions.SetFilterAction2;
import org.omnetpp.scave.editors.IDListSelection;
import org.omnetpp.scave.editors.ScaveEditor;
import org.omnetpp.scave.editors.datatable.ChooseTableColumnsAction;
import org.omnetpp.scave.editors.datatable.DataTable;
import org.omnetpp.scave.editors.datatable.FilteredDataPanel;
import org.omnetpp.scave.editors.datatable.FilteredDataTabFolder;
import org.omnetpp.scave.editors.datatable.IDataControl;
import org.omnetpp.scave.editors.treeproviders.ScaveModelLabelProvider;
import org.omnetpp.scave.engine.IDList;
import org.omnetpp.scave.engine.ResultFileManager;
import org.omnetpp.scave.engineext.IResultFilesChangeListener;
import org.omnetpp.scave.model.AddDiscardOp;
import org.omnetpp.scave.model.BarChart;
import org.omnetpp.scave.model.Dataset;
import org.omnetpp.scave.model.DatasetItem;
import org.omnetpp.scave.model.HistogramChart;
import org.omnetpp.scave.model.LineChart;
import org.omnetpp.scave.model.ResultType;
import org.omnetpp.scave.model.ScatterChart;
import org.omnetpp.scave.model2.ChartLine;
import org.omnetpp.scave.model2.DatasetManager;
import org.omnetpp.scave.model2.ResultItemRef;
import org.omnetpp.scave.model2.ScaveModelUtil;

/**
 * View that shows the content of a dataset.
 *
 * @author tomi
 */
// work in progress ...
// TODO add icon
public class DatasetView extends ViewWithMessagePart implements ISelectionProvider	{

	public static final String ID = "org.omnetpp.scave.DatasetView";

	private FilteredDataTabFolder tabFolder;

	private boolean viewControlCreated;

	private ScaveEditor activeScaveEditor;
	private Dataset selectedDataset;
	private DatasetItem selectedItem;

	private IAction selectAllAction;
	private IAction toggleFilterAction;
	private SetFilterAction2 setFilterAction;

	private ISelectionListener selectionChangedListener;
	private IPartListener partListener;
	private IResultFilesChangeListener resultFilesChangeListener;
	private INotifyChangedListener modelChangeListener;

	private ILabelProvider labelProvider;

	private ISelection selection;
	private ListenerList selectionChangeListeners = new ListenerList();

	private Runnable scheduledUpdate;

	@Override
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		site.setSelectionProvider(this);
	}


	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		initActions();
		hookPageListeners();
		createToolbarButtons();
        configurePanel(tabFolder.getAllPanel());
		configurePanel(tabFolder.getVectorsPanel());
        configurePanel(tabFolder.getScalarsPanel());
		configurePanel(tabFolder.getHistogramsPanel());
        createContextMenu(tabFolder.getAllPanel());
		createContextMenu(tabFolder.getVectorsPanel());
		createContextMenu(tabFolder.getScalarsPanel());
		createContextMenu(tabFolder.getHistogramsPanel());
	}


	@Override
	protected Control createViewControl(Composite parent) {
		tabFolder = new FilteredDataTabFolder(parent, SWT.NONE);
        tabFolder.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH));
		setVisibleDataPanel(tabFolder.getVectorsPanel());
		showFilter(false);

		viewControlCreated = true;

		if (enableGuiTest) {
            tabFolder.getAllPanel().setData(WIDGET_ID, DATASET_VIEW_ALL_PANEL_ID);
			tabFolder.getScalarsPanel().setData(WIDGET_ID, DATASET_VIEW_SCALARS_PANEL_ID);
			tabFolder.getVectorsPanel().setData(WIDGET_ID, DATASET_VIEW_VECTORS_PANEL_ID);
			tabFolder.getHistogramsPanel().setData(WIDGET_ID, DATASET_VIEW_HISTOGRAMS_PANEL_ID);
		}

		return tabFolder;
	}

	private FilteredDataPanel configurePanel(FilteredDataPanel panel) {
		IDataControl control = panel.getDataControl();
        control.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IDataControl control = (IDataControl)e.widget;
				if (control.getSelectionCount() == 0)
					setSelection(StructuredSelection.EMPTY);
				else {
					IDListSelection selection = new IDListSelection(control.getSelectedIDs(), control.getResultFileManager());
					setSelection(selection);
				}
			}
		});
		control.addMouseListener(new MouseListener() {
            public void mouseDoubleClick(MouseEvent e) {
                if (e.getSource() instanceof IDataControl)
                    new CreateTempChartAction().run();
            }

            public void mouseDown(MouseEvent e) {
            }

            public void mouseUp(MouseEvent e) {
            }
		});
		return panel;
	}

	private void initActions() {
		toggleFilterAction = new ToggleFilterAction();
		toggleFilterAction.setChecked(isFilterVisible());
		setFilterAction = new SetFilterAction2();
		selectAllAction = new SelectAllAction();
		IActionBars actionBars = getViewSite().getActionBars();
		actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), selectAllAction);
	}

	private void createToolbarButtons() {
		IToolBarManager manager = getViewSite().getActionBars().getToolBarManager();
		manager.add(toggleFilterAction);
		manager.add(new Separator());
	}

	private void createContextMenu(final FilteredDataPanel panel) {
		IDataControl control = panel.getDataControl();
		IMenuManager menuManager = control.getContextMenuManager();
		if (control instanceof DataTable)
		    menuManager.add(new ChooseTableColumnsAction((DataTable)control));
		menuManager.add(setFilterAction);
		control.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ResultFileManager.callWithReadLock(panel.getResultFileManager(), new Callable<Object>() {
					public Object call() throws Exception {
						setFilterAction.update(panel);
						return null;
					}
				});
			}
		});
		// XXX call getSite().registerContexMenu() ?
	}

	@Override
	public void dispose() {
		unhookPageListeners();
		super.dispose();
	}

	private void hookPageListeners() {
		selectionChangedListener = new ISelectionListener() {
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				workbechSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(selectionChangedListener);

		partListener = new IPartListener() {
			public void partActivated(IWorkbenchPart part) {
				if (part instanceof ScaveEditor)
					activeEditorChanged((ScaveEditor)part);
				else if (part instanceof IEditorPart)
					activeEditorChanged(null);
			}

			public void partClosed(IWorkbenchPart part) {
				if (part == activeScaveEditor)
					activeEditorChanged(null);
			}

			public void partBroughtToTop(IWorkbenchPart part) {}
			public void partDeactivated(IWorkbenchPart part) {}
			public void partOpened(IWorkbenchPart part) {}
		};
		getSite().getPage().addPartListener(partListener);

		activeEditorChanged(getActiveEditor());
	}

	private void hookEditorListeners(IEditorPart editor) {
		if (editor instanceof ScaveEditor) {
			ScaveEditor scaveEditor = (ScaveEditor)editor;
			scaveEditor.getResultFileManager().addChangeListener(resultFilesChangeListener =
				new IResultFilesChangeListener() {
				public void resultFileManagerChanged(ResultFileManager manager) {
					if (scheduledUpdate == null) {
						scheduledUpdate = new Runnable() {
							public void run() {
								scheduledUpdate = null;
								updateDataControl();
							}
						};
						Display.getDefault().asyncExec(scheduledUpdate);
					}
				}
			});

			IChangeNotifier notifier = (IChangeNotifier)scaveEditor.getAdapterFactory();
			notifier.addListener(modelChangeListener = new INotifyChangedListener() {
				public void notifyChanged(Notification notification) {
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							updateDataControl();
						}
					});
				}
			});
		}
	}

	private void unhookPageListeners() {
		IWorkbenchPage page = getSite().getPage();
		if (selectionChangedListener != null) {
			page.removePostSelectionListener(selectionChangedListener);
			selectionChangedListener = null;
		}
		if (partListener != null) {
			page.removePartListener(partListener);
			partListener = null;
		}
	}

	private void unhookEditorListeners(IEditorPart editor) {
		if (editor instanceof ScaveEditor) {
			ScaveEditor scaveEditor = (ScaveEditor)editor;
			if (resultFilesChangeListener != null)
				scaveEditor.getResultFileManager().removeChangeListener(resultFilesChangeListener);

			if (modelChangeListener != null) {
				IChangeNotifier notifier = (IChangeNotifier)scaveEditor.getAdapterFactory();
				notifier.removeListener(modelChangeListener);
			}
		}
	}

	private void setVisibleDataPanel(FilteredDataPanel panel) {
		if (setFilterAction != null)
			setFilterAction.update(panel);
		updateContentDescription();
	}

	private void workbechSelectionChanged(ISelection selection) {
		Dataset dataset = null;
		DatasetItem item = null;
		ResultItemRef resultItem = null;

		if (selection == this.selection)
			return;

		if (selection instanceof IStructuredSelection && ((IStructuredSelection)selection).getFirstElement() instanceof EObject) {
			EObject selected = (EObject)((IStructuredSelection)selection).getFirstElement();
			dataset = ScaveModelUtil.findEnclosingOrSelf(selected, Dataset.class);
			item = ScaveModelUtil.findEnclosingOrSelf(selected, DatasetItem.class);
			setInput(dataset, item);
		}
		else if (selection instanceof IStructuredSelection) {
			Object selectedObject = ((IStructuredSelection)selection).getFirstElement();
			if (selectedObject instanceof ChartLine) {
				ChartLine selectedLine = (ChartLine)selectedObject;
				item = selectedLine.getChart();
				dataset = ScaveModelUtil.findEnclosingDataset(item);
				resultItem = selectedLine.getResultItemRef();
			}
		}

		if (dataset != null) {
			setInput(dataset, item);
			if (resultItem != null && resultItem.getID() != -1L) {
				selectResultItem(resultItem);
			}
		}
		else {
			setInput(null, null);
			showMessage("No dataset item selected.");
		}
	}

	private void selectResultItem(ResultItemRef item) {
		FilteredDataPanel panel = tabFolder.getFilteredDataPanel(item.getID());
		if (panel != null && !panel.isDisposed()) {
			IDataControl control = panel.getDataControl();
			if (!control.isDisposed()) {
				control.setSelectionByID(item.getID());
			}
		}
	}

	private void activeEditorChanged(IEditorPart editor) {
		if (editor != activeScaveEditor) {
			unhookEditorListeners(activeScaveEditor);
			activeScaveEditor = editor instanceof ScaveEditor ? (ScaveEditor)editor : null;
			hookEditorListeners(activeScaveEditor);

			if (activeScaveEditor != null) {
				ResultFileManager manager = activeScaveEditor.getResultFileManager();
				tabFolder.setResultFileManager(manager);
				labelProvider = new ScaveModelLabelProvider(new AdapterFactoryLabelProvider(activeScaveEditor.getAdapterFactory()));
				hideMessage();
			}
			else {
                tabFolder.setResultFileManager(null);
				labelProvider = null;
				showMessage("No active scave editor.");
			}
		}
	}

	private void setInput(Dataset dataset, DatasetItem item) {
		if (selectedDataset != dataset || selectedItem != item) {
			selectedDataset = dataset;
			selectedItem = item;
			updateDataControl();
			setActivePanel(item);
			updateContentDescription();
		}
	}

    private void setActivePanel(DatasetItem item) {
        ResultType type = null;

        if (item instanceof AddDiscardOp)
            type = ((AddDiscardOp)item).getType();
        else if (item instanceof BarChart || item instanceof ScatterChart)
            type = ResultType.SCALAR_LITERAL;
        else if (item instanceof LineChart)
            type = ResultType.VECTOR_LITERAL;
        else if (item instanceof HistogramChart)
            type = ResultType.HISTOGRAM_LITERAL;
// TODO:
//        else if (item instanceof Group)
//            type = ;
//        else if (item instanceof Apply)
//            type = ((Apply)item).;
//        else if (item instanceof Compute)
//            type = ((Compute)item).;

        if (type != null)
            tabFolder.setActivePanel(type);
        else
            tabFolder.setActivePanel(tabFolder.getAllPanel());
    }

	private void updateContentDescription() {
		if (labelProvider != null && selectedDataset != null) {
			ResultType visibleType = tabFolder.getActivePanelType();
			String type = (visibleType == ResultType.SCALAR_LITERAL ? "scalars" :
				 		   visibleType == ResultType.VECTOR_LITERAL ? "vectors" :
				 		   visibleType == ResultType.HISTOGRAM_LITERAL ? "histograms" : null);

			String desc = "";
			if (type != null)
				desc += type;
			if (selectedItem != null) {
				if (desc.length() > 0) desc += " at ";
				desc += "'" + labelProvider.getText(selectedItem) + "'";
			}
			if (desc.length() > 0)
				desc += " from ";
			else
				desc += "content of";
			desc += labelProvider.getText(selectedDataset);
			hideMessage();
			setContentDescription(desc);
		}
		else
			showMessage("No dataset item selected.");
	}

	/**
	 * Prevent calling showMessage until the view fully created.
	 */
	@Override
	protected void showMessage(String text) {
		if (viewControlCreated)
			super.showMessage(text);
	}

	private void updateDataControl() {
		if (activeScaveEditor == null) {
            Assert.isTrue(tabFolder.getAllPanel().getResultFileManager() == null);
			Assert.isTrue(tabFolder.getScalarsPanel().getResultFileManager() == null);
			Assert.isTrue(tabFolder.getVectorsPanel().getResultFileManager() == null);
			Assert.isTrue(tabFolder.getHistogramsPanel().getResultFileManager() == null);
            tabFolder.getAllPanel().setIDList(IDList.EMPTY);
			tabFolder.getScalarsPanel().setIDList(IDList.EMPTY);
			tabFolder.getVectorsPanel().setIDList(IDList.EMPTY);
			tabFolder.getHistogramsPanel().setIDList(IDList.EMPTY);
			tabFolder.refreshPanelTitles();
			return;
		}

		final ResultFileManager manager = activeScaveEditor.getResultFileManager();
		ResultFileManager.callWithReadLock(manager, new Callable<Object>() {
			public Object call() {
                IDList all = IDList.EMPTY;
				IDList scalars = IDList.EMPTY;
				IDList vectors = IDList.EMPTY;
				IDList histograms = IDList.EMPTY;
				if (selectedDataset != null) {
                    all = DatasetManager.getIDListFromDataset(manager, selectedDataset, selectedItem, null);
					scalars = DatasetManager.getIDListFromDataset(manager, selectedDataset, selectedItem, ResultType.SCALAR_LITERAL);
					vectors = DatasetManager.getIDListFromDataset(manager, selectedDataset, selectedItem, ResultType.VECTOR_LITERAL);
					histograms = DatasetManager.getIDListFromDataset(manager, selectedDataset, selectedItem, ResultType.HISTOGRAM_LITERAL);
				}
                tabFolder.getAllPanel().setIDList(all);
				tabFolder.getScalarsPanel().setIDList(scalars);
				tabFolder.getVectorsPanel().setIDList(vectors);
				tabFolder.getHistogramsPanel().setIDList(histograms);
	            tabFolder.refreshPanelTitles();
				return null;
			}
		});
	}

	private void showFilter(boolean show) {
        tabFolder.getAllPanel().showFilterPanel(show);
		tabFolder.getVectorsPanel().showFilterPanel(show);
		tabFolder.getScalarsPanel().showFilterPanel(show);
		tabFolder.getHistogramsPanel().showFilterPanel(show);
	}

	private boolean isFilterVisible() {
		return tabFolder.getVectorsPanel().isFilterPanelVisible();
	}

	/*
	 * Actions
	 */

	class ShowDataControlAction extends Action
	{
		FilteredDataPanel panel;

		public ShowDataControlAction(ResultType type) {
			super(null, IAction.AS_RADIO_BUTTON);

			switch (type.getValue()) {
			case ResultType.SCALAR:
				setText("Show scalars");
				setImageDescriptor(ImageFactory.getDescriptor(ImageFactory.TOOLBAR_IMAGE_SHOWSCALARS));
				panel = tabFolder.getScalarsPanel();
				break;
			case ResultType.VECTOR:
				setText("Show vectors");
				setImageDescriptor(ImageFactory.getDescriptor(ImageFactory.TOOLBAR_IMAGE_SHOWVECTORS));
				panel = tabFolder.getVectorsPanel();
				break;
			case ResultType.HISTOGRAM:
				setText("Show histograms");
				setImageDescriptor(ImageFactory.getDescriptor(ImageFactory.TOOLBAR_IMAGE_SHOWHISTOGRAMS));
				panel = tabFolder.getHistogramsPanel();
				break;
			default:
				Assert.isLegal(false, "Unknown result type: " + type);
				break;
			}
		}

		public void run() {
			setVisibleDataPanel(panel);
		}
	}

	class ToggleFilterAction extends Action
	{
		public ToggleFilterAction() {
			super(isFilterVisible() ? "Hide filter" : "Show filter", IAction.AS_CHECK_BOX);
			setDescription("Toggles the filtering panel on/off.");
			setImageDescriptor(ImageFactory.getDescriptor(ImageFactory.TOOLBAR_IMAGE_FILTER));
		}

		@Override
		public void run() {
			boolean visible = isFilterVisible();
			showFilter(
			        !visible);
			setText(visible ? "Show filter" : "Hide filter");
		}
	}

	class SelectAllAction extends Action
	{
		@Override
		public void run() {
			FilteredDataPanel panel = tabFolder.getActivePanel();
			if (panel != null && panel.getDataControl() != null) {
				panel.getDataControl().setFocus();
				panel.getDataControl().selectAll();
			}
		}
	}

	/*
	 * ISelectionProvider
	 */

	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		selectionChangeListeners.add(listener);
	}

	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		selectionChangeListeners.remove(listener);
	}

	protected void fireSelectionChangeEvent(ISelection selection) {
		SelectionChangedEvent event = new SelectionChangedEvent(this, selection);
		for (Object listener : selectionChangeListeners.getListeners())
			((ISelectionChangedListener)listener).selectionChanged(event);
	}

	public ISelection getSelection() {
		return selection;
	}

	public void setSelection(ISelection selection) {
		if (selection != this.selection) {
			this.selection = selection;
			fireSelectionChangeEvent(selection);
		}
	}
}
