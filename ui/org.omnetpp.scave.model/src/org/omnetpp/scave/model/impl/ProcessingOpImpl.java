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
package org.omnetpp.scave.model.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

import org.omnetpp.scave.model.Param;
import org.omnetpp.scave.model.ProcessingOp;
import org.omnetpp.scave.model.ScaveModelPackage;
import org.omnetpp.scave.model.SelectDeselectOp;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Processing Op</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.omnetpp.scave.model.impl.ProcessingOpImpl#getOperation <em>Operation</em>}</li>
 *   <li>{@link org.omnetpp.scave.model.impl.ProcessingOpImpl#getFilters <em>Filters</em>}</li>
 *   <li>{@link org.omnetpp.scave.model.impl.ProcessingOpImpl#getParams <em>Params</em>}</li>
 *   <li>{@link org.omnetpp.scave.model.impl.ProcessingOpImpl#getComputedFile <em>Computed File</em>}</li>
 *   <li>{@link org.omnetpp.scave.model.impl.ProcessingOpImpl#getComputationHash <em>Computation Hash</em>}</li>
 *   <li>{@link org.omnetpp.scave.model.impl.ProcessingOpImpl#getGroupBy <em>Group By</em>}</li>
 * </ul>
 *
 * @generated
 */
public abstract class ProcessingOpImpl extends DatasetItemImpl implements ProcessingOp {
    /**
     * The default value of the '{@link #getOperation() <em>Operation</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getOperation()
     * @generated
     * @ordered
     */
    protected static final String OPERATION_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getOperation() <em>Operation</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getOperation()
     * @generated
     * @ordered
     */
    protected String operation = OPERATION_EDEFAULT;

    /**
     * The cached value of the '{@link #getFilters() <em>Filters</em>}' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getFilters()
     * @generated
     * @ordered
     */
    protected EList<SelectDeselectOp> filters;

    /**
     * The cached value of the '{@link #getParams() <em>Params</em>}' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getParams()
     * @generated
     * @ordered
     */
    protected EList<Param> params;

    /**
     * The default value of the '{@link #getComputedFile() <em>Computed File</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getComputedFile()
     * @generated
     * @ordered
     */
    protected static final String COMPUTED_FILE_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getComputedFile() <em>Computed File</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getComputedFile()
     * @generated
     * @ordered
     */
    protected String computedFile = COMPUTED_FILE_EDEFAULT;

    /**
     * The default value of the '{@link #getComputationHash() <em>Computation Hash</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getComputationHash()
     * @generated
     * @ordered
     */
    protected static final long COMPUTATION_HASH_EDEFAULT = 0L;

    /**
     * The cached value of the '{@link #getComputationHash() <em>Computation Hash</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getComputationHash()
     * @generated
     * @ordered
     */
    protected long computationHash = COMPUTATION_HASH_EDEFAULT;

    /**
     * The cached value of the '{@link #getGroupBy() <em>Group By</em>}' attribute list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getGroupBy()
     * @generated
     * @ordered
     */
    protected EList<String> groupBy;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    protected ProcessingOpImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    @Override
    protected EClass eStaticClass() {
        return ScaveModelPackage.Literals.PROCESSING_OP;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public String getOperation() {
        return operation;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public void setOperation(String newOperation) {
        String oldOperation = operation;
        operation = newOperation;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, ScaveModelPackage.PROCESSING_OP__OPERATION, oldOperation, operation));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public EList<SelectDeselectOp> getFilters() {
        if (filters == null) {
            filters = new EObjectContainmentEList<SelectDeselectOp>(SelectDeselectOp.class, this, ScaveModelPackage.PROCESSING_OP__FILTERS);
        }
        return filters;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public EList<Param> getParams() {
        if (params == null) {
            params = new EObjectContainmentEList<Param>(Param.class, this, ScaveModelPackage.PROCESSING_OP__PARAMS);
        }
        return params;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public String getComputedFile() {
        return computedFile;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public void setComputedFile(String newComputedFile) {
        String oldComputedFile = computedFile;
        computedFile = newComputedFile;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, ScaveModelPackage.PROCESSING_OP__COMPUTED_FILE, oldComputedFile, computedFile));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public long getComputationHash() {
        return computationHash;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public void setComputationHash(long newComputationHash) {
        long oldComputationHash = computationHash;
        computationHash = newComputationHash;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, ScaveModelPackage.PROCESSING_OP__COMPUTATION_HASH, oldComputationHash, computationHash));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public EList<String> getGroupBy() {
        if (groupBy == null) {
            groupBy = new EDataTypeUniqueEList<String>(String.class, this, ScaveModelPackage.PROCESSING_OP__GROUP_BY);
        }
        return groupBy;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    @Override
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch (featureID) {
            case ScaveModelPackage.PROCESSING_OP__FILTERS:
                return ((InternalEList<?>)getFilters()).basicRemove(otherEnd, msgs);
            case ScaveModelPackage.PROCESSING_OP__PARAMS:
                return ((InternalEList<?>)getParams()).basicRemove(otherEnd, msgs);
        }
        return super.eInverseRemove(otherEnd, featureID, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    @Override
    public Object eGet(int featureID, boolean resolve, boolean coreType) {
        switch (featureID) {
            case ScaveModelPackage.PROCESSING_OP__OPERATION:
                return getOperation();
            case ScaveModelPackage.PROCESSING_OP__FILTERS:
                return getFilters();
            case ScaveModelPackage.PROCESSING_OP__PARAMS:
                return getParams();
            case ScaveModelPackage.PROCESSING_OP__COMPUTED_FILE:
                return getComputedFile();
            case ScaveModelPackage.PROCESSING_OP__COMPUTATION_HASH:
                return getComputationHash();
            case ScaveModelPackage.PROCESSING_OP__GROUP_BY:
                return getGroupBy();
        }
        return super.eGet(featureID, resolve, coreType);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    @SuppressWarnings("unchecked")
        @Override
    public void eSet(int featureID, Object newValue) {
        switch (featureID) {
            case ScaveModelPackage.PROCESSING_OP__OPERATION:
                setOperation((String)newValue);
                return;
            case ScaveModelPackage.PROCESSING_OP__FILTERS:
                getFilters().clear();
                getFilters().addAll((Collection<? extends SelectDeselectOp>)newValue);
                return;
            case ScaveModelPackage.PROCESSING_OP__PARAMS:
                getParams().clear();
                getParams().addAll((Collection<? extends Param>)newValue);
                return;
            case ScaveModelPackage.PROCESSING_OP__COMPUTED_FILE:
                setComputedFile((String)newValue);
                return;
            case ScaveModelPackage.PROCESSING_OP__COMPUTATION_HASH:
                setComputationHash((Long)newValue);
                return;
            case ScaveModelPackage.PROCESSING_OP__GROUP_BY:
                getGroupBy().clear();
                getGroupBy().addAll((Collection<? extends String>)newValue);
                return;
        }
        super.eSet(featureID, newValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    @Override
    public void eUnset(int featureID) {
        switch (featureID) {
            case ScaveModelPackage.PROCESSING_OP__OPERATION:
                setOperation(OPERATION_EDEFAULT);
                return;
            case ScaveModelPackage.PROCESSING_OP__FILTERS:
                getFilters().clear();
                return;
            case ScaveModelPackage.PROCESSING_OP__PARAMS:
                getParams().clear();
                return;
            case ScaveModelPackage.PROCESSING_OP__COMPUTED_FILE:
                setComputedFile(COMPUTED_FILE_EDEFAULT);
                return;
            case ScaveModelPackage.PROCESSING_OP__COMPUTATION_HASH:
                setComputationHash(COMPUTATION_HASH_EDEFAULT);
                return;
            case ScaveModelPackage.PROCESSING_OP__GROUP_BY:
                getGroupBy().clear();
                return;
        }
        super.eUnset(featureID);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    @Override
    public boolean eIsSet(int featureID) {
        switch (featureID) {
            case ScaveModelPackage.PROCESSING_OP__OPERATION:
                return OPERATION_EDEFAULT == null ? operation != null : !OPERATION_EDEFAULT.equals(operation);
            case ScaveModelPackage.PROCESSING_OP__FILTERS:
                return filters != null && !filters.isEmpty();
            case ScaveModelPackage.PROCESSING_OP__PARAMS:
                return params != null && !params.isEmpty();
            case ScaveModelPackage.PROCESSING_OP__COMPUTED_FILE:
                return COMPUTED_FILE_EDEFAULT == null ? computedFile != null : !COMPUTED_FILE_EDEFAULT.equals(computedFile);
            case ScaveModelPackage.PROCESSING_OP__COMPUTATION_HASH:
                return computationHash != COMPUTATION_HASH_EDEFAULT;
            case ScaveModelPackage.PROCESSING_OP__GROUP_BY:
                return groupBy != null && !groupBy.isEmpty();
        }
        return super.eIsSet(featureID);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    @Override
    public String toString() {
        if (eIsProxy()) return super.toString();

        StringBuffer result = new StringBuffer(super.toString());
        result.append(" (operation: ");
        result.append(operation);
        result.append(", computedFile: ");
        result.append(computedFile);
        result.append(", computationHash: ");
        result.append(computationHash);
        result.append(", groupBy: ");
        result.append(groupBy);
        result.append(')');
        return result.toString();
    }

} //ProcessingOpImpl