/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.
  
  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package com.simulcraft.test.gui.access;

import junit.framework.Assert;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.simulcraft.test.gui.core.UIStep;
import com.simulcraft.test.gui.core.InBackgroundThread;

public class ShellAccess extends CompositeAccess
{
	public ShellAccess(Shell shell) {
		super(shell);
	}

    @Override
	public Shell getControl() {
		return (Shell)widget;
	}
    
    @Override
    protected Point getAbsolutePointToClick() {
        return getCenter(getControl().getBounds());
    }

    @Override
    @UIStep
    public Rectangle getAbsoluteBounds() {
        return getControl().getBounds();
    }
    
	@UIStep
	public MenuAccess getMenuBar() {
		return new MenuAccess(getControl().getMenuBar());
	}

	@InBackgroundThread
	public void chooseFromMainMenu(String labelPath) {
	    MenuAccess menuAccess = getMenuBar();
	    for (String label : labelPath.split("\\|"))
	        //menuAccess = menuAccess.findMenuItemByLabel(label).activateWithMouseClick();
	        menuAccess = menuAccess.activateMenuItemWithMouse(label);
	}

	/**
	 * Asserts that this shell is the active shell, i.e. it waits (with timeout)
	 * for it to become active.
	 */
	@UIStep
	public void assertIsActive() {
		Assert.assertTrue("not the active shell", Display.getCurrent().getActiveShell() == getControl());
	}
}
