/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;

import org.omnetpp.scave.editors.DatasetEditor;


public class CutAction extends Action {

    private final IWorkbenchWindow window;

    public CutAction(IWorkbenchWindow window) {
        this.window = window;
    }

    public void run() {
        IEditorPart editor = window.getActivePage().getActiveEditor();
        if (editor instanceof DatasetEditor) {
            DatasetEditor dsEditor = (DatasetEditor)editor;
            dsEditor.getFilterPanel().doCut();
        }
    }
}
