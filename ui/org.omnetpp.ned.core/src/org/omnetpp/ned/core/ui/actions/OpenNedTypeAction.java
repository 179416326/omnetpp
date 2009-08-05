/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.
  
  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.core.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.ListDialog;
import org.omnetpp.common.util.UIUtils;
import org.omnetpp.ned.core.NEDResources;
import org.omnetpp.ned.core.NEDResourcesPlugin;
import org.omnetpp.ned.model.NEDTreeUtil;
import org.omnetpp.ned.model.interfaces.INEDTypeInfo;

/**
 * Action to open a NED type selection dialog.
 * 
 * @author andras, rhornig
 */
public class OpenNedTypeAction implements IWorkbenchWindowActionDelegate {
    public void init(IWorkbenchWindow window) {
    }

    public void dispose() {
    }
    
    public void run(IAction action) {
        NEDResources ned = NEDResourcesPlugin.getNEDResources();
        
        // pop up a chooser dialog
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        ElementListSelectionDialog dialog = new ElementListSelectionDialog(window.getShell(), new LabelProvider() {
            @Override
            public Image getImage(Object element) {
                INEDTypeInfo nedType = (INEDTypeInfo) element;
                return NEDTreeUtil.getNedModelLabelProvider().getImage(nedType.getNEDElement());
            }

            @Override
            public String getText(Object element) {
                INEDTypeInfo nedType = (INEDTypeInfo) element;
                String simpleName = nedType.getName();
                String fullyQualifiedName = nedType.getFullyQualifiedName();
                String typeName = nedType.getNEDElement().getReadableTagName();
                if (simpleName.equals(fullyQualifiedName))
                	return nedType.getName() + " -- " + typeName;
                else
                	return nedType.getName() + " -- " + typeName + " (" + fullyQualifiedName + ")";
            }
            
        }); 
        dialog.setElements(ned.getNedTypesFromAllProjects().toArray());
        dialog.setMessage("Select NED type to open:");
        dialog.setTitle("Open NED Type");
        dialog.setDialogBoundsSettings(UIUtils.getDialogSettings(NEDResourcesPlugin.getDefault(), "OpenNEDTypeDialog"), Dialog.DIALOG_PERSISTSIZE + Dialog.DIALOG_PERSISTLOCATION);
        if (dialog.open() == ListDialog.OK) {
            INEDTypeInfo component = (INEDTypeInfo) dialog.getResult()[0];
            NEDResourcesPlugin.openNEDElementInEditor(component.getNEDElement());
        }
    }

    public void selectionChanged(IAction action, ISelection selection) {
    }

}
