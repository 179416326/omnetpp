package org.omnetpp.scave.editors.ui;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.TableItem;
import org.omnetpp.scave.editors.IDListSelection;
import org.omnetpp.scave.editors.ScaveEditor;
import org.omnetpp.scave.editors.ScaveEditorContributor;
import org.omnetpp.scave.editors.datatable.ChooseTableColumnsAction;
import org.omnetpp.scave.editors.datatable.DataTable;
import org.omnetpp.scave.editors.datatable.FilteredDataPanel;
import org.omnetpp.scave.engine.IDList;
import org.omnetpp.scave.engine.ResultFileManager;
import org.omnetpp.scave.engine.VectorResult;
import org.omnetpp.scave.engineext.IResultFilesChangeListener;
import org.omnetpp.scave.engineext.ResultFileManagerEx;
import org.omnetpp.scave.model.ResultType;

/**
 * This is the "Browse data" page of Scave Editor
 */
public class BrowseDataPage extends ScaveEditorPage {

	// UI elements
	private Label label;
	private TabFolder tabfolder;

	private TabItem vectorsTab;
	private TabItem scalarsTab;
	private TabItem histogramsTab;
	private FilteredDataPanel vectorsPanel;
	private FilteredDataPanel scalarsPanel;
	private FilteredDataPanel histogramsPanel;

	private IResultFilesChangeListener fileChangeListener;
	private SelectionListener selectionChangeListener;


	public BrowseDataPage(Composite parent, ScaveEditor editor) {
		super(parent, SWT.V_SCROLL, editor);
		initialize();
		hookListeners();
	}

	@Override
	public void dispose() {
		unhookListeners();
		super.dispose();
	}

	public FilteredDataPanel getScalarsPanel() {
		return scalarsPanel;
	}

	public FilteredDataPanel getVectorsPanel() {
		return vectorsPanel;
	}

	public FilteredDataPanel getHistogramsPanel() {
		return histogramsPanel;
	}

	public FilteredDataPanel getActivePanel() {
		int index = tabfolder.getSelectionIndex();
		if (index >= 0)
			return (FilteredDataPanel)tabfolder.getItem(index).getControl();
		else
			return null;
	}

	public void setActivePanel(FilteredDataPanel panel) {
		TabItem[] items = tabfolder.getItems();
		for (int i = 0; i < items.length; ++i)
			if (items[i].getControl() == panel) {
				tabfolder.setSelection(i);
				return;
			}
	}

	public ResultType getActivePanelType() {
		FilteredDataPanel activePanel = getActivePanel();
		if (activePanel == vectorsPanel)
			return ResultType.VECTOR_LITERAL;
		else if (activePanel == scalarsPanel)
			return ResultType.SCALAR_LITERAL;
		else if (activePanel == histogramsPanel)
			return ResultType.HISTOGRAM_LITERAL;
		else
			throw new IllegalStateException();
	}

	private void initialize() {
		// set up UI
		setPageTitle("Browse data");
		setFormTitle("Browse data");
		//setBackground(ColorFactory.asColor("white"));
		setExpandHorizontal(true);
		setExpandVertical(true);
		getBody().setLayout(new GridLayout());
		label = new Label(getBody(), SWT.WRAP);
		label.setText("Here you can see all data that come from the files specified in the Inputs page.");
		label.setBackground(this.getBackground());
		createTabFolder();

		configurePanel(scalarsPanel);
		configurePanel(vectorsPanel);
		configurePanel(histogramsPanel);

		// set up contents
		ResultFileManagerEx manager = scaveEditor.getResultFileManager();
		scalarsPanel.setResultFileManager(manager);
		vectorsPanel.setResultFileManager(manager);
		histogramsPanel.setResultFileManager(manager);

		refreshPage(manager);
	}

