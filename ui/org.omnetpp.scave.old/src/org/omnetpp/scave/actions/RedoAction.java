package org.omnetpp.scave.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;

import org.omnetpp.scave.editors.DatasetEditor;


public class RedoAction extends Action {

	private final IWorkbenchWindow window;

	public RedoAction(IWorkbenchWindow window) {
		this.window = window;
	}

	public void run() {
		IEditorPart editor = window.getActivePage().getActiveEditor();
		if (editor instanceof DatasetEditor) {
			//DatasetEditor dsEditor = (DatasetEditor)editor;

			// do something here...
			MessageDialog.openInformation(window.getShell(),
					"OMNEST/OMNeT++ Scave Plug-in",
					"This action is not implemented yet.");
		}
	}
}
