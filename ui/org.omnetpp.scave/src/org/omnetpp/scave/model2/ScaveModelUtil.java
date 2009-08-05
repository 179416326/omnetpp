package org.omnetpp.scave.model2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.omnetpp.scave.engine.HistogramResult;
import org.omnetpp.scave.engine.IDList;
import org.omnetpp.scave.engine.ResultFileManager;
import org.omnetpp.scave.engine.ResultItem;
import org.omnetpp.scave.engine.ScalarResult;
import org.omnetpp.scave.engine.VectorResult;
import org.omnetpp.scave.model.Add;
import org.omnetpp.scave.model.Analysis;
import org.omnetpp.scave.model.BarChart;
import org.omnetpp.scave.model.Chart;
import org.omnetpp.scave.model.ChartSheet;
import org.omnetpp.scave.model.ChartSheets;
import org.omnetpp.scave.model.Dataset;
import org.omnetpp.scave.model.Group;
import org.omnetpp.scave.model.HistogramChart;
import org.omnetpp.scave.model.LineChart;
import org.omnetpp.scave.model.Property;
import org.omnetpp.scave.model.ResultType;
import org.omnetpp.scave.model.ScaveModelFactory;
import org.omnetpp.scave.model.ScaveModelPackage;

/**
 * A collection of static methods to manipulate model objects
 * @author andras
 */
public class ScaveModelUtil {

	private static final String DEFAULT_CHARTSHEET_NAME = "default";

	private static final ScaveModelFactory factory = ScaveModelFactory.eINSTANCE;
	private static final ScaveModelPackage pkg = ScaveModelPackage.eINSTANCE;

	public static ChartSheet createDefaultChartSheet() {
		ChartSheet chartsheet = factory.createChartSheet();
		chartsheet.setName(DEFAULT_CHARTSHEET_NAME);
		return chartsheet;
	}

	public static Dataset createDataset(String name) {
		Dataset dataset = factory.createDataset();
		dataset.setName(name);
		return dataset;
	}

	public static Dataset createDataset(String name, Filter filter, ResultType type) {
		Dataset dataset = factory.createDataset();
		dataset.setName(name);
		dataset.getItems().add(createAdd(filter, type));
		return dataset;
	}

	public static Dataset createDataset(String name, ResultItem[] items, String[] runidFields) {
		Dataset dataset = factory.createDataset();
		dataset.setName(name);
		dataset.getItems().addAll(createAdds(items, runidFields));
		return dataset;
	}

	public static Chart createChart(String name, ResultType type) {
		if (type==ResultType.SCALAR_LITERAL)
			return createBarChart(name);
		else if (type==ResultType.VECTOR_LITERAL)
			return createLineChart(name);
		else if (type==ResultType.HISTOGRAM_LITERAL)
			return createHistogramChart(name);
		else
			throw new IllegalArgumentException();
	}

	public static BarChart createBarChart(String name) {
		BarChart chart = factory.createBarChart();
		chart.setName(name);
		return chart;
	}

	public static LineChart createLineChart(String name) {
		LineChart chart = factory.createLineChart();
		chart.setName(name);
		return chart;
	}

	public static HistogramChart createHistogramChart(String name) {
		HistogramChart chart = factory.createHistogramChart();
		chart.setName(name);
		return chart;
	}
	
	public static Add createAdd(String filterString, ResultType type) {
		Add add = factory.createAdd();
		add.setFilterPattern(filterString);
		add.setType(type);
		return add;
	}

	public static Add createAdd(Filter filter, ResultType type) {
		return createAdd(filter.getFilterPattern(), type);
	}

	/**
	 * Generates Add commands with filter patterns that identify elements in items[].
	 * @param runidFields  may be null (meaning autoselect)
	 */
	public static Collection<Add> createAdds(ResultItem[] items, String[] runidFields) {
		List<Add> adds = new ArrayList<Add>(items.length);
		for (ResultItem item : items)
			adds.add(createAdd(item, runidFields));
		return adds;
	}

