/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package com.simulcraft.test.gui.access;

import junit.framework.Assert;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.TableItem;
import org.omnetpp.common.ui.ImageCombo;

import com.simulcraft.test.gui.core.InBackgroundThread;
import com.simulcraft.test.gui.core.UIStep;

public class ImageComboAccess extends ControlAccess
{
    public ImageComboAccess(ImageCombo combo) {
        super(combo);
    }

    @Override
    public ImageCombo getControl() {
        return (ImageCombo)widget;
    }

    @UIStep
    public boolean isEditable() {
        return (getControl().getStyle() & SWT.READ_ONLY) == 0;
    }

    @UIStep
    public String[] getComboItems() {
        TableItem[] items = getControl().getItems();
        String[] result = new String[items.length];
        for (int i=0; i<items.length; i++)
            result[i] = items[i].getText();
        return result;
    }

    @UIStep
    public String getTextContent() {
        return getControl().getText();
    }

    @UIStep
    public void assertEditable() {
        Assert.assertTrue("combo is readonly", isEditable());
    }

    @UIStep
    public void assertTextContent(String regex) {
        Assert.assertTrue("combo content does not match "+regex, getTextContent().matches(regex));
    }

    @UIStep
    public void typeOver(String content) {
        assertEnabled();
        assertEditable();
        assertHasFocus();
        ImageCombo combo = getControl();
        pressKey('a', SWT.CONTROL);
        if (combo.getText().length() > 0 && content.equals(""))
            pressKey(SWT.DEL);
        else
            typeIn(content);
    }

    @InBackgroundThread
    public void clickAndTypeOver(String content) {
        click();
        typeOver(content);
    }

    @UIStep
    public void clickOnArrow() {
        Rectangle bounds = getControl().getBounds();
        int x = bounds.x + bounds.width - 5;
        int y = bounds.y + bounds.height / 2;
        clickAbsolute(LEFT_MOUSE_BUTTON, getControl().getParent().toDisplay(x, y));
    }

    @InBackgroundThread
    public void selectItem(String content) {
        clickOnArrow();

        if (isEditable()) {
            typeOver("");
            pressKey(SWT.ARROW_DOWN);
        }
        else
            pressKey(SWT.HOME);

        int index = findString(getComboItems(), content);
        for (int i=0; i<index; i++)
            pressKey(SWT.ARROW_DOWN);

        pressEnter();

        assertTextContent(content);
    }

    @UIStep
    public void assertContainsItem(String regex) {
        Assert.assertTrue("combo does not contain item: " + regex, findString(getComboItems(), regex) != -1);
    }

    @UIStep
    public void assertNotContainsItem(String regex) {
        Assert.assertTrue("combo contains item: " + regex, findString(getComboItems(), regex) == -1);
    }

}
