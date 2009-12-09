/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package com.simulcraft.test.gui.access;

import junit.framework.Assert;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import com.simulcraft.test.gui.core.UIStep;

public class TreeColumnAccess extends ClickableWidgetAccess
{
	public TreeColumnAccess(TreeColumn treeColumn) {
		super(treeColumn);
	}

    @Override
	public TreeColumn getWidget() {
		return (TreeColumn)widget;
	}

    @UIStep
    public TreeAccess getTree() {
        return (TreeAccess) createAccess(getWidget().getParent());
    }

	@UIStep
	public TreeColumnAccess reveal() {
		//TODO horizontally scroll there
		return this;
	}

	@Override
	protected Point getAbsolutePointToClick() {
        // center of the header. Note: header is at NEGATIVE table coordinates! (origin is top-left of data area)
	    getTree().assertHeaderVisible();
        Tree tree = (Tree)getWidget().getParent();
        Point point = tree.toDisplay(getX() + getWidget().getWidth()/2, -tree.getHeaderHeight()/2);
        Assert.assertTrue("point to click is scrolled out", getTree().getAbsoluteBounds().contains(point));
        Assert.assertTrue("column has zero width, cannot click", getWidget().getWidth() > 0);
        return point;
	}

	@Override
	protected Point toAbsolute(Point point) {
	    // TODO:
	    throw new RuntimeException();
	}

	@Override
	protected Menu getContextMenu() {
		return (Menu)getWidget().getParent().getMenu();
	}

	/**
	 * Returns the x coordinate of the left edge of the column, relative to the tree.
	 * Horizontal scrolling of tree is also taken into account.
	 */
	@UIStep
	public int getX() {
	    Tree tree = (Tree)getWidget().getParent();
	    TreeColumn[] columns = tree.getColumns();
	    int[] columnOrder = tree.getColumnOrder();
	    int x = 0;
	    for (int i = 0; i < columns.length; i++) {
	        TreeColumn col = columns[columnOrder[i]];
	        if (col == getWidget()) {
                int horizontalScrollOffset = tree.getHorizontalBar().getSelection();
                return x - horizontalScrollOffset;
            }
	        x += col.getWidth();
	    }
	    Assert.assertTrue("column is not in the tree", false);
	    return 0;
	}
}
