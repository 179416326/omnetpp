/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package com.simulcraft.test.gui.access;

import java.util.List;

import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.omnetpp.common.ui.ImageCombo;
import org.omnetpp.common.util.IPredicate;
import org.omnetpp.common.util.Predicate;

import com.simulcraft.test.gui.core.UIStep;


public class CompositeAccess extends ControlAccess
{
    public CompositeAccess(Composite composite) {
        super(composite);
    }

    @Override
    public Composite getControl() {
        return (Composite)widget;
    }

    @UIStep
    public Control findDescendantControl(Class<? extends Control> clazz) {
        return findDescendantControl(getControl(), clazz);
    }

    @UIStep
    public Control findDescendantControl(IPredicate predicate) {
        return findDescendantControl(getControl(), predicate);
    }

    @UIStep
    public List<Control> collectDescendantControls(IPredicate predicate) {
        return collectDescendantControls(getControl(), predicate);
    }

    @UIStep
    public ButtonAccess findButtonWithLabel(final String label) {
        return new ButtonAccess((Button)findDescendantControl(getControl(), new IPredicate() {
            public boolean matches(Object object) {
                if (object instanceof Button) {
                    Button button = (Button)object;
                    return button.getText().replace("&", "").matches(label);
                }
                return false;
            }

            @Override
            public String toString() {
                return "button with label '"+label+"'";
            }
        }));
    }

    @UIStep
    public ButtonAccess findButtonAfterLabel(final String labelText, final String buttonText) {
        final LabelAccess labelAccess = findLabel(labelText);
        return new ButtonAccess((Button)labelAccess.findNextControl(new IPredicate() {
            public boolean matches(Object object) {
                if (object instanceof Button) {
                    Button button = (Button)object;
                    return button.getText().replace("&", "").matches(buttonText);
                }
                return false;
            }
            @Override
            public String toString() {
                return "button with label '"+buttonText+"' after label '"+labelText+"'";
            }
        }));
    }

    @UIStep
    public ButtonAccess tryToFindButtonWithLabel(final String label) {
        List<Control> buttons = collectDescendantControls(getControl(), new IPredicate() {
            public boolean matches(Object object) {
                if (object instanceof Button) {
                    Button button = (Button)object;
                    return button.getText().replace("&", "").matches(label);
                }
                return false;
            }

            @Override
            public String toString() {
                return "button with label '"+label+"'";
            }
        });
        Button button = (Button)atMostOneObject(buttons);
        return button != null ? (ButtonAccess)createAccess(button) : null;
    }

    @UIStep
    public LabelAccess findLabel(final String label) {
        Label labelControl = (Label)findDescendantControl(getControl(), new IPredicate() {
            public boolean matches(Object object) {
                if (object instanceof Label) {
                    Label labelControl = (Label)object;
                    return labelControl.getText().replace("&", "").matches(label);
                }
                return false;
            }

            @Override
            public String toString() {
                return "label '"+label+"'";
            }
        });
        return new LabelAccess(labelControl);
    }

    @UIStep
    public TreeAccess findTree() {
        return new TreeAccess((Tree)findDescendantControl(Tree.class));
    }

    @UIStep
    public TableAccess findTable() {
        return new TableAccess((Table)findDescendantControl(Table.class));
    }

       @UIStep
    public CanvasAccess findCanvas() {
            return new CanvasAccess((Canvas)findDescendantControl(Canvas.class));
    }

    @UIStep
    public CTabFolderAccess findCTabFolder() {
        return (CTabFolderAccess)createAccess(findDescendantControl(getControl(), CTabFolder.class));
    }

    @UIStep
    public TabFolderAccess findTabFolder() {
        return (TabFolderAccess)createAccess(findDescendantControl(getControl(), TabFolder.class));
    }

    @UIStep
    public StyledTextAccess findStyledText() {
        return new StyledTextAccess((StyledText)findDescendantControl(StyledText.class));
    }

    @UIStep
    public ComboAccess findCombo() {
        return (ComboAccess)createAccess(findDescendantControl(Combo.class));
    }

    @UIStep
    public TextAccess findTextAfterLabel(String label) {
        final LabelAccess labelAccess = findLabel(label);
        return new TextAccess((Text)labelAccess.findNextControl(Text.class));
    }

    @UIStep
    public TextAccess findText() {
        return new TextAccess((Text)Access.findDescendantControl(getControl(), Text.class));
    }

    @UIStep
    public ComboAccess findComboAfterLabel(String label) {
        final LabelAccess labelAccess = findLabel(label);
        return new ComboAccess((Combo)labelAccess.findNextControl(Combo.class));
    }

    @UIStep
    public ImageComboAccess findImageComboAfterLabel(String label) {
        final LabelAccess labelAccess = findLabel(label);
        return new ImageComboAccess((ImageCombo)labelAccess.findNextControl(ImageCombo.class));
    }

    @UIStep
    public TreeAccess findTreeAfterLabel(String label) {
        final LabelAccess labelAccess = findLabel(label);
        return new TreeAccess((Tree)labelAccess.findNextControl(Tree.class));
    }

    @UIStep
    public TableAccess findTableAfterLabel(String label) {
        final LabelAccess labelAccess = findLabel(label);
        return new TableAccess((Table)labelAccess.findNextControl(Table.class));
    }

    @UIStep
    public ControlAccess findControlWithID(String id) {
        return (ControlAccess)createAccess(findDescendantControl(Predicate.hasID(id)));
    }

    @UIStep
    public ToolItemAccess findToolItemWithTooltip(final String toolTip) {
        ToolBar toolBar = (ToolBar)findDescendantControl(new IPredicate() {
            public boolean matches(Object object) {
                if (object instanceof ToolBar) {
                    ToolBar toolBar = ((ToolBar)object);
                    if (findToolItem(toolBar, toolTip) != null)
                        return true;
                }
                return false;
            }

            public String toString() {
                return "a ToolItem with tool tip: " + toolTip;
            }
        });
        return (ToolItemAccess)createAccess(findToolItem(toolBar, toolTip));
    }

    private ToolItem findToolItem(ToolBar toolBar, final String tooltip) {
        return (ToolItem)findObject(toolBar.getItems(), true, new IPredicate() {
            public boolean matches(Object object) {
                ToolItem toolItem = (ToolItem)object;
                return toolItem.getToolTipText() != null && toolItem.getToolTipText().matches(tooltip);
            }

            public String toString() {
                return "a ToolItem with tool tip: " + tooltip;
            }
        });
    }

}
