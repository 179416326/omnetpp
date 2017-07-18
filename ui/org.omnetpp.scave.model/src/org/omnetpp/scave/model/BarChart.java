/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

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
 * A representation of the model object '<em><b>Bar Chart</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.omnetpp.scave.model.BarChart#getGroupByFields <em>Group By Fields</em>}</li>
 *   <li>{@link org.omnetpp.scave.model.BarChart#getGroupNameFormat <em>Group Name Format</em>}</li>
 *   <li>{@link org.omnetpp.scave.model.BarChart#getBarNameFormat <em>Bar Name Format</em>}</li>
 *   <li>{@link org.omnetpp.scave.model.BarChart#getBarFields <em>Bar Fields</em>}</li>
 *   <li>{@link org.omnetpp.scave.model.BarChart#getAveragedFields <em>Averaged Fields</em>}</li>
 * </ul>
 *
 * @see org.omnetpp.scave.model.ScaveModelPackage#getBarChart()
 * @model
 * @generated
 */
public interface BarChart extends Chart {
    /**
     * Returns the value of the '<em><b>Group By Fields</b></em>' attribute list.
     * The list contents are of type {@link java.lang.String}.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Group By Fields</em>' attribute list isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @return the value of the '<em>Group By Fields</em>' attribute list.
     * @see org.omnetpp.scave.model.ScaveModelPackage#getBarChart_GroupByFields()
     * @model ordered="false"
     * @generated
     */
    EList<String> getGroupByFields();

    /**
     * Returns the value of the '<em><b>Group Name Format</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Group Name Format</em>' attribute isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @return the value of the '<em>Group Name Format</em>' attribute.
     * @see #setGroupNameFormat(String)
     * @see org.omnetpp.scave.model.ScaveModelPackage#getBarChart_GroupNameFormat()
     * @model
     * @generated
     */
    String getGroupNameFormat();

    /**
     * Sets the value of the '{@link org.omnetpp.scave.model.BarChart#getGroupNameFormat <em>Group Name Format</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @param value the new value of the '<em>Group Name Format</em>' attribute.
     * @see #getGroupNameFormat()
     * @generated
     */
    void setGroupNameFormat(String value);

    /**
     * Returns the value of the '<em><b>Bar Name Format</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Bar Name Format</em>' attribute isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @return the value of the '<em>Bar Name Format</em>' attribute.
     * @see #setBarNameFormat(String)
     * @see org.omnetpp.scave.model.ScaveModelPackage#getBarChart_BarNameFormat()
     * @model
     * @generated
     */
    String getBarNameFormat();

    /**
     * Sets the value of the '{@link org.omnetpp.scave.model.BarChart#getBarNameFormat <em>Bar Name Format</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @param value the new value of the '<em>Bar Name Format</em>' attribute.
     * @see #getBarNameFormat()
     * @generated
     */
    void setBarNameFormat(String value);

    /**
     * Returns the value of the '<em><b>Bar Fields</b></em>' attribute list.
     * The list contents are of type {@link java.lang.String}.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Bar Fields</em>' attribute list isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @return the value of the '<em>Bar Fields</em>' attribute list.
     * @see org.omnetpp.scave.model.ScaveModelPackage#getBarChart_BarFields()
     * @model ordered="false"
     * @generated
     */
    EList<String> getBarFields();

    /**
     * Returns the value of the '<em><b>Averaged Fields</b></em>' attribute list.
     * The list contents are of type {@link java.lang.String}.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Averaged Fields</em>' attribute list isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @return the value of the '<em>Averaged Fields</em>' attribute list.
     * @see org.omnetpp.scave.model.ScaveModelPackage#getBarChart_AveragedFields()
     * @model ordered="false"
     * @generated
     */
    EList<String> getAveragedFields();

} // BarChart