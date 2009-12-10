/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package com.simulcraft.test.gui.recorder.event;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;

import com.simulcraft.test.gui.recorder.GUIRecorder;
import com.simulcraft.test.gui.recorder.JavaSequence;

public class TextClickRecognizer extends EventRecognizer {
    public TextClickRecognizer(GUIRecorder recorder) {
        super(recorder);
    }

    public JavaSequence recognizeEvent(Event e) {
        if (e.widget instanceof Text && (e.type == SWT.MouseDown || e.type == SWT.MouseDoubleClick) && e.button == 1) { // left click into a StyledText
            Text text = (Text) e.widget;
            //return makeSeq(text, expr("clickRelative("+e.x+", "+e.y+")", 0.5, null));
            return makeMethodCall(text, expr("click()", 0.5, null));
        }
        return null;
    }
}

