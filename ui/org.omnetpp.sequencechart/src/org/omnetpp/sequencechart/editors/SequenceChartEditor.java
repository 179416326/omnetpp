package org.omnetpp.sequencechart.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.INavigationLocation;
import org.eclipse.ui.INavigationLocationProvider;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.part.EditorPart;
import org.omnetpp.common.canvas.RubberbandSupport;
import org.omnetpp.eventlog.engine.EventLog;
import org.omnetpp.eventlog.engine.FileReader;
import org.omnetpp.eventlog.engine.FilteredEventLog;
import org.omnetpp.eventlog.engine.IEvent;
import org.omnetpp.eventlog.engine.IEventLog;
import org.omnetpp.eventlog.engine.IntVector;
import org.omnetpp.eventlog.engine.ModuleCreatedEntry;
import org.omnetpp.eventlog.selection.IEventLogSelection;
import org.omnetpp.scave.engine.DataflowManager;
import org.omnetpp.scave.engine.IDList;
import org.omnetpp.scave.engine.Node;
import org.omnetpp.scave.engine.NodeType;
import org.omnetpp.scave.engine.NodeTypeRegistry;
import org.omnetpp.scave.engine.ResultFile;
import org.omnetpp.scave.engine.StringMap;
import org.omnetpp.scave.engine.VectorResult;
import org.omnetpp.scave.engine.XYArray;
import org.omnetpp.scave.engineext.ResultFileManagerEx;
import org.omnetpp.sequencechart.moduletree.ModuleTreeBuilder;
import org.omnetpp.sequencechart.moduletree.ModuleTreeItem;
import org.omnetpp.sequencechart.widgets.ModuleTreeDialog;
import org.omnetpp.sequencechart.widgets.SequenceChart;

/**
 * Sequence chart display tool. (It is not actually an editor; it is only named so
 * because it extends EditorPart).
 * 
 * @author andras
 */
//FIXME unhook from listeners (there are "widget is disposed" errors in the log after the editor is closed)  
public class SequenceChartEditor extends EditorPart implements INavigationLocationProvider, IGotoMarker {

	private SequenceChart sequenceChart;
	private IEventLog eventLog;  // the log file loaded
	private ModuleTreeItem moduleTree; // modules in eventLog
	private ArrayList<ModuleTreeItem> selectedAxisModules; // which modules should have an axis
	private int tracedEventNumber = -1;
	private ResultFileManagerEx resultFileManager; 
	private IDList idlist; // idlist of the loaded vector file
	private XYArray[] stateVectors; // vector file loaded for the log file

	private final Color CHART_BACKGROUND_COLOR = ColorConstants.white;

	public SequenceChartEditor() {
		super();
	}

    public void gotoMarker(IMarker marker)
    {
// TODO: pass down marker
    }
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		
		String logFileName;
		if (input instanceof IFileEditorInput) {
			IFileEditorInput fileInput = (IFileEditorInput)input;
// TODO: this is how markers can be created
//			try {
//				IMarker marker = fileInput.getFile().createMarker(IMarker.BOOKMARK);
//				marker.setAttribute(IMarker.MESSAGE, "ALMA");
//			} catch (CoreException e) {
//				throw new RuntimeException(e);
//			}
//			fileInput.getFile().setPersistentProperty(key, logFileName);
//			fileInput.getFile().setSessionProperty(key, fileInput);
			logFileName = fileInput.getFile().getLocation().toFile().getAbsolutePath();
		}
		else if (input instanceof IPathEditorInput) {
			IPathEditorInput pathFileInput = (IPathEditorInput)input;
			logFileName = pathFileInput.getPath().toFile().getAbsolutePath();
		}
		else 
			throw new PartInitException("Unsupported input type");

		eventLog = new EventLog(new FileReader(logFileName, /* EventLog will delete it */false));

		String vectorFileName = logFileName.replaceFirst("\\.log$", ".vec");
		if (!vectorFileName.equals(logFileName) && new java.io.File(vectorFileName).exists()) {
			stateVectors = readVectorFile(vectorFileName);
			System.out.println("read "+stateVectors.length+" vectors from "+vectorFileName);
		}
		
		setPartName(input.getName());
		
		buildModuleTree();

