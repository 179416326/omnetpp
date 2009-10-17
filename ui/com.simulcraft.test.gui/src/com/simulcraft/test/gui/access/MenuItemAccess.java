/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.
  
  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package com.simulcraft.test.gui.access;

import junit.framework.Assert;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.omnetpp.common.util.ReflectionUtils;

import com.simulcraft.test.gui.core.UIStep;

public class MenuItemAccess extends ClickableWidgetAccess
{
	public MenuItemAccess(MenuItem menuItem) {
		super(menuItem);
	}

    @Override
	public MenuItem getWidget() {
		return (MenuItem)widget;
	}

    @UIStep
    public void assertVisible() {
        Assert.assertTrue("menu item '"+getWidget().getText()+"' should be visible", getWidget().getParent().isVisible());
    }
    
	@UIStep
	public void assertEnabled() {
    	Assert.assertTrue("menu item '"+getWidget().getText()+"' should be enabled", getWidget().isEnabled());
	}

    @UIStep
    public void assertDisabled() {
        Assert.assertTrue("menu item '"+getWidget().getText()+"' should be disabled", !getWidget().isEnabled());
    }
    
	@UIStep
	public MenuAccess activateWithMouseClick() {
		assertEnabled();
		assertVisible();
		click();
		return getWidget().getMenu() == null ? null : new MenuAccess(getWidget().getMenu());
	}

	@Override
	protected Point getAbsolutePointToClick() {
		Rectangle parentRectangle = (Rectangle)ReflectionUtils.invokeMethod(getWidget().getParent(), "getBounds");
		Rectangle rectangle = (Rectangle)ReflectionUtils.invokeMethod(getWidget(), "getBounds");
		rectangle.x = rectangle.x + parentRectangle.x;
		rectangle.y = rectangle.y + parentRectangle.y;
		return getCenter(rectangle);
	}

	@Override
    protected Point toAbsolute(Point point) {
        Rectangle parentRectangle = (Rectangle)ReflectionUtils.invokeMethod(getWidget().getParent(), "getBounds");
        return new Point(parentRectangle.x + point.x, parentRectangle.y + point.y);
	}

	@Override
	protected Menu getContextMenu() {
		return null;  // a menu has no context menu
	}
}
