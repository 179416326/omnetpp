/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package com.simulcraft.test.gui.recorder.event;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Event;

import com.simulcraft.test.gui.recorder.GUIRecorder;
import com.simulcraft.test.gui.recorder.JavaSequence;

public class ButtonEventRecognizer extends EventRecognizer {

    public ButtonEventRecognizer(GUIRecorder recorder) {
        super(recorder);
    }

    public JavaSequence recognizeEvent(Event e) {
        if (e.type == SWT.MouseDown && e.widget instanceof Button) {
            Button button = (Button)e.widget;
            if ((button.getStyle() & SWT.PUSH) != 0) {
                return makeMethodCall(button, expr("selectWithMouseClick()", 0.8, null));
            }
            if ((button.getStyle() & (SWT.CHECK|SWT.RADIO)) != 0) {
                String methodText = button.getSelection() ? "deselectWithMouseClick()" : "selectWithMouseClick()"; //XXX no such methods yet
                return makeMethodCall(button, expr(methodText, 0.8, null));
            }
        }
        return null;
    }

}