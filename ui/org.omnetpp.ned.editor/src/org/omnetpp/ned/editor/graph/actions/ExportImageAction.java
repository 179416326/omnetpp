/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.editor.graph.actions;

import org.eclipse.gef.requests.GroupRequest;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchPart;
import org.omnetpp.common.image.ImageFactory;
import org.omnetpp.ned.editor.graph.parts.CompoundModuleEditPart;

import de.unikassel.imageexport.wizards.ExportImageOfCurrentDiagramWizard;

/**
 * TODO add documentation
 *
 * @author rhornig
 */
public class ExportImageAction extends org.eclipse.gef.ui.actions.SelectionAction {

	public static final String ID = "ExportImage";
	public static final String MENUNAME = "Export Image...";
	public static final String TOOLTIP = "Export an image of the selected modules";
	public static final ImageDescriptor IMAGE = ImageFactory.getDescriptor(ImageFactory.TOOLBAR_IMAGE_EXPORTIMAGE);

	GroupRequest request;

	public ExportImageAction(IWorkbenchPart part) {
		super(part);
		setText(MENUNAME);
		setId(ID);
		setToolTipText(TOOLTIP);
		setImageDescriptor(IMAGE);
		setHoverImageDescriptor(IMAGE);
	}

	@Override
	protected boolean calculateEnabled() {
	    if (getSelectedObjects().size() == 0)
	        return false;
	    // check if any compound module present in the selection
	    boolean result = false;
	    for (Object ob : getSelectedObjects())
	        if (ob instanceof CompoundModuleEditPart)
	            result = true;

	    return result;
	}

	@Override
	public void run() {
	    ExportImageOfCurrentDiagramWizard wizard = new ExportImageOfCurrentDiagramWizard();
        wizard.init(getWorkbenchPart().getSite().getWorkbenchWindow().getWorkbench(), (IStructuredSelection)getSelection());
        WizardDialog dialog = new WizardDialog(getWorkbenchPart().getSite().getShell(), wizard);
        dialog.open();
	}

}
