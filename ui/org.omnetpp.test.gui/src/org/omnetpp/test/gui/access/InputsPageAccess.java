/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.
  
  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.test.gui.access;

import static org.omnetpp.scave.TestSupport.FILE_RUN_VIEW_TREE_ID;
import static org.omnetpp.scave.TestSupport.INPUT_FILES_TREE;
import static org.omnetpp.scave.TestSupport.LOGICAL_VIEW_TREE_ID;
import static org.omnetpp.scave.TestSupport.RUN_FILE_VIEW_TREE_ID;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.omnetpp.common.util.Predicate;

import com.simulcraft.test.gui.access.Access;
import com.simulcraft.test.gui.access.CTabItemAccess;
import com.simulcraft.test.gui.access.CompositeAccess;
import com.simulcraft.test.gui.access.ShellAccess;
import com.simulcraft.test.gui.access.TreeAccess;
import com.simulcraft.test.gui.core.UIStep;

public class InputsPageAccess extends CompositeAccess {

	public InputsPageAccess(Composite control) {
		super(control);
	}
	
	public TreeAccess getInputFilesViewTree() {
		return (TreeAccess)createAccess(
					findDescendantControl(
							getControl(),
							Predicate.hasID(INPUT_FILES_TREE)));
	}
	
	public TreeAccess getFileRunViewTree() {
		return (TreeAccess)createAccess(
				findDescendantControl(
						getControl(),
						Predicate.hasID(FILE_RUN_VIEW_TREE_ID)));
	}
	
	public TreeAccess getRunFileViewTree() {
		return (TreeAccess)createAccess(
				findDescendantControl(
						getControl(),
						Predicate.hasID(RUN_FILE_VIEW_TREE_ID)));
	}

	public TreeAccess getLogicalViewTree() {
		return (TreeAccess)createAccess(
				findDescendantControl(
						getControl(),
						Predicate.hasID(LOGICAL_VIEW_TREE_ID)));
	}
	
	@UIStep
	public CTabItem findTab(String label) {
		return Access.findDescendantCTabItemByLabel(getControl(), label);
	}
	
	@UIStep
	public boolean isTabSelected(String label) {
		CTabItem item = findTab(label);
		return item.getParent().getSelection() == item;
	}
	
	public void selectTab(String label) {
		CTabItemAccess item = (CTabItemAccess)createAccess(findTab(label));
		item.click();
	}
	
	public void ensureTabSelected(String label) {
		if (!isTabSelected(label))
			selectTab(label);
	}
	
	public TreeAccess ensureFileRunViewVisible() {
		ensureTabSelected(".*file.*run.*");
		return getFileRunViewTree();
	}
	
	public TreeAccess ensureRunFileViewVisible() {
		ensureTabSelected(".*run.*file.*");
		return getRunFileViewTree();
	}
	
	public TreeAccess ensureLogicalViewVisible() {
		ensureTabSelected(".*[lL]ogical.*");
		return getLogicalViewTree();
	}
	
	public InputsPageAccess addFileWithWildcard(String wildcard) {
        findButtonWithLabel("Wildcard.*").selectWithMouseClick();
        ShellAccess dialog = Access.findShellWithTitle("Add files with wildcard"); 
        dialog.findTextAfterLabel("Enter the file name.*").typeOver(wildcard);
        dialog.pressKey(SWT.CR);
        return this;
	}
	
	public InputsPageAccess removeInputFile(String pattern) {
    	TreeAccess inputFilesTree = getInputFilesViewTree();
        inputFilesTree.findTreeItemByContent("file " + pattern).click();
        inputFilesTree.pressKey(SWT.DEL);
        return this;
	}
}
