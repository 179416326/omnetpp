/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.model.ex;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.omnetpp.ned.model.DisplayString;
import org.omnetpp.ned.model.INEDElement;
import org.omnetpp.ned.model.NEDElement;
import org.omnetpp.ned.model.interfaces.IChannelKindTypeElement;
import org.omnetpp.ned.model.interfaces.IInterfaceTypeElement;
import org.omnetpp.ned.model.interfaces.INEDTypeInfo;
import org.omnetpp.ned.model.interfaces.INedTypeElement;
import org.omnetpp.ned.model.interfaces.INedTypeLookupContext;
import org.omnetpp.ned.model.notification.NEDModelEvent;
import org.omnetpp.ned.model.pojo.ChannelInterfaceElement;
import org.omnetpp.ned.model.pojo.ExtendsElement;

/**
 * TODO add documentation
 *
 * @author rhornig
 */
public class ChannelInterfaceElementEx extends ChannelInterfaceElement implements IChannelKindTypeElement, IInterfaceTypeElement {

	private INEDTypeInfo typeInfo;
	protected DisplayString displayString = null;

	protected ChannelInterfaceElementEx() {
		super();
		typeInfo = getDefaultNedTypeResolver().createTypeInfoFor(this);
	}

    protected ChannelInterfaceElementEx(INEDElement parent) {
		super(parent);
		typeInfo = getDefaultNedTypeResolver().createTypeInfoFor(this);
	}

    public INEDTypeInfo getNEDTypeInfo() {
    	return typeInfo;
    }

    @Override
    public void fireModelEvent(NEDModelEvent event) {
    	// invalidate cached display string because NED tree may have changed outside the DisplayString class
    	if (!NEDElementUtilEx.isDisplayStringUpToDate(displayString, this))
    		displayString = null;
    	super.fireModelEvent(event);
    }

    public DisplayString getDisplayString() {
    	if (displayString == null)
    		displayString = new DisplayString(this, NEDElementUtilEx.getDisplayStringLiteral(this));
    	displayString.setFallbackDisplayString(NEDElement.displayStringOf(getFirstExtendsRef()));
    	return displayString;
    }

    // "extends" support
    public String getFirstExtends() {
        return NEDElementUtilEx.getFirstExtends(this);
    }

    public void setFirstExtends(String ext) {
        NEDElementUtilEx.setFirstExtends(this, ext);
    }

    public INedTypeElement getFirstExtendsRef() {
        return getNEDTypeInfo().getFirstExtendsRef();
    }

    public List<ExtendsElement> getAllExtends() {
    	return getAllExtendsFrom(this);
    }

	public INedTypeLookupContext getParentLookupContext() {
		return getParentLookupContextFor(this);
	}

    // parameter support
    public Map<String, ParamElementEx> getParamAssignments() {
        return getNEDTypeInfo().getParamAssignments();
    }

    public Map<String, ParamElementEx> getParamDeclarations() {
        return getNEDTypeInfo().getParamDeclarations();
    }

    public List<ParamElementEx> getParameterInheritanceChain(String parameterName) {
        return getNEDTypeInfo().getParameterInheritanceChain(parameterName);
    }

    public Map<String, PropertyElementEx> getProperties() {
        return getNEDTypeInfo().getProperties();
    }

    public Set<INedTypeElement> getLocalUsedTypes() {
        return getNEDTypeInfo().getLocalUsedTypes();
    }
}
