/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.
  
  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.common.properties;

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.omnetpp.common.contentassist.ContentProposalProvider;
import org.omnetpp.common.image.ImageFactory;
import org.omnetpp.common.image.ImageSelectionDialog;

public class ImageCellEditor extends TextCellEditorEx {

    public class ImageContentProposalProvider extends ContentProposalProvider {
        public ImageContentProposalProvider() {
            super(true);
        }

        @Override
        protected List<IContentProposal> getProposalCandidates(String prefix) {
            if (prefix.contains("/")) 
                return sort(toProposals(ImageFactory.getImageNameList().toArray(new String[]{})));
            else 
                return sort(toProposals(ImageFactory.getCategories().toArray(new String[]{})));
        }
        
    }
    
    public ImageCellEditor() {
		super();
	}

	public ImageCellEditor(Composite parent, int style) {
		super(parent, style);
	}

	protected ImageCellEditor(Composite parent) {
		super(parent);
	}

	@Override
	protected Control createControl(Composite parent) {
	    Control result = super.createControl(parent);
        IContentProposalProvider proposalProvider = new ImageContentProposalProvider();
        new ContentAssistCommandAdapter(text, new TextContentAdapter(), proposalProvider, 
                ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, "/".toCharArray(), true);

        return result;
	}

	@Override
	protected Object openDialogBox(Control cellEditorWindow) {
		ImageSelectionDialog cellDialog = 
			new ImageSelectionDialog(cellEditorWindow.getShell(), (String)getValue());

		if (cellDialog.open() == Dialog.OK) 
		    return cellDialog.getImageId();

		// dialog cancelled
		return null;
	}

}
