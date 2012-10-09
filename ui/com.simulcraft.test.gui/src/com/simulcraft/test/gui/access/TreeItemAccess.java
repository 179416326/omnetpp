/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package com.simulcraft.test.gui.access;

import junit.framework.Assert;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;

import com.simulcraft.test.gui.core.InBackgroundThread;
import com.simulcraft.test.gui.core.UIStep;

public class TreeItemAccess extends ClickableWidgetAccess
{
    public TreeItemAccess(TreeItem treeItem) {
        super(treeItem);
    }

    @Override
    public TreeItem getWidget() {
        return (TreeItem)widget;
    }

    @UIStep
    public TreeAccess getTree() {
        return (TreeAccess) createAccess(getWidget().getParent());
    }

    @UIStep
    public TreeItemAccess reveal() {
        getWidget().getParent().showItem(getWidget());
        return this;
    }

    @Override @UIStep
    protected Point getAbsolutePointToClick() {
        Point point = toAbsolute(getCenter(getWidget().getBounds()));
        Assert.assertTrue("point to click is scrolled out", getTree().getAbsoluteBounds().contains(point));
        Assert.assertTrue("column has zero width", getWidget().getBounds().width > 0);
        return point;
    }

    @Override
    protected Point toAbsolute(Point point) {
        return getWidget().getParent().toDisplay(point);
    }

    /**
     * Useful for selecting a tree item without incidentally activating its cell editor.
     */
    @UIStep
    public void clickLeftEdge() {
        Rectangle bounds = getWidget().getBounds();
        Point point = getWidget().getParent().toDisplay(new Point(1, bounds.y+bounds.height/2));
        Assert.assertTrue("point to click is scrolled out", getTree().getAbsoluteBounds().contains(point));
        Assert.assertTrue("column has zero width", bounds.width > 0);
        clickAbsolute(LEFT_MOUSE_BUTTON, point);
    }

    @Override
    protected Menu getContextMenu() {
        return (Menu)getWidget().getParent().getMenu();
    }

    @UIStep
    public void ensureChecked(boolean state) {
        if (getWidget().getChecked() != state) {
            click();
            pressKey(' ');
        }
    }

    @UIStep
    public void ensureExpanded() {
        if (!getWidget().getExpanded()) {
            reveal();
            click();
            pressKey(SWT.KEYPAD_ADD);  // ARROW_RIGHT doesn't work on Linux
        }
    }

    @UIStep
    public void clickColumn(int index) {
        Point point = getWidget().getParent().toDisplay(getCenter(getWidget().getTextBounds(index)));
        Assert.assertTrue("point to click is scrolled out", getTree().getAbsoluteBounds().contains(point));
        clickAbsolute(LEFT_MOUSE_BUTTON, point);
    }

    @InBackgroundThread
    public TextAccess activateCellEditor() {
        Control oldFocusControl = getFocusControl().getControl();
        click();
        Control focusControl = getFocusControl().getControl();
        Assert.assertTrue("cell editor could not be activated", focusControl instanceof Text && oldFocusControl != focusControl);
        System.out.println(oldFocusControl.getClass().getSimpleName());
        return (TextAccess)createAccess(focusControl);
    }

    @InBackgroundThread
    public TextAccess activateCellEditor(int index) {
        Control oldFocusControl = getFocusControl().getControl();
        clickColumn(index);
        Control focusControl = getFocusControl().getControl();
        Assert.assertTrue("cell editor could not be activated", focusControl instanceof Text && oldFocusControl != focusControl);
        System.out.println(oldFocusControl.getClass().getSimpleName());
        return (TextAccess)createAccess(focusControl);
    }

    @InBackgroundThread
    public void clickAndTypeOver(String content) {
        TextAccess cellEditor = activateCellEditor();
        cellEditor.typeOver(content);
        cellEditor.pressEnter();
        //assertTextContent(Pattern.quote(content));  //XXX this would assume text gets stored "as is"
    }

    @InBackgroundThread
    public void clickAndTypeOver(int index, String content) {
        TextAccess cellEditor = activateCellEditor(index);
        cellEditor.typeOver(content);
        cellEditor.pressEnter();
        //assertTextContent(index, Pattern.quote(content));  //XXX this would assume text gets stored "as is"
    }

    @UIStep
    public void assertTextContent(String content) {
        Assert.assertTrue(getWidget().getText().matches(content));
    }

    @UIStep
    public void assertTextContent(int index, String content) {
        Assert.assertTrue(getWidget().getText(index).matches(content));
    }
}
