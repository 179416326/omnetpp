package org.omnetpp.scave.editors.datatable;

import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.swt.widgets.Control;

/**
 * Content adapter that can replace a text segment in the control with the proposal.
 */
public interface IControlContentAdapter2 extends IControlContentAdapter {

	void replaceControlContents(Control control, int startPos, int endPos, String text, int cursorPos);
}