	/**
	 * Generates an Add command with filter pattern to identify item.
	 * @param runidFields  may be null (meaning autoselect)
	 */
	public static Add createAdd(ResultItem item, String[] runidFields) {
		Add add = factory.createAdd();
		add.setFilterPattern(new FilterUtil(item, runidFields).getFilterPattern());
		if (item instanceof ScalarResult)
			add.setType(ResultType.SCALAR_LITERAL);
		else if (item instanceof VectorResult)
			add.setType(ResultType.VECTOR_LITERAL);
		else if (item instanceof HistogramResult)
			add.setType(ResultType.HISTOGRAM_LITERAL);
		else
			throw new RuntimeException("unknown result type");
		return add;
	}

	/**
	 * Returns the analysis node of the specified resource.
	 * It is assumed that the resource contains exactly one analysis node as
	 * content.
	 */
	public static Analysis getAnalysis(Resource resource) {
		Assert.isTrue(resource.getContents().size() == 1 && resource.getContents().get(0) instanceof Analysis,
				"Analysis node not found in: " + resource.getURI().toString());
		return (Analysis)resource.getContents().get(0);
	}

	/**
	 * Returns the analysis node containing <code>eobject</code>.
	 */
	public static Analysis getAnalysis(EObject eobject) {
		Assert.isTrue(eobject.eClass().getEPackage() == pkg,
				"Scave model object expected, received: " + eobject.toString());
		return getAnalysis(eobject.eResource());
	}

	public static ChartSheet getDefaultChartSheet(Resource resource) {
		Analysis analysis = getAnalysis(resource);
		for (ChartSheet chartsheet : (List<ChartSheet>)analysis.getChartSheets().getChartSheets())
			if (DEFAULT_CHARTSHEET_NAME.equals(chartsheet.getName()))
				return chartsheet;
		return null;
	}

	public static Dataset findEnclosingDataset(Chart chart) {
		EObject parent = chart.eContainer();
		while (parent != null && !(parent instanceof Dataset))
			parent = parent.eContainer();
		return (Dataset)parent;
	}

	/**
	 * Returns the datasets in the resource.
	 */
	public static List<Dataset> findDatasets(Resource resource) {
		List<Dataset> result = new ArrayList<Dataset>();
		Analysis analysis = getAnalysis(resource);
		if (analysis.getDatasets() != null) {
			for (Object object : analysis.getDatasets().getDatasets()) {
				Dataset dataset = (Dataset)object;
				result.add(dataset);
			}
		}
		return result;
	}

	public static <T extends EObject> T findEnclosingObject(EObject object, Class<T> type) {
		while (object != null && !type.isInstance(object))
			object = object.eContainer();
		return (T)object;
	}

	/**
	 * Returns all object in the container having the specified type.
	 */
	public static <T extends EObject> List<T> findObjects(EObject container, Class<T> type) {
		ArrayList<T> objects = new ArrayList<T>();
		for (TreeIterator iterator = container.eAllContents(); iterator.hasNext(); ) {
			Object object = iterator.next();
			if (type.isInstance(object))
				objects.add((T)object);
		}
		return objects;
 	}

	/**
	 * Returns all objects in the resource having the specified type.
	 */
	public static <T extends EObject> List<T> findObjects(Resource resource, Class<T> type) {
		ArrayList<T> objects = new ArrayList<T>();
		for (TreeIterator iterator = resource.getAllContents(); iterator.hasNext(); ) {
			Object object = iterator.next();
			if (type.isInstance(object))
				objects.add((T)object);
		}
		return objects;
	}

	/**
	 * Collect charts from the given collection.
	 */
	public static List<Chart> collectCharts(Collection items) {
		List<Chart> charts = new ArrayList<Chart>();
		for (Object item : items)
			if (item instanceof Chart) {
				charts.add((Chart)item);
			}
			else if (item instanceof Dataset || item instanceof Group) {
				for (TreeIterator iter = ((EObject)item).eAllContents(); iter.hasNext(); ) {
					Object object = iter.next();
					if (object instanceof Chart)
						charts.add((Chart)object);
					else if (!(object instanceof Dataset || object instanceof Group))
						iter.prune();
				}
			}
		return charts;
	}

