package org.omnetpp.scave.actions;

import static org.omnetpp.scave.model2.RunAttribute.EXPERIMENT;
import static org.omnetpp.scave.model2.RunAttribute.MEASUREMENT;
import static org.omnetpp.scave.model2.RunAttribute.REPLICATION;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.omnetpp.scave.editors.ScaveEditor;
import org.omnetpp.scave.editors.treeproviders.GenericTreeNode;
import org.omnetpp.scave.editors.ui.BrowseDataPage;
import org.omnetpp.scave.engine.ResultFile;
import org.omnetpp.scave.engine.Run;
import org.omnetpp.scave.model.InputFile;
import org.omnetpp.scave.model2.Filter;
import org.omnetpp.scave.model2.FilterUtil;
import org.omnetpp.scave.model2.RunAttribute;

/**
 * Sets the filters on the "Browse data" page with the attributes of the
 * selected object.
 *
 * @author tomi
 */
public class SetFilterAction extends AbstractScaveAction {

	public SetFilterAction() {
		setText("Set filter");
		setToolTipText("Set the filter on the \"Browse data\" page from the current selection.");
	}

	@Override
	protected void doRun(ScaveEditor scaveEditor, IStructuredSelection selection) {
		Object selected = selection.getFirstElement();
		FilterUtil filterUtil = getFilterParams(selected);
		if (filterUtil != null) {
			Filter filter = new Filter(filterUtil.getFilterPattern());
			BrowseDataPage page = scaveEditor.getBrowseDataPage();
			page.getScalarsPanel().setFilterParams(filter);
			page.getVectorsPanel().setFilterParams(filter);
			page.getHistogramsPanel().setFilterParams(filter);
			scaveEditor.showPage(page);
		}
	}

	@Override
	protected boolean isApplicable(ScaveEditor editor, IStructuredSelection selection) {
		return true;
	}

	/**
	 * Returns a filter object with the attributes of the selected object and their ancestors.
	 * Returns non-null if the filter is to be applied on the "Browse data" page.
	 */
	protected FilterUtil getFilterParams(Object object) {

		if (object instanceof InputFile) {
			FilterUtil filterUtil = new FilterUtil();
			InputFile inputFile = (InputFile)object;
			filterUtil.setField(FilterUtil.FIELD_FILENAME, inputFile.getName());
			return filterUtil;
		}
		else if (object instanceof GenericTreeNode) {
			// GenericTreeNode trees are used on the Inputs page.
			GenericTreeNode node = (GenericTreeNode)object;
			Object payload = node.getPayload();

			// get params up to the root
			FilterUtil filterUtil = getFilterParams(node.getParent());
			if (filterUtil==null) 
				filterUtil = new FilterUtil();

			if (payload instanceof ResultFile) {
				ResultFile resultFile = (ResultFile)payload;
				filterUtil.setField(FilterUtil.FIELD_FILENAME, resultFile.getFilePath());
			}
			else if (payload instanceof Run) {
				Run run = (Run)payload;
				filterUtil.setField(FilterUtil.FIELD_RUNNAME, run.getRunName());
			}
			else if (payload instanceof RunAttribute) {
				RunAttribute attr = (RunAttribute)payload;
				String name = attr.getName();
				String value = attr.getValue();
				if (EXPERIMENT.equals(name))
					filterUtil.setField(FilterUtil.FIELD_EXPERIMENT, value);
				else if (MEASUREMENT.equals(name))
					filterUtil.setField(FilterUtil.FIELD_MEASUREMENT, value);
				else if (REPLICATION.equals(name))
					filterUtil.setField(FilterUtil.FIELD_REPLICATION, value);
			}
			return filterUtil;
		}

		return null;
	}
}
