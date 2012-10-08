/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.editors.datatable;

import static org.omnetpp.scave.engine.ResultItemField.FILE;
import static org.omnetpp.scave.engine.ResultItemField.MODULE;
import static org.omnetpp.scave.engine.ResultItemField.NAME;
import static org.omnetpp.scave.engine.ResultItemField.RUN;
import static org.omnetpp.scave.engine.RunAttribute.CONFIGNAME;
import static org.omnetpp.scave.engine.RunAttribute.EXPERIMENT;
import static org.omnetpp.scave.engine.RunAttribute.MEASUREMENT;
import static org.omnetpp.scave.engine.RunAttribute.REPLICATION;
import static org.omnetpp.scave.engine.RunAttribute.RUNNUMBER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.omnetpp.common.engine.BigDecimal;
import org.omnetpp.common.util.CsvWriter;
import org.omnetpp.scave.ScavePlugin;
import org.omnetpp.scave.engine.HistogramResult;
import org.omnetpp.scave.engine.IDList;
import org.omnetpp.scave.engine.ResultFileManager;
import org.omnetpp.scave.engine.ResultItem;
import org.omnetpp.scave.engine.ScalarResult;
import org.omnetpp.scave.engine.VectorResult;
import org.omnetpp.scave.engineext.ResultFileManagerEx;
import org.omnetpp.scave.model.ResultType;

/**
 * This is a preconfigured VIRTUAL table, which displays a list of
 * output vectors, output scalars or histograms, given an IDList and
 * the corresponding ResultFileManager as input. It is optimized
 * for very large amounts of data. (Display time is constant,
 * so it can be used with even millions of table lines without
 * performance degradation).
 *
 * The user is responsible to keep contents up-to-date in case
 * ResultFileManager or IDList contents change. Refreshing can be
 * done either by a call to setIDList(), or by refresh().
 *
 * @author andras
 */
public class DataTable extends Table implements IDataControl {

	/**
	 * Keys used in getData(),setData()
	 */
	public static final String COLUMN_KEY = "DataTable.Column";
	public static final String ITEM_KEY = "DataTable.Item";

	static class Column {

		private String text;
		private String fieldName;
		private int defaultWidth;
		private boolean defaultVisible;

		public Column(String text, String fieldName, int defaultWidth, boolean defaultVisible) {
			this.text = text;
			this.fieldName = fieldName;
			this.defaultWidth = defaultWidth;
			this.defaultVisible = defaultVisible;
		}

		@Override
        public Column clone() {
			return new Column(this.text, this.fieldName, this.defaultWidth, this.defaultVisible);
		}

		@Override
        public boolean equals(Object other) {
			return other instanceof Column && this.text.equals(((Column)other).text);
		}

		@Override
        public int hashCode() {
			return text.hashCode();
		}
	}

	private static final Column
		COL_DIRECTORY = new Column("Folder", null, 60, true),
		COL_FILE = new Column("File name", FILE,100, true),
		COL_CONFIG = new Column("Config name", CONFIGNAME, 80, true),
		COL_RUNNUMBER = new Column("Run number", RUNNUMBER, 20, true),
		COL_RUN_ID = new Column("Run id", RUN, 100, true),
		COL_EXPERIMENT = new Column("Experiment", EXPERIMENT, 80, false),
		COL_MEASUREMENT = new Column("Measurement", MEASUREMENT, 120, false),
		COL_REPLICATION = new Column("Replication", REPLICATION, 60, false),
		COL_MODULE = new Column("Module", MODULE, 160, true),
		COL_DATA = new Column("Name", NAME, 100, true),
		COL_VALUE = new Column("Value", null, 80, true),
		COL_COUNT = new Column("Count", null, 50, true),
		COL_MEAN = new Column("Mean", null, 60, true),
		COL_STDDEV = new Column("StdDev", null, 60, true),
        COL_VARIANCE = new Column("Variance", null, 60, true),
		COL_MIN = new Column("Min", null, 60, false),
		COL_MAX = new Column("Max", null, 60, false),
		COL_VECTOR_ID = new Column("Vector id", null, 40, false),
		COL_MIN_TIME = new Column("Min time", null, 60, false),
		COL_MAX_TIME = new Column("Max time", null, 60, false);

	private static final Column[] allScalarColumns = new Column[] {
		COL_DIRECTORY, COL_FILE, COL_CONFIG, COL_RUNNUMBER, COL_RUN_ID,
		COL_EXPERIMENT, COL_MEASUREMENT, COL_REPLICATION,
		COL_MODULE, COL_DATA,
		COL_VALUE
	};

