package org.omnetpp.scave.charting;

import java.util.List;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.omnetpp.common.util.DelayedJob;
import org.omnetpp.scave.engine.ResultFileManager;
import org.omnetpp.scave.model.Chart;
import org.omnetpp.scave.model.Dataset;
import org.omnetpp.scave.model.InputFile;
import org.omnetpp.scave.model.Inputs;
import org.omnetpp.scave.model.Property;
import org.omnetpp.scave.model.ScaveModelPackage;
import org.omnetpp.scave.model2.ScaveModelUtil;

/**
 * This class listens on changes in the model, and refreshes the chart accordingly.
 * Currently it only listens on chart property changes. 
 * 
 * @author tomi
 */
public class ChartUpdater {
	private static final int CHART_UPDATE_DELAY_MS = 200;

	private Chart chart;
	private ChartCanvas view;
	private ResultFileManager manager;
	private DelayedJob startUpdateJob = new DelayedJob(CHART_UPDATE_DELAY_MS) {
		public void run() {
			updateDataset();
		}
	};
	
	public ChartUpdater(Chart chart, ChartCanvas view, ResultFileManager manager) {
		this.chart = chart;
		this.view = view;
		this.manager = manager;
	}
	
	public ResultFileManager getResultFileManager() {
		return manager;
	}

	/**
	 * Propagate changes on the "Chart" model object to the chart view. 
	 */
	@SuppressWarnings("unchecked")
	public void updateChart(Notification notification) {
		if (notification.isTouch() || !(notification.getNotifier() instanceof EObject))
			return;
		EObject notifier = (EObject)notification.getNotifier();
		if (notifier.eResource() != chart.eResource())
			return;
		
		// add/remove chart property 
		if (notifier instanceof Chart) {
			switch (notification.getFeatureID(Chart.class)) {
			case ScaveModelPackage.CHART__PROPERTIES:
				Property property;
				switch (notification.getEventType()) {
				case Notification.ADD:
					property = (Property)notification.getNewValue();
					setChartProperty(property.getName(), property.getValue());
					break;
				case Notification.REMOVE:
					property = (Property)notification.getOldValue();
					setChartProperty(property.getName(), null);
					break;
				case Notification.ADD_MANY:
					for (Property prop : (List<Property>)notification.getNewValue()) {
						setChartProperty(prop.getName(), prop.getValue());
					}
					break;
				case Notification.REMOVE_MANY:
					for (Property prop : (List<Property>)notification.getOldValue()) {
						setChartProperty(prop.getName(), null);
					}
					break;
				}
				break;
			}
		}
		// change chart property
		else if (notifier instanceof Property) {
			switch (notification.getFeatureID(Property.class)) {
			case ScaveModelPackage.PROPERTY__VALUE:
				Property property = (Property)notification.getNotifier();
				setChartProperty(property.getName(), (String)notification.getNewValue());
				break;
			}
		}
		// add/remove input file
		else if (notifier instanceof Inputs) {
			// TODO should be checked that visible items are affected
			scheduleDatasetUpdate();
		}
		// change input file
		else if (notifier instanceof InputFile) {
			// TODO should be checked that chart is affected
			scheduleDatasetUpdate();
		}
		else if (notification.getFeature() != null &&
				notification.getFeature() instanceof EStructuralFeature &&
				"name".equals(((EStructuralFeature)notification.getFeature()).getName())) {
			// ignore name changes
		}
		// add/remove change in the dataset of the chart or in its base dataset
		else {
			Dataset changedDataset = ScaveModelUtil.findEnclosingOrSelf(notifier, Dataset.class);
			if (changedDataset == null)
				return;
			
			Dataset chartDataset = ScaveModelUtil.findEnclosingDataset(chart);
			while (chartDataset != null && chartDataset != changedDataset)
				chartDataset = chartDataset.getBasedOn();

			if (chartDataset == changedDataset) {
				scheduleDatasetUpdate();
			}
		}
	}

	private void setChartProperty(String name, String value) {
		if (!view.isDisposed())
			view.setProperty(name, value);
	}
	
	private void scheduleDatasetUpdate() {
		startUpdateJob.restartTimer();
	}

	/**
	 * Starts a job that recalculates chart contents.
	 */
	public void updateDataset() {
		ChartFactory.populateChart(view, chart, ChartUpdater.this.manager);
	}
}
