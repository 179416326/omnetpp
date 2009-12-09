/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.model.notification;

import org.omnetpp.ned.model.INEDElement;

/**
 * Fired when a NEDElement has problem markers added or cleared.
 *
 * @author Andras
 */
public class NEDMarkerChangeEvent extends NEDModelEvent
{
	public NEDMarkerChangeEvent(INEDElement source) {
		super(source);
	}
}