	private static final Column[] allVectorColumns = new Column[] {
		COL_DIRECTORY, COL_FILE, COL_CONFIG, COL_RUNNUMBER, COL_RUN_ID,
		COL_EXPERIMENT, COL_MEASUREMENT, COL_REPLICATION,
		COL_MODULE, COL_DATA,
		COL_VECTOR_ID,
		COL_COUNT, COL_MEAN, COL_STDDEV, COL_VARIANCE, COL_MIN, COL_MAX, COL_MIN_TIME, COL_MAX_TIME
	};

	private static final Column[] allHistogramColumns = new Column[] {
		COL_DIRECTORY, COL_FILE, COL_CONFIG, COL_RUNNUMBER, COL_RUN_ID,
		COL_EXPERIMENT, COL_MEASUREMENT, COL_REPLICATION,
		COL_MODULE, COL_DATA,
		COL_COUNT, COL_MEAN, COL_STDDEV, COL_VARIANCE, COL_MIN, COL_MAX
	};

	private ResultType type;
	private ResultFileManagerEx manager;
	private IDList idList;
	private ListenerList listeners;
	private List<Column> visibleColumns; // list of visible columns, this list will be saved and restored
	private IPreferenceStore preferences = ScavePlugin.getDefault().getPreferenceStore();

	// holds actions for the context menu for this data table
	private MenuManager contextMenuManager = new MenuManager("#PopupMenu");

	private static final ResultItem[] NULL_SELECTION = new ResultItem[0];

	private TableItem selectedItem;
	private TableColumn selectedColumn;

	public DataTable(Composite parent, int style, ResultType type) {
		super(parent, style | SWT.VIRTUAL | SWT.FULL_SELECTION);
		Assert.isTrue(type==ResultType.SCALAR_LITERAL || type==ResultType.VECTOR_LITERAL || type==ResultType.HISTOGRAM_LITERAL);
		this.type = type;
		setHeaderVisible(true);
		setLinesVisible(true);
		initDefaultState();
		initColumns();

		addListener(SWT.SetData, new Listener() {
			public void handleEvent(final Event e) {
				ResultFileManager.callWithReadLock(manager, new Callable<Object>() {
					public Object call() {
						TableItem item = (TableItem)e.item;
						int lineNumber = indexOf(item);
						fillTableLine(item, lineNumber);
						return null;
					}
				});
			}
		});

		setMenu(contextMenuManager.createContextMenu(this));

		addMouseListener(new MouseAdapter() {
			@Override
            public void mouseDown(MouseEvent event) {
				handleMouseDown(event);
			}
		});
	}

	/**
	 * Override the ban on subclassing of Table, after having read the doc of
	 * checkSubclass(). In this class we only build upon the public interface
	 * of Table, so there can be no unwanted side effects. We prefer subclassing
	 * to delegating all 1,000,000 Table methods to an internal Table instance.
	 */
	@Override
	protected void checkSubclass() {
	}

	public ResultType getType() {
		return type;
	}

	public void setResultFileManager(ResultFileManagerEx manager) {
		this.manager = manager;
	}

	public ResultFileManagerEx getResultFileManager() {
		return manager;
	}

	public void setIDList(IDList idlist) {
		this.idList = idlist;
		restoreSortOrder();
		refresh();
		fireContentChangedEvent();
	}

	public IDList getIDList() {
		return idList;
	}

	public IMenuManager getContextMenuManager() {
		return contextMenuManager;
	}

	protected Column[] getAllColumns() {
		switch (type.getValue()) {
		case ResultType.SCALAR:		return allScalarColumns;
		case ResultType.VECTOR:		return allVectorColumns;
		case ResultType.HISTOGRAM:	return allHistogramColumns;
		default: return null;
		}
	}

	public String[] getAllColumnNames() {
		Column[] columns = getAllColumns();
		String[] columnNames = new String[columns.length];
		for (int i = 0; i < columns.length; ++i)
			columnNames[i] = columns[i].text;
		return columnNames;
	}

	public String[] getVisibleColumnNames() {
		String[] columnNames = new String[visibleColumns.size()];
		for (int i = 0; i < visibleColumns.size(); ++i)
			columnNames[i] = visibleColumns.get(i).text;
		return columnNames;
	}

