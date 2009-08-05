/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.omnetpp.scave.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.AbstractEnumerator;

/**
 * <!-- begin-user-doc -->
 * A representation of the literals of the enumeration '<em><b>Result Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * @see org.omnetpp.scave.model.ScaveModelPackage#getResultType()
 * @model
 * @generated
 */
public final class ResultType extends AbstractEnumerator {
	/**
	 * The '<em><b>Scalar</b></em>' literal value.
	 * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>Scalar</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
	 * @see #SCALAR_LITERAL
	 * @model name="scalar" literal="SCALAR"
	 * @generated
	 * @ordered
	 */
    public static final int SCALAR = 1;

	/**
	 * The '<em><b>Vector</b></em>' literal value.
	 * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>Vector</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
	 * @see #VECTOR_LITERAL
	 * @model name="vector" literal="VECTOR"
	 * @generated
	 * @ordered
	 */
    public static final int VECTOR = 2;

	/**
	 * The '<em><b>Histogram</b></em>' literal value.
	 * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>Histogram</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
	 * @see #HISTOGRAM_LITERAL
	 * @model name="histogram" literal="HISTOGRAM"
	 * @generated
	 * @ordered
	 */
    public static final int HISTOGRAM = 3;

	/**
	 * The '<em><b>Scalar</b></em>' literal object.
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @see #SCALAR
	 * @generated
	 * @ordered
	 */
    public static final ResultType SCALAR_LITERAL = new ResultType(SCALAR, "scalar", "SCALAR");

	/**
	 * The '<em><b>Vector</b></em>' literal object.
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @see #VECTOR
	 * @generated
	 * @ordered
	 */
    public static final ResultType VECTOR_LITERAL = new ResultType(VECTOR, "vector", "VECTOR");

	/**
	 * The '<em><b>Histogram</b></em>' literal object.
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @see #HISTOGRAM
	 * @generated
	 * @ordered
	 */
    public static final ResultType HISTOGRAM_LITERAL = new ResultType(HISTOGRAM, "histogram", "HISTOGRAM");

	/**
	 * An array of all the '<em><b>Result Type</b></em>' enumerators.
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated
	 */
    private static final ResultType[] VALUES_ARRAY =
		new ResultType[] {
			SCALAR_LITERAL,
			VECTOR_LITERAL,
			HISTOGRAM_LITERAL,
		};

	/**
	 * A public read-only list of all the '<em><b>Result Type</b></em>' enumerators.
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated
	 */
    public static final List VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Result Type</b></em>' literal with the specified literal value.
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated
	 */
    public static ResultType get(String literal) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			ResultType result = VALUES_ARRAY[i];
			if (result.toString().equals(literal)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Result Type</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated
	 */
    public static ResultType getByName(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			ResultType result = VALUES_ARRAY[i];
			if (result.getName().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Result Type</b></em>' literal with the specified integer value.
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated
	 */
    public static ResultType get(int value) {
		switch (value) {
			case SCALAR: return SCALAR_LITERAL;
			case VECTOR: return VECTOR_LITERAL;
			case HISTOGRAM: return HISTOGRAM_LITERAL;
		}
		return null;	
	}

	/**
	 * Only this class can construct instances.
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated
	 */
    private ResultType(int value, String name, String literal) {
		super(value, name, literal);
	}

} //ResultType