		// try to open the log view
		// TODO: resurrect
//		try {
			// Eclipse feature: during startup, showView() throws "Abnormal Workbench Condition" because perspective is null
//			if (getSite().getPage().getPerspective()!=null)
//				getSite().getPage().showView(EventLogTableView.PART_ID);
//		} catch (PartInitException e) {
//			SeqChartPlugin.getDefault().logException(e);					
//		}
	}
	
	private XYArray[] readVectorFile(String fileName)
	{
		resultFileManager = new ResultFileManagerEx();
		ResultFile file = resultFileManager.loadFile(fileName);
		idlist = resultFileManager.getAllVectors();
		idlist = resultFileManager.filterIDList(idlist, null, "*", "State");
		DataflowManager net = new DataflowManager();
		NodeTypeRegistry factory = NodeTypeRegistry.instance();

		// create VectorFileReader nodes
		NodeType vectorFileReaderType = factory.getNodeType("vectorfilereader");
		StringMap args = new StringMap();
		args.set("filename", file.getFilePath());
		Node fileReaderNode = vectorFileReaderType.create(net, args);

		// create network
		NodeType removeRepeatsType = factory.getNodeType("removerepeats");
		NodeType arrayBuilderType = factory.getNodeType("arraybuilder");
		Node [] arrayBuilderNodes = new Node[(int)idlist.size()];
		for (int i = 0; i < (int)idlist.size(); i++) {
			VectorResult vec = resultFileManager.getVector(idlist.get(i));
			// no filter: connect directly to an ArrayBuilder
			args = new StringMap();
			Node removeRepeatsNode = removeRepeatsType.create(net, args);
			Node arrayBuilderNode = arrayBuilderType.create(net, args);
			arrayBuilderNodes[i] = arrayBuilderNode;
			net.connect(vectorFileReaderType.getPort(fileReaderNode, "" + vec.getVectorId()),
						removeRepeatsType.getPort(removeRepeatsNode, "in"));
			net.connect(removeRepeatsType.getPort(removeRepeatsNode, "out"),
						arrayBuilderType.getPort(arrayBuilderNode, "in"));
		}

		// run the netwrork
		net.dump();
		net.execute();

		// extract results
		XYArray[] xyArray = new XYArray[arrayBuilderNodes.length];
		for (int i = 0; i < arrayBuilderNodes.length; i++)
			xyArray[i] = arrayBuilderNodes[i].getArray();
		
		return xyArray;
	}
	
	private void buildModuleTree() {
		ModuleTreeBuilder treeBuilder = new ModuleTreeBuilder();
		for (int i = 1; i <= eventLog.getNumModuleCreatedEntries(); i++) {
			ModuleCreatedEntry entry = eventLog.getModuleCreatedEntry(i);
			
			if (entry != null)
				treeBuilder.addModule(entry.getParentModuleId(), entry.getModuleId(), entry.getModuleClassName(), entry.getFullName() + i);
		}

		moduleTree = treeBuilder.getModuleTree();
	}
	
	private ArrayList<ModuleTreeItem> getAllAxisModules() {
		final ArrayList<ModuleTreeItem> modules = new ArrayList<ModuleTreeItem>();
		moduleTree.visitLeaves(new ModuleTreeItem.IModuleTreeItemVisitor() {
			public void visit(ModuleTreeItem treeItem) {
				if (treeItem != moduleTree)
					modules.add(treeItem);
			}
		});

		return modules;
	}
	
	@Override
	public void createPartControl(Composite parent) {
		// add sequence chart 
		Composite upper = new Composite(parent, SWT.NONE);
		upper.setLayout(new GridLayout());

		// create sequence chart widget
		sequenceChart = new SequenceChart(upper, SWT.DOUBLE_BUFFERED);
		sequenceChart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		sequenceChart.setBackground(CHART_BACKGROUND_COLOR);
		new RubberbandSupport(sequenceChart, SWT.CTRL) {
			@Override
			public void rubberBandSelectionMade(Rectangle r) {
				sequenceChart.zoomToRectangle(new org.eclipse.draw2d.geometry.Rectangle(r));
				markLocation();
			}
		};

		// create control strip (this needs the seqChart pointer)
		Composite controlStrip = createControlStrip(upper);
		controlStrip.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		controlStrip.moveAbove(sequenceChart);

		// set up operations: click, double-click
		sequenceChart.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// on double-click, filter the event log
				List<IEvent> events = ((IEventLogSelection)sequenceChart.getSelection()).getEvents();
				if (events.size()>1) { 
					//XXX pop up selection dialog instead?
					MessageDialog.openInformation(getEditorSite().getShell(), "Information", "Ambiguous double-click: there are "+events.size()+" events under the mouse! Zooming may help.");
				} else if (events.size()==1) {
					showSequenceChartForEvent(events.get(0).getEventNumber());
				}
			}
		});
		
		sequenceChart.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				if (e.button == 3)
					displayPopupMenu(e);
			}
		});

		// give eventLog to the chart for display
		showFullSequenceChart();
		
		getSite().setSelectionProvider(sequenceChart);
		
		// follow selection
		getSite().getPage().addSelectionListener(new ISelectionListener() {
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				if (part!=sequenceChart) {
					sequenceChart.setSelection(selection);
					markLocation();
				}
			}
		});
	}

	private Composite createControlStrip(Composite upper) {
		Composite controlStrip = new Composite(upper, SWT.NONE);
		RowLayout layout = new RowLayout();
		controlStrip.setLayout(layout);

		Combo timelineSortMode = new Combo(controlStrip, SWT.NONE);
		for (SequenceChart.TimelineSortMode t : SequenceChart.TimelineSortMode.values())
			timelineSortMode.add(t.name());
		timelineSortMode.select(sequenceChart.getTimelineSortMode().ordinal());
		timelineSortMode.setVisibleItemCount(SequenceChart.TimelineSortMode.values().length);
		
		Combo timelineMode = new Combo(controlStrip, SWT.NONE);
		for (SequenceChart.TimelineMode t : SequenceChart.TimelineMode.values())
			timelineMode.add(t.name());
		// TODO: timelineMode.select(seqChart.getTimelineMode().ordinal());
		timelineMode.setVisibleItemCount(SequenceChart.TimelineMode.values().length);
		
		Button showNonDeliveryMessages = new Button(controlStrip, SWT.CHECK);
		showNonDeliveryMessages.setText("Usage arrows");
		
		Button showEventNumbers = new Button(controlStrip, SWT.CHECK);
		showEventNumbers.setText("Event#");

		Button showMessageNames = new Button(controlStrip, SWT.CHECK);
		showMessageNames.setText("Msg name");
		
		Button showArrowHeads = new Button(controlStrip, SWT.CHECK);
		showArrowHeads.setText("Arrowheads");

		Button canvasCaching = new Button(controlStrip, SWT.CHECK);
		canvasCaching.setText("Caching");
		
		Button selectModules = new Button(controlStrip, SWT.NONE);
		selectModules.setText("Modules...");
		
		Button zoomIn = new Button(controlStrip, SWT.NONE);
		zoomIn.setText("Zoom in");
		
		Button zoomOut = new Button(controlStrip, SWT.NONE);
		zoomOut.setText("Zoom out");

		Button increaseSpacing = new Button(controlStrip, SWT.NONE);
		increaseSpacing.setText("Increase spacing");
		
		Button decreaseSpacing = new Button(controlStrip, SWT.NONE);
		decreaseSpacing.setText("Decrease spacing");

		selectModules.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				displayModuleTreeDialog();
			}});

		zoomIn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				sequenceChart.zoomIn();
				markLocation();
			}});
		
		zoomOut.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				sequenceChart.zoomOut();
				markLocation();
			}});

		increaseSpacing.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				sequenceChart.setAxisSpacing(sequenceChart.getAxisSpacing()+5);
			}});
		
		decreaseSpacing.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (sequenceChart.getAxisSpacing()>5)
					sequenceChart.setAxisSpacing(sequenceChart.getAxisSpacing()-5);
			}});
		
		showMessageNames.setSelection(sequenceChart.getShowMessageNames());
		showMessageNames.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				sequenceChart.setShowMessageNames(((Button)e.getSource()).getSelection());
			}
		});
		
		showNonDeliveryMessages.setSelection(sequenceChart.getShowNonDeliveryMessages());
		showNonDeliveryMessages.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				sequenceChart.setShowNonDeliveryMessages(((Button)e.getSource()).getSelection());
			}
		});
		
		showEventNumbers.setSelection(sequenceChart.getShowEventNumbers());
		showEventNumbers.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				sequenceChart.setShowEventNumbers(((Button)e.getSource()).getSelection());
			}
		});

		showArrowHeads.setSelection(sequenceChart.getShowArrowHeads());
		showArrowHeads.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				sequenceChart.setShowArrowHeads(((Button)e.getSource()).getSelection());
			}
		});
		
		canvasCaching.setSelection(sequenceChart.getCaching());
		canvasCaching.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				sequenceChart.setCaching(((Button)e.getSource()).getSelection());
				sequenceChart.redraw();
			}
		});

		timelineMode.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				sequenceChart.setTimelineMode(SequenceChart.TimelineMode.values()[((Combo)e.getSource()).getSelectionIndex()]);
			}
		});
		
		timelineSortMode.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				sequenceChart.setTimelineSortMode(SequenceChart.TimelineSortMode.values()[((Combo)e.getSource()).getSelectionIndex()]);
			}
		});
		
		return controlStrip;
	}

	/**
	 * Goes to the given event and updates the chart.
	 */
	private void showSequenceChartForEvent(int eventNumber) {
		IEvent event = eventLog.getEventForEventNumber(eventNumber);

		if (event == null)
			MessageDialog.openError(getEditorSite().getShell(), "Error", "Event #" + eventNumber + " not found.");
		else {
			tracedEventNumber = eventNumber;	
			showFilteredEventLog();
		}
	}

	private void showFullSequenceChart() {
		tracedEventNumber = -1;
		selectedAxisModules = getAllAxisModules();
		showEventLog(eventLog, selectedAxisModules);
	}
	
	/**
	 * Filters event log by the currently selected event number and modules.
	 */
	private void showFilteredEventLog() {
		showFilteredEventLog(eventLog, selectedAxisModules, tracedEventNumber);
	}

	private void showFilteredEventLog(IEventLog eventLog, ArrayList<ModuleTreeItem> axisModules, int eventNumber) {
		final IntVector moduleIds = new IntVector();

		for (int i = 0; i < axisModules.size(); i++) {
			ModuleTreeItem treeItem = axisModules.get(i);
			treeItem.visitLeaves(new ModuleTreeItem.IModuleTreeItemVisitor() {
				public void visit(ModuleTreeItem treeItem) {
					moduleIds.add(treeItem.getModuleId());
				}
			});
		}
		FilteredEventLog filteredEventLog = new FilteredEventLog(eventLog);
		filteredEventLog.setModuleIds(moduleIds);
		filteredEventLog.setTracedEventNumber(eventNumber);
		showEventLog(filteredEventLog, axisModules);
	}

	private void showEventLog(IEventLog eventLog,  ArrayList<ModuleTreeItem> axisModules) {
		ArrayList<XYArray> axisVectors = new ArrayList<XYArray>();
		for (ModuleTreeItem treeItem : axisModules) {
			axisVectors.add(null);

			if (idlist != null) 
				for (int i = 0; i < idlist.size(); i++)
					if (resultFileManager.getItem(idlist.get(i)).getModuleName().equals(treeItem.getModuleFullPath())) {
						axisVectors.set(axisVectors.size() - 1, stateVectors[i]);
						break;
					}
		}
		
		sequenceChart.setParameters(eventLog, axisModules, axisVectors);
	}

	protected void displayModuleTreeDialog() {
		ModuleTreeDialog dialog = new ModuleTreeDialog(getSite().getShell(), moduleTree, selectedAxisModules);
		dialog.open();
		Object[] selection = dialog.getResult(); 
		if (selection != null) { // not cancelled
			selectedAxisModules = new ArrayList<ModuleTreeItem>();
			for (Object sel : selection)
				selectedAxisModules.add((ModuleTreeItem)sel);

			System.out.println("Selected:");
			for (ModuleTreeItem sel : selectedAxisModules)
				System.out.println(" "+sel.getModuleFullPath());

			showFilteredEventLog();
		}
	}
	
	private void displayPopupMenu(MouseEvent e) {
/* FIXME: resurrect this code
		final int x = e.x;
		Menu popupMenu = new Menu(seqChart);
		ArrayList<IEvent> events = new ArrayList<IEvent>();
		ArrayList<MessageEntry> msgs = new ArrayList<MessageEntry>();
		Point p = seqChart.toControl(seqChart.getDisplay().getCursorLocation());
		seqChart.collectStuffUnderMouse(p.x, p.y, events, msgs);

		// center menu item
		MenuItem subMenuItem = new MenuItem(popupMenu, SWT.PUSH);
		subMenuItem.setText("Center");
		subMenuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				seqChart.gotoSimulationTimeWithCenter(seqChart.pixelToSimulationTime(x));
			}
		});

		// axis submenu
		MenuItem cascadeItem = new MenuItem(popupMenu, SWT.CASCADE);
		cascadeItem.setText("Axis");
		Menu subMenu = new Menu(popupMenu);
		cascadeItem.setMenu(subMenu);

		subMenuItem = new MenuItem(subMenu, SWT.PUSH);
		subMenuItem.setText("Dense");
		subMenuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				seqChart.setAxisSpacing(20);
			}
		});
		
		subMenuItem = new MenuItem(subMenu, SWT.PUSH);
		subMenuItem.setText("Evenly");
		subMenuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				seqChart.setAxisSpacing(-1);
			}
		});
		
		new MenuItem(popupMenu, SWT.SEPARATOR);
		
		// events submenu
		for (final IEvent event : events) {
			cascadeItem = new MenuItem(popupMenu, SWT.CASCADE);
			cascadeItem.setText(seqChart.getEventText(event));
			subMenu = new Menu(popupMenu);
			cascadeItem.setMenu(subMenu);

			subMenuItem = new MenuItem(subMenu, SWT.PUSH);
			subMenuItem.setText("Center");
			subMenuItem.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					seqChart.gotoEvent(event);
				}
			});

			subMenuItem = new MenuItem(subMenu, SWT.PUSH);
			subMenuItem.setText("Select");
			subMenuItem.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					// TODO:
				}
			});

			subMenuItem = new MenuItem(subMenu, SWT.PUSH);
			subMenuItem.setText("Filter to");
			subMenuItem.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					showSequenceChartForEvent(event.getEventNumber());
				}
			});
		}
		
		new MenuItem(popupMenu, SWT.SEPARATOR);

		// messages submenu
		for (final MessageEntry msg : msgs) {
			cascadeItem = new MenuItem(popupMenu, SWT.CASCADE);
			cascadeItem.setText(seqChart.getMessageText(msg));
			subMenu = new Menu(popupMenu);
			cascadeItem.setMenu(subMenu);

			subMenuItem = new MenuItem(subMenu, SWT.PUSH);
			subMenuItem.setText("Zoom to message");
			subMenuItem.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					seqChart.gotoSimulationTimeRange(msg.getSource().getSimulationTime(), msg.getTarget().getSimulationTime(), (int)(seqChart.getWidth() * 0.1));
				}
			});

			subMenuItem = new MenuItem(subMenu, SWT.PUSH);
			subMenuItem.setText("Go to source event");
			subMenuItem.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					seqChart.gotoEvent(msg.getSource());
				}
			});

			subMenuItem = new MenuItem(subMenu, SWT.PUSH);
			subMenuItem.setText("Go to target event");
			subMenuItem.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					seqChart.gotoEvent(msg.getTarget());
				}
			});
		}
		
		// axis submenu
		final ModuleTreeItem axisModule = seqChart.findAxisAt(p.y);
		if (axisModule != null) {
			cascadeItem = new MenuItem(popupMenu, SWT.CASCADE);
			cascadeItem.setText(seqChart.getAxisText(axisModule));
			subMenu = new Menu(popupMenu);
			cascadeItem.setMenu(subMenu);

			subMenuItem = new MenuItem(subMenu, SWT.PUSH);
			subMenuItem.setText("Center");
			subMenuItem.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					seqChart.gotoAxisModule(axisModule);
				}
			});

			subMenuItem = new MenuItem(subMenu, SWT.PUSH);
			subMenuItem.setText("Zoom to value");
			subMenuItem.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					seqChart.gotoAxisValue(axisModule, seqChart.pixelToSimulationTime(x));
				}
			});			
		}
		
		seqChart.setMenu(popupMenu);
*/
	}

	public void dispose() {
		super.dispose();
	}

	@Override
	public void setFocus() {
		sequenceChart.setFocus();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}
	
	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	public void markLocation() {
		getSite().getPage().getNavigationHistory().markLocation(SequenceChartEditor.this);
	}
	
	public class SequenceChartLocation implements INavigationLocation {
		private double startSimulationTime;
		private double endSimulationTime;
		
		public SequenceChartLocation(double startSimulationTime, double endSimulationTime) {
			this.startSimulationTime = startSimulationTime;
			this.endSimulationTime = endSimulationTime;
		}

		public void dispose() {
			// void
		}

		public Object getInput() {
			return SequenceChartEditor.this.getEditorInput();
		}

		public String getText() {
			return SequenceChartEditor.this.getPartName() + ": " + startSimulationTime + "s - " + endSimulationTime + "s";
		}

		public boolean mergeInto(INavigationLocation currentLocation) {
			return false;
		}

		public void releaseState() {
			// void
		}

		public void restoreLocation() {
			sequenceChart.gotoSimulationTimeRange(startSimulationTime, endSimulationTime);
		}

		public void restoreState(IMemento memento) {
			// TODO: implement
		}

		public void saveState(IMemento memento) {
			// TODO: implement
		}

		public void setInput(Object input) {
			SequenceChartEditor.this.setInput((IFileEditorInput)input);
		}

		public void update() {
			// void
		}
	}

	public INavigationLocation createEmptyNavigationLocation() {
		return new SequenceChartLocation(0, Double.NaN);
	}

	public INavigationLocation createNavigationLocation() {
		return new SequenceChartLocation(sequenceChart.getViewportLeftSimulationTime(), sequenceChart.getViewportRightSimulationTime());
	}
}