	public void setVisibleColumns(String[] columnTexts) {
		for (Column column : getAllColumns()) {
            TableColumn tableColumn = getTableColumn(column);
            boolean currentlyVisible = tableColumn != null;
		    boolean requestedVisible = ArrayUtils.indexOf(columnTexts, column.text) != -1;

			if (requestedVisible && !currentlyVisible)
				addColumn(column);
			else if (!requestedVisible && currentlyVisible){
                visibleColumns.remove(column);
                tableColumn.dispose();
			}
		}

		int position = 0;
		int[] columnOrder = new int[getColumns().length];
        for (Column column : getAllColumns())
            if (visibleColumns.indexOf(column) != -1)
                columnOrder[position++] = getTableColumnIndex(column);

        setColumnOrder(columnOrder);

		saveState();
		refresh();
	}

	public IDList getSelectedIDs() {
		int[] selectionIndices = getSelectionIndices();
		IDList items = new IDList();

		for (int i = 0; i < selectionIndices.length; ++i)
			items.add(idList.get(selectionIndices[i]));

		return items;
	}

	public ResultItem[] getSelectedItems() {
		if (manager == null)
			return NULL_SELECTION;

		int[] selectionIndices = getSelectionIndices();
		ResultItem[] items = new ResultItem[selectionIndices.length];

		for (int i = 0; i < items.length; ++i) {
			items[i] = manager.getItem(idList.get(selectionIndices[i]));
		}

		return items;
	}

	public void refresh() {
		setItemCount(idList.size());
		clearAll();
	}

	protected void initColumns() {
		visibleColumns = new ArrayList<Column>();
		loadState();
	}

	protected TableColumn getTableColumn(Column column) {
		for (TableColumn tableColumn : getColumns())
			if (tableColumn.getData(COLUMN_KEY).equals(column))
				return tableColumn;
		return null;
	}

    protected int getTableColumnIndex(Column column) {
        TableColumn[] columns = getColumns();
        for (int index = 0; index < columns.length; index++) {
            TableColumn tableColumn = columns[index];
            if (tableColumn.getData(COLUMN_KEY).equals(column))
                return index;
        }
        return -1;
    }

	protected TableColumn addColumn(Column newColumn) {
		visibleColumns.add(newColumn);
		TableColumn tableColumn = new TableColumn(this, SWT.NONE);
		tableColumn.setText(newColumn.text);
		tableColumn.setWidth(newColumn.defaultWidth);
		tableColumn.setData(COLUMN_KEY, newColumn);
		tableColumn.addSelectionListener(new SelectionAdapter() {
			@Override
            public void widgetSelected(SelectionEvent e) {
				TableColumn tableColumn = (TableColumn)e.widget;
				if (!tableColumn.isDisposed()) {
					Column column = (Column)tableColumn.getData(COLUMN_KEY);
					int sortDirection = (getSortColumn() == tableColumn && getSortDirection() == SWT.UP ? SWT.DOWN : SWT.UP);
					setSortColumn(tableColumn);
					setSortDirection(sortDirection);
					sortBy(column, sortDirection);
					refresh();
					fireContentChangedEvent();
				}
			}
		});
		tableColumn.addControlListener(new ControlAdapter() {
		    @Override
		    public void controlResized(ControlEvent e) {
		        saveState();
		    }
		});

        return tableColumn;
	}

	private void restoreSortOrder() {
		TableColumn sortColumn = getSortColumn();
		int sortDirection = getSortDirection();
		if (sortColumn != null && sortDirection != SWT.NONE) {
			Column column = (Column)sortColumn.getData(COLUMN_KEY);
			if (column != null)
				sortBy(column, sortDirection);
		}
	}

