/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.omnetpp.scave.model;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Discard</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.omnetpp.scave.model.Discard#getExcepts <em>Excepts</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.omnetpp.scave.model.ScaveModelPackage#getDiscard()
 * @model
 * @generated
 */
public interface Discard extends AddDiscardOp {
    /**
     * Returns the value of the '<em><b>Excepts</b></em>' containment reference list.
     * The list contents are of type {@link org.omnetpp.scave.model.Except}.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Excepts</em>' containment reference list isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @return the value of the '<em>Excepts</em>' containment reference list.
     * @see org.omnetpp.scave.model.ScaveModelPackage#getDiscard_Excepts()
     * @model containment="true"
     * @generated
     */
    EList<Except> getExcepts();

} // Discard