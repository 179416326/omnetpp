package org.omnetpp.scave.actions;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.omnetpp.scave.editors.ScaveEditor;
import org.omnetpp.scave.editors.ui.ChartPage;

/**
 * Copy chart contents to the clipboard.
 */
public class CopyChartToClipboardAction extends AbstractScaveAction {
	public CopyChartToClipboardAction() {
		setText("Copy to clipboard");
		setToolTipText("Copy chart to clipboard");
	}

	@Override
	protected void doRun(ScaveEditor editor, IStructuredSelection selection) {
		final ChartPage page = (ChartPage)editor.getActiveEditorPage();
		BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
			public void run() {
				page.getChartView().copyToClipboard();
			}
		});
	}

	@Override
	protected boolean isApplicable(ScaveEditor editor, IStructuredSelection selection) {
		System.out.println("********* ActivePAge:"+editor.getActiveEditorPage());
		return editor.getActiveEditorPage() instanceof ChartPage;
	}
}