	private void sortBy(Column column, int direction) {
		if (manager == null)
			return;

		boolean ascending = direction == SWT.UP;
		if (COL_DIRECTORY.equals(column))
			idList.sortByDirectory(manager, ascending);
		else if (COL_FILE.equals(column))
			idList.sortByFileName(manager, ascending);
		else if (COL_CONFIG.equals(column))
			idList.sortByRunAttribute(manager, CONFIGNAME, ascending);
		else if (COL_RUNNUMBER.equals(column))
			idList.sortByRunAttribute(manager, RUNNUMBER, ascending);
		else if (COL_RUN_ID.equals(column))
			idList.sortByRun(manager, ascending);
		else if (COL_MODULE.equals(column))
			idList.sortByModule(manager, ascending);
		else if (COL_DATA.equals(column))
			idList.sortByName(manager, ascending);
		else if (COL_VALUE.equals(column))
			idList.sortScalarsByValue(manager, ascending);
		else if (COL_VECTOR_ID.equals(column))
			idList.sortVectorsByVectorId(manager, ascending);
		// TODO: the following 6 if branches have some code duplication due to the fact that
		// vector and histogram results do not share a common superclass that provides statistics
		else if (COL_COUNT.equals(column)) {
		    if (idList.areAllHistograms())
	            idList.sortHistogramsByLength(manager, ascending);
		    else if (idList.areAllVectors())
		        idList.sortVectorsByLength(manager, ascending);
		}
		else if (COL_MEAN.equals(column)) {
            if (idList.areAllHistograms())
                idList.sortHistogramsByMean(manager, ascending);
            else if (idList.areAllVectors())
                idList.sortVectorsByMean(manager, ascending);
		}
		else if (COL_STDDEV.equals(column)) {
            if (idList.areAllHistograms())
                idList.sortHistogramsByStdDev(manager, ascending);
            else if (idList.areAllVectors())
                idList.sortVectorsByStdDev(manager, ascending);
		}
		else if (COL_MIN.equals(column)) {
            if (idList.areAllHistograms())
                idList.sortHistogramsByMin(manager, ascending);
            else if (idList.areAllVectors())
                idList.sortVectorsByMin(manager, ascending);
		}
		else if (COL_MAX.equals(column)) {
            if (idList.areAllHistograms())
                idList.sortHistogramsByMax(manager, ascending);
            else if (idList.areAllVectors())
                idList.sortVectorsByMax(manager, ascending);
		}
        else if (COL_VARIANCE.equals(column)) {
            if (idList.areAllHistograms())
                idList.sortHistogramsByVariance(manager, ascending);
            else if (idList.areAllVectors())
                idList.sortVectorsByVariance(manager, ascending);
        }
		else if (COL_EXPERIMENT.equals(column))
			idList.sortByRunAttribute(manager, EXPERIMENT, ascending);
		else if (COL_MEASUREMENT.equals(column))
			idList.sortByRunAttribute(manager, MEASUREMENT, ascending);
		else if (COL_REPLICATION.equals(column))
			idList.sortByRunAttribute(manager, REPLICATION, ascending);
		else if (COL_MIN_TIME.equals(column))
			idList.sortVectorsByStartTime(manager, ascending);
		else if (COL_MAX_TIME.equals(column))
			idList.sortVectorsByEndTime(manager, ascending);
	}

	protected void fillTableLine(TableItem item, int lineNumber) {
		if (manager == null)
			return;

		long id = idList.get(lineNumber);
		item.setData(ITEM_KEY, id);

		for (int i = 0; i < visibleColumns.size(); ++i) {
			Column column = visibleColumns.get(i);
			String value = getCellValue(lineNumber, column);
			item.setText(i, value);
		}
	}

	protected void toCSV(CsvWriter writer, int lineNumber) {
		if (manager == null)
			return;

		for (int i = 0; i < visibleColumns.size(); ++i) {
			Column column = visibleColumns.get(i);
			writer.addField(getCellValue(lineNumber, column));
		}

		writer.endRecord();
	}