	/**
	 * Collect unreferenced charts from the given collection.
	 */
	public static Collection<Chart> collectUnreferencedCharts(Collection items) {
		List<Chart> charts = collectCharts(items);
		if (charts.size() > 0) {
			Map references = ScaveCrossReferencer.find(charts.get(0).eResource());
			charts.removeAll(references.keySet());
		}
		return charts;
	}

	/**
	 * Collect references to scave objects.
	 * Currently the only references are from chart sheets to charts,
	 * so the scope of the search is limited to chart sheets.
	 */
	static class ScaveCrossReferencer extends EcoreUtil.CrossReferencer {

		protected ScaveCrossReferencer(Collection eobjects) {
			super(eobjects);
		}

		public static Map find(Resource resource) {
			return EcoreUtil.CrossReferencer.find(Collections.singleton(resource));
		}

		@Override
		protected boolean containment(EObject eObject) {
			return eObject instanceof Resource ||
				   eObject instanceof Analysis ||
				   eObject instanceof ChartSheets ||
				   eObject instanceof ChartSheet;
		}
	}

	public static Property getChartProperty(Chart chart, String propertyName) {
		for (Object object : chart.getProperties()) {
			Property property = (Property)object;
			if (property.getName().equals(propertyName))
				return property;
		}
		return null;
	}

	public static void setChartProperty(EditingDomain ed, Chart chart, String propertyName, String propertyValue) {
		Property property = getChartProperty(chart, propertyName);
		Command command;
		if (property == null) {
			property = factory.createProperty();
			property.setName(propertyName);
			property.setValue(propertyValue);
			command = AddCommand.create(
						ed,
						chart,
						pkg.getChart_Properties(),
						property);
		}
		else {
			command = SetCommand.create(
						ed,
						property,
						pkg.getProperty_Value(),
						propertyValue);
		}
		ed.getCommandStack().execute(command);
	}

	public static IDList getAllIDs(ResultFileManager manager, ResultType type) {
		switch (type.getValue()) {
		case ResultType.SCALAR: return manager.getAllScalars();
		case ResultType.VECTOR:	return manager.getAllVectors();
		case ResultType.HISTOGRAM: return manager.getAllHistograms();
		}
		Assert.isTrue(false, "Unknown dataset type: " + type);
		return null;
	}

	public static ResultItem[] getResultItems(IDList idlist, ResultFileManager manager) {
		int size = (int)idlist.size();
		ResultItem[] items = new ResultItem[size];
		for (int i = 0; i < size; ++i)
			items[i] = manager.getItem(idlist.get(i));
		return items;
	}

	public static IDList filterIDList(IDList idlist, Filter filter, ResultFileManager manager) {
		Assert.isTrue(filter.getFilterPattern()!=null);
		return manager.filterIDList(idlist, filter.getFilterPattern());
	}

	/**
	 * Returns the default chart sheet.
	 * When the resource did not contain default chart sheet a new one is created,
	 * and a AddCommand is appended to the <code>command</command>, that adds
	 * the new chart sheet to the resource.
	 */
	public static ChartSheet getOrCreateDefaultChartSheet(EditingDomain ed, CompoundCommand command, Resource resource) {
		ChartSheet chartsheet = getDefaultChartSheet(resource);
		if (chartsheet == null) {
			chartsheet = createDefaultChartSheet();
			command.append(
				AddCommand.create(
					ed,
					getAnalysis(resource).getChartSheets(),
					pkg.getChartSheets_ChartSheets(),
					chartsheet,
					0));
		}
		return chartsheet;
	}

	public static void dumpIDList(String header, IDList idlist, ResultFileManager manager) {
		System.out.print(header);
		if (idlist.size() == 0)
			System.out.println("Empty");
		else {
			System.out.println();
			for (int i = 0; i < idlist.size(); ++i) {
				ResultItem r = manager.getItem(idlist.get(i));
				System.out.println(
					String.format("File: %s Run: %s Module: %s Name: %s",
						r.getFileRun().getFile().getFilePath(),
						r.getFileRun().getRun().getRunName(),
						r.getModuleName(),
						r.getName()));
			}
		}
	}
}
