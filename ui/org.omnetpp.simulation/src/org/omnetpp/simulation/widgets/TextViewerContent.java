/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.simulation.widgets;

import org.eclipse.swt.graphics.Color;


/**
 * An implementation of ITextViewerContent that uses a string as input.
 * 
 * @author Andras
 */
public class TextViewerContent implements ITextViewerContent {
    private String[] lines;  // text, split to lines

    public TextViewerContent(String text) {
        this.lines = text.split("\n");  // XXX split() discards trailing blank lines
        if (lines.length == 0) 
            lines = new String[] { "" };
    }
    
    @Override
    public int getLineCount() {
        return lines.length;
    }

    @Override
    public String getLineText(int lineIndex) {
        return lines[lineIndex];
    }

    @Override
    public Color getLineColor(int lineIndex) {
        return null;
    }

    @Override
    public void addTextChangeListener(ITextChangeListener listener) {
    }

    @Override
    public void removeTextChangeListener(ITextChangeListener listener) {
    }
}