	protected String getCellValue(int row, Column column) {
		if (manager == null)
			return "";

		try {
		    //TODO: code very similar to ResultItemPropertySource -- make them common?
			long id = idList.get(row);
			ResultItem result = manager.getItem(id);

			if (COL_DIRECTORY.equals(column))
				return result.getFileRun().getFile().getDirectory();
			else if (COL_FILE.equals(column)) {
				String fileName = result.getFileRun().getFile().getFileName();
				return fileName;
			}
			else if (COL_CONFIG.equals(column)) {
				String config = result.getFileRun().getRun().getAttribute(CONFIGNAME);
				return config != null ? config : "n.a.";
			}
			else if (COL_RUNNUMBER.equals(column)) {
				String runNumber = result.getFileRun().getRun().getAttribute(RUNNUMBER);
				return runNumber != null ? runNumber : "n.a.";
			}
			else if (COL_RUN_ID.equals(column))
				return result.getFileRun().getRun().getRunName();
			else if (COL_MODULE.equals(column))
				return result.getModuleName();
			else if (COL_DATA.equals(column))
				return result.getName();
			else if (COL_EXPERIMENT.equals(column)) {
				String experiment = result.getFileRun().getRun().getAttribute(EXPERIMENT);
				return experiment != null ? experiment : "n.a.";
			}
			else if (COL_MEASUREMENT.equals(column)) {
				String measurement = result.getFileRun().getRun().getAttribute(MEASUREMENT);
				return measurement != null ? measurement : "n.a.";
			}
			else if (COL_REPLICATION.equals(column)) {
				String replication = result.getFileRun().getRun().getAttribute(REPLICATION);
				return replication != null ? replication : "n.a.";
			}
			else if (type == ResultType.SCALAR_LITERAL) {
				ScalarResult scalar = (ScalarResult)result;
				if (COL_VALUE.equals(column))
					return String.valueOf(scalar.getValue());
			}
			else if (type == ResultType.VECTOR_LITERAL) {
				VectorResult vector = (VectorResult)result;
				if (COL_VECTOR_ID.equals(column)) {
					return String.valueOf(vector.getVectorId());
				}
				else if (COL_COUNT.equals(column)) {
					int count = vector.getStatistics().getCount();
					return count >= 0 ? String.valueOf(count) : "n.a.";
				}
				else if (COL_MEAN.equals(column)) {
					double mean = vector.getStatistics().getMean();
					return Double.isNaN(mean) ? "n.a." : String.valueOf(mean);
				}
				else if (COL_STDDEV.equals(column)) {
					double stddev = vector.getStatistics().getStddev();
					return Double.isNaN(stddev) ? "n.a." : String.valueOf(stddev);
				}
                else if (COL_VARIANCE.equals(column)) {
                    double variance = vector.getStatistics().getVariance();
                    return Double.isNaN(variance) ? "n.a." : String.valueOf(variance);
                }
				else if (COL_MIN.equals(column)) {
					double min = vector.getStatistics().getMin();
					return Double.isNaN(min) ? "n.a." : String.valueOf(min);
				}
				else if (COL_MAX.equals(column)) {
					double max = vector.getStatistics().getMax();
					return Double.isNaN(max) ? "n.a." : String.valueOf(max);
				}
				else if (COL_MIN_TIME.equals(column)) {
					BigDecimal minTime = vector.getStartTime();
					return minTime == null || minTime.isNaN() ? "n.a." : String.valueOf(minTime);
				}
				else if (COL_MAX_TIME.equals(column)) {
					BigDecimal maxTime = vector.getEndTime();
					return maxTime == null || maxTime.isNaN() ? "n.a." : String.valueOf(maxTime);
				}
			}
			else if (type == ResultType.HISTOGRAM_LITERAL) {
				HistogramResult histogram = (HistogramResult)result;
				if (COL_COUNT.equals(column)) {
					int count = histogram.getStatistics().getCount();
					return count >= 0 ? String.valueOf(count) : "n.a.";
				}
				else if (COL_MEAN.equals(column)) {
					double mean = histogram.getStatistics().getMean();
					return Double.isNaN(mean) ? "n.a." : String.valueOf(mean);
				}
				else if (COL_STDDEV.equals(column)) {
					double stddev = histogram.getStatistics().getStddev();
					return Double.isNaN(stddev) ? "n.a." : String.valueOf(stddev);
				}
                else if (COL_VARIANCE.equals(column)) {
                    double variance = histogram.getStatistics().getVariance();
                    return Double.isNaN(variance) ? "n.a." : String.valueOf(variance);
                }
				else if (COL_MIN.equals(column)) {
					double min = histogram.getStatistics().getMin();
					return Double.isNaN(min) ? "n.a." : String.valueOf(min);
				}
				else if (COL_MAX.equals(column)) {
					double max = histogram.getStatistics().getMax();
					return Double.isNaN(max) ? "n.a." : String.valueOf(max);
				}
			}
		}
		catch (RuntimeException e) {
			// stale ID?
			return "";
		}

		return "";
	}

	public void copySelectionToClipboard() {
		CsvWriter writer = new CsvWriter('\t');
		// add header
		for (Column column : visibleColumns)
			writer.addField(column.text);
		writer.endRecord();
		// add selected lines
		int[] selection = getSelectionIndices();
		for (int i = 0; i < selection.length; ++i)
			toCSV(writer, selection[i]);

		Clipboard clipboard = new Clipboard(getDisplay());
		clipboard.setContents(new Object[] {writer.toString()}, new Transfer[] {TextTransfer.getInstance()});
		clipboard.dispose();
	}

	public void addDataListener(IDataListener listener) {
		if (listeners == null)
			listeners = new ListenerList();
		listeners.add(listener);
	}

