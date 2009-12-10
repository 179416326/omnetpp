/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.test.gui.inifileeditor.configpages;

import org.eclipse.swt.SWT;
import org.omnetpp.test.gui.access.InifileFormEditorAccess;
import org.omnetpp.test.gui.inifileeditor.InifileEditorTestCase;

import com.simulcraft.test.gui.access.LabelAccess;
import com.simulcraft.test.gui.access.TextAccess;
import com.simulcraft.test.gui.access.TextEditorAccess;

public class TextFieldEditorTest extends InifileEditorTestCase {
    private TextAccess prepareTest() throws Exception {
        createFileWithContent("");
        openFileFromProjectExplorerView();
        InifileFormEditorAccess formEditor = findInifileEditor().ensureActiveFormEditor();
        TextAccess text = formEditor.activateCategoryPage("Output Files").findTextAfterLabel("Output vector file.*");
        return text;
    }

//    private void assertTextEditorContentMatches(String content) {
//        findInifileEditor().ensureActiveTextEditor().assertContentIgnoringWhiteSpace(content);
//    }

    public void testFocusTransfer() throws Exception {
        // text field should gain focus when its label is clicked
        TextAccess text = prepareTest();
        text.assertHasNoFocus();

        InifileFormEditorAccess formEditor = findInifileEditor().ensureActiveFormEditor();
        LabelAccess label = formEditor.getActiveCategoryPage().findLabel("Output vector file.*");
        label.click();

        text.assertHasFocus(); // must get focus when its text gets
    }

    public void testCtrlA() throws Exception {
        TextAccess text = prepareTest();

        text.clickAndTypeOver("fooooo");

        text.pressKey('a', SWT.CTRL);
        text.pressKey(SWT.BS);

        text.assertTextContent(""); // must be empty now
    }

    public void testShiftHome() throws Exception {
        TextAccess text = prepareTest();

        text.clickAndTypeOver("FooOoo");

        text.pressKey(SWT.END);
        text.pressKey(SWT.HOME, SWT.SHIFT);
        text.pressKey(SWT.BS);

        text.assertTextContent(""); // must be empty now
    }

    public void testShiftHomeInTextEditor() throws Exception {
        createFileWithContent("");
        openFileFromProjectExplorerView();
        TextEditorAccess textEditor = findInifileEditor().ensureActiveTextEditor();
        textEditor.typeIn("hello world");

        textEditor.pressKey(SWT.END);
        textEditor.pressKey(SWT.HOME, SWT.SHIFT);
        textEditor.pressKey(SWT.BS);
    }
}
