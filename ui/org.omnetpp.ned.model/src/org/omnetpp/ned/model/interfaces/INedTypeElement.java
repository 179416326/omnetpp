/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.model.interfaces;

import java.util.List;

import org.omnetpp.ned.model.pojo.ExtendsElement;


/**
 * An interface for elements that have type info associated and can be extended
 * by other types. They are toplevel elements
 * in a NED file, except property definitions and includes.
 *
 * @author rhornig
 */
public interface INedTypeElement extends ITypeElement, IHasDisplayString, IHasParameters {

	/**
	 * Returns the typeinfo belonging to this NED type. This can be trusted
	 * to be NOT null for all NED types, including duplicate and invalid ones.
	 *
	 * Only null for detached trees that haven't been seen at all by NEDResources
	 * (i.e. not part of any NED file).
	 */
	public INedTypeInfo getNedTypeInfo();

    /**
     * Returns the base object's name (ONLY the first extends node name returned).
     * NULL if no base object exist or the object (or its inheritance chain) is invalid
     * (ie. contains a cycle)
     */
    public String getFirstExtends();

    /**
     * Sets the first "extends" node to the given NED type name
     */
    public void setFirstExtends(String name);

    /**
     * Returns the model element that represents the base object of this element.
     *
     * NOTE that this checks only the FIRST "extends" node, so it doesn't return full
     * inheritance info for ModuleInterface and ChannelInterface.
     */
    public INedTypeElement getSuperType();

    /**
     * Returns the list of child elements that hold the "extends" names (usually only
     * a single element, but for interfaces it can be more)
     */
    public List<ExtendsElement> getAllExtends();

    /**
     * For an inner type it returns the containing compound module type, and
     * for a toplevel type it returns the containing NED file.
     */
	public INedTypeLookupContext getParentLookupContext();

}