	public void removeDataListener(IDataListener listener) {
		if (listeners != null)
			listeners.remove(listener);
	}

	protected void fireContentChangedEvent() {
		if (listeners != null) {
			for (Object listener : new ArrayList<Object>(Arrays.asList(this.listeners.getListeners())))
				((IDataListener)listener).contentChanged(this);
		}
	}

	/*
	 * Save/load state
	 */

	protected String getPreferenceStoreKey(Column column, String field) {
		return "DataTable." + type + "." + column.text + "." + field;
	}

	protected void initDefaultState() {
		if (preferences != null) {
			for (Column column : getAllColumns()) {
				preferences.setDefault(getPreferenceStoreKey(column, "visible"), column.defaultVisible);
                preferences.setDefault(getPreferenceStoreKey(column, "width"), column.defaultWidth);
			}
		}
	}

	protected void loadState() {
		if (preferences != null) {
			visibleColumns.clear();
			for (Column column : getAllColumns()) {
				boolean visible = preferences.getBoolean(getPreferenceStoreKey(column, "visible"));
				if (visible) {
                    Column clone = column.clone();
                    clone.defaultWidth = preferences.getInt(getPreferenceStoreKey(column, "width"));
					addColumn(clone);
				}
			}
		}
	}

	protected void saveState() {
		if (preferences != null) {
			for (Column column : getAllColumns()) {
				boolean visible = visibleColumns.indexOf(column) >= 0;
				preferences.setValue(getPreferenceStoreKey(column, "visible"), visible);
				if (visible)
                    preferences.setValue(getPreferenceStoreKey(column, "width"), getTableColumn(column).getWidth());
			}
		}
	}

	/*
	 * Select cells.
	 */
	void handleMouseDown(MouseEvent event) {
		if (isDisposed() || !isVisible()) return;
		Point pt = new Point(event.x, event.y);
		int lineWidth = getLinesVisible() ? getGridLineWidth() : 0;
		TableItem item = getItem(pt);
		if ((getStyle() & SWT.FULL_SELECTION) != 0) {
			if (item == null) return;
		}
		else {
			int start = item != null ? indexOf(item) : getTopIndex();
			int end = getItemCount();
			Rectangle clientRect = getClientArea();
			for (int i = start; i < end; i++) {
				TableItem nextItem = getItem(i);
				Rectangle rect = nextItem.getBounds(0);
				if (pt.y >= rect.y && pt.y < rect.y + rect.height + lineWidth) {
					item = nextItem;
					break;
				}
				if (rect.y > clientRect.y + clientRect.height) 	return;
			}
			if (item == null) return;
		}
		TableColumn newColumn = null;
		int columnCount = getColumnCount();
		if (columnCount > 0) {
			for (int i = 0; i < columnCount; i++) {
				Rectangle rect = item.getBounds(i);
				rect.width += lineWidth;
				rect.height += lineWidth;
				if (rect.contains(pt)) {
					newColumn = getColumn(i);
					break;
				}
			}
			if (newColumn == null) {
				newColumn = getColumn(0);
			}
		}
		setSelectedCell(item, newColumn);
	}

	private void setSelectedCell(TableItem item, TableColumn column) {
		selectedItem = item;
		selectedColumn = column;
	}

	public String getSelectedField() {
		if (selectedColumn != null && !selectedColumn.isDisposed()) {
			Column column = (Column)selectedColumn.getData(COLUMN_KEY);
			if (column != null)
				return column.fieldName;
		}
		return null;
	}

	public ResultItem getSelectedItem() {
		if (selectedItem != null && !selectedItem.isDisposed()) {
			long id = (Long)selectedItem.getData(ITEM_KEY);
			if (!manager.isStaleID(id))
				return manager.getItem(id);
		}
		return null;
	}

	public void setSelectedID(long id) {
		int index = idList.indexOf(id);
		if (index != -1)
			setSelection(index);
	}

	public void setSelectedIDs(IDList selectedIDList) {
	    ArrayList<Integer> indicesList = new ArrayList<Integer>();
	    for (int i = 0; i < selectedIDList.size(); i++) {
	        int index = idList.indexOf(selectedIDList.get(i));
	        if (index != -1)
	            indicesList.add(index);
	    }
	    int[] indices = new int[indicesList.size()];
	    for (int i = 0; i < indices.length; i++)
	        indices[i] = indicesList.get(i);
	    setSelection(indices);
	}
}