	private void createTabFolder() {
		tabfolder = new TabFolder(getBody(), SWT.TOP);
		//tabfolder.setBackground(new Color(null,255,255,255));
		tabfolder.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL |
											  GridData.GRAB_VERTICAL |
											  GridData.FILL_BOTH));

		// create pages
		vectorsPanel = new FilteredDataPanel(tabfolder, SWT.NONE, DataTable.TYPE_VECTOR);
		configureFilteredDataPanel(vectorsPanel);
		scalarsPanel = new FilteredDataPanel(tabfolder, SWT.NONE, DataTable.TYPE_SCALAR);
		configureFilteredDataPanel(scalarsPanel);
		histogramsPanel = new FilteredDataPanel(tabfolder, SWT.NONE, DataTable.TYPE_HISTOGRAM);
		configureFilteredDataPanel(histogramsPanel);

		// create tabs (note: tab labels will be refreshed from initialize())
		vectorsTab = addItem("Vectors", vectorsPanel);
		scalarsTab = addItem("Scalars", scalarsPanel);
		histogramsTab = addItem("Histograms", histogramsPanel);

		tabfolder.setSelection(0);
	}

	private TabItem addItem(String text, Control control) {
		TabItem item = new TabItem(tabfolder, SWT.NONE);
		item.setText(text);
		item.setControl(control);
		return item;
	}

	private void configurePanel(FilteredDataPanel panel) {
		// populate the popup menu of the panel
		IMenuManager contextMenuManager = panel.getTable().getContextMenuManager();
		ScaveEditorContributor editorContributor = ScaveEditorContributor.getDefault();
		contextMenuManager.add(editorContributor.getCreateTempChartAction());
		contextMenuManager.add(new Separator());
		contextMenuManager.add(editorContributor.getAddFilterToDatasetAction());
		contextMenuManager.add(editorContributor.getAddSelectedToDatasetAction());
		contextMenuManager.add(new Separator());
		contextMenuManager.add(editorContributor.getCopyToClipboardAction());
		contextMenuManager.add(new Separator());
		contextMenuManager.add(new ChooseTableColumnsAction(panel.getTable()));
		contextMenuManager.add(new Separator());
		contextMenuManager.add(editorContributor.getShowVectorBrowserViewAction());
	}

	private void hookListeners() {
		// asynchronously update the page contents on result file changes (ie. input file load/unload)
		if (fileChangeListener == null) {
			fileChangeListener = new IResultFilesChangeListener() {
				public void resultFileManagerChanged(final ResultFileManager manager) {
					getDisplay().asyncExec(new Runnable() {
						public void run() {
							refreshPage(manager);
						}
					});
				}
			};
			scaveEditor.getResultFileManager().addListener(fileChangeListener);
		}

		// when they double-click in the vectors panel, open chart
		//FIXME remembering selectionChangeListener, and unhooking it in unhookListeners() is probably redundant
		if (selectionChangeListener == null) {
			selectionChangeListener = new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (e.getSource() instanceof DataTable) {
						DataTable table = (DataTable)e.getSource();
						scaveEditor.setSelection(new IDListSelection(table.getSelectedIDs(), table.getResultFileManager()));
					}
					
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					ScaveEditorContributor editorContributor = ScaveEditorContributor.getDefault();
					editorContributor.getCreateTempChartAction().run();
				}
			};
			vectorsPanel.getTable().addSelectionListener(selectionChangeListener);
			scalarsPanel.getTable().addSelectionListener(selectionChangeListener);
			histogramsPanel.getTable().addSelectionListener(selectionChangeListener);
		}
	}

	private void unhookListeners() {
		if (fileChangeListener != null) {
			ResultFileManagerEx manager = scaveEditor.getResultFileManager();
			manager.removeListener(fileChangeListener);
			fileChangeListener = null;
		}
		if (selectionChangeListener != null) {
			vectorsPanel.getTable().removeSelectionListener(selectionChangeListener);
			scalarsPanel.getTable().removeSelectionListener(selectionChangeListener);
			histogramsPanel.getTable().removeSelectionListener(selectionChangeListener);
			selectionChangeListener = null;
		}
	}

	@Override
	public void pageSelected() {
		// when the user switches to this page, try to show a tab that actually
		// contains some data
		if (getActivePanel().getTable().getItemCount() == 0) {
			FilteredDataPanel panelToActivate = null;
			if (vectorsPanel.getTable().getItemCount() > 0)
				panelToActivate = vectorsPanel;
			else if (scalarsPanel.getTable().getItemCount() > 0)
				panelToActivate = scalarsPanel;
			else if (histogramsPanel.getTable().getItemCount() > 0)
				panelToActivate = histogramsPanel;
			if (panelToActivate != null)
				this.setActivePanel(panelToActivate);
		}
	}

	/**
	 * Updates the page: retrieves the list of vectors, scalars, etc from
	 * ResultFileManager, updates the data tables with them, and updates
	 * the tab labels as well.
	 */
	protected void refreshPage(final ResultFileManager manager) {
		IDList vectors = manager.getAllVectors();
		IDList scalars = manager.getAllScalars();
		IDList histograms = new IDList(); // TODO

		vectorsTab.setText("Vectors ("+vectors.size()+")");
		scalarsTab.setText("Scalars ("+scalars.size()+")");
		histogramsTab.setText("Histograms ("+histograms.size()+")");

		scalarsPanel.setIDList(scalars);
		vectorsPanel.setIDList(vectors);
		histogramsPanel.setIDList(histograms);
	}
}
