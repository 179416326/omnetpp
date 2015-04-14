/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package com.simulcraft.test.gui.recorder.event;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

import com.simulcraft.test.gui.recorder.GUIRecorder;
import com.simulcraft.test.gui.recorder.JavaSequence;

public class TraversalRecognizer extends EventRecognizer {
    public TraversalRecognizer(GUIRecorder recorder) {
        super(recorder);
    }

    public JavaSequence recognizeEvent(Event e) {
        if (e.type == SWT.Traverse) {
            // see TraversalEvent documentation
            // XXX Content Assist: when navigating while it has focus (ie after 1 click), arrows will be recorded twice! (both as KeyDown and Traversal)
            switch (e.detail) {
                case SWT.TRAVERSE_NONE: break;
                case SWT.TRAVERSE_ESCAPE: return pressKey(SWT.ESC, 0); //FIXME generates 5 lines or so (???)
                case SWT.TRAVERSE_RETURN: return pressKey(SWT.CR, 0); //FIXME generates 5 lines or so (???)
                case SWT.TRAVERSE_TAB_NEXT: return pressKey(SWT.TAB, 0);
                case SWT.TRAVERSE_TAB_PREVIOUS: return pressKey(SWT.TAB, SWT.SHIFT);
                case SWT.TRAVERSE_ARROW_NEXT: return pressKey(SWT.ARROW_DOWN, 0);
                case SWT.TRAVERSE_ARROW_PREVIOUS: return pressKey(SWT.ARROW_UP, 0);
                case SWT.TRAVERSE_MNEMONIC: break;  //XXX ???
                case SWT.TRAVERSE_PAGE_NEXT: return pressKey(SWT.PAGE_DOWN, 0);
                case SWT.TRAVERSE_PAGE_PREVIOUS: return pressKey(SWT.PAGE_UP, 0);
            }
        }
        return null;
    }

    private JavaSequence pressKey(int keyCode, int modifierState) {
        return makeStatement(expr("Access." + KeyboardEventRecognizer.toPressKeyInvocation(keyCode, modifierState), 0.5, null));
    }

 }