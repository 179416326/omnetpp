/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.omnetpp.scave.model.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.impl.EFactoryImpl;

import org.eclipse.emf.ecore.plugin.EcorePlugin;

import org.omnetpp.scave.model.*;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class ScaveModelFactoryImpl extends EFactoryImpl implements ScaveModelFactory {
	/**
	 * Creates the default factory implementation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static ScaveModelFactory init() {
		try {
			ScaveModelFactory theScaveModelFactory = (ScaveModelFactory)EPackage.Registry.INSTANCE.getEFactory("http://www.omnetpp.org/omnetpp/scave"); 
			if (theScaveModelFactory != null) {
				return theScaveModelFactory;
			}
		}
		catch (Exception exception) {
			EcorePlugin.INSTANCE.log(exception);
		}
		return new ScaveModelFactoryImpl();
	}

	/**
	 * Creates an instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ScaveModelFactoryImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EObject create(EClass eClass) {
		switch (eClass.getClassifierID()) {
			case ScaveModelPackage.DATASET: return createDataset();
			case ScaveModelPackage.CHART: return createChart();
			case ScaveModelPackage.ADD: return createAdd();
			case ScaveModelPackage.APPLY: return createApply();
			case ScaveModelPackage.EXCEPT: return createExcept();
			case ScaveModelPackage.PROPERTY: return createProperty();
			case ScaveModelPackage.GROUP: return createGroup();
			case ScaveModelPackage.DISCARD: return createDiscard();
			case ScaveModelPackage.PARAM: return createParam();
			case ScaveModelPackage.CHART_SHEET: return createChartSheet();
			case ScaveModelPackage.ANALYSIS: return createAnalysis();
			case ScaveModelPackage.SELECT: return createSelect();
			case ScaveModelPackage.DESELECT: return createDeselect();
			case ScaveModelPackage.INPUTS: return createInputs();
			case ScaveModelPackage.CHART_SHEETS: return createChartSheets();
			case ScaveModelPackage.DATASETS: return createDatasets();
			case ScaveModelPackage.INPUT_FILE: return createInputFile();
			case ScaveModelPackage.COMPUTE: return createCompute();
			default:
				throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Object createFromString(EDataType eDataType, String initialValue) {
		switch (eDataType.getClassifierID()) {
			case ScaveModelPackage.DATASET_TYPE:
				return createDatasetTypeFromString(eDataType, initialValue);
			default:
				throw new IllegalArgumentException("The datatype '" + eDataType.getName() + "' is not a valid classifier");
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertToString(EDataType eDataType, Object instanceValue) {
		switch (eDataType.getClassifierID()) {
			case ScaveModelPackage.DATASET_TYPE:
				return convertDatasetTypeToString(eDataType, instanceValue);
			default:
				throw new IllegalArgumentException("The datatype '" + eDataType.getName() + "' is not a valid classifier");
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Dataset createDataset() {
		DatasetImpl dataset = new DatasetImpl();
		return dataset;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Chart createChart() {
		ChartImpl chart = new ChartImpl();
		return chart;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Add createAdd() {
		AddImpl add = new AddImpl();
		return add;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Apply createApply() {
		ApplyImpl apply = new ApplyImpl();
		return apply;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Except createExcept() {
		ExceptImpl except = new ExceptImpl();
		return except;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Property createProperty() {
		PropertyImpl property = new PropertyImpl();
		return property;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Group createGroup() {
		GroupImpl group = new GroupImpl();
		return group;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Discard createDiscard() {
		DiscardImpl discard = new DiscardImpl();
		return discard;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Param createParam() {
		ParamImpl param = new ParamImpl();
		return param;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ChartSheet createChartSheet() {
		ChartSheetImpl chartSheet = new ChartSheetImpl();
		return chartSheet;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Analysis createAnalysis() {
		AnalysisImpl analysis = new AnalysisImpl();
		return analysis;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Select createSelect() {
		SelectImpl select = new SelectImpl();
		return select;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Deselect createDeselect() {
		DeselectImpl deselect = new DeselectImpl();
		return deselect;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Inputs createInputs() {
		InputsImpl inputs = new InputsImpl();
		return inputs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ChartSheets createChartSheets() {
		ChartSheetsImpl chartSheets = new ChartSheetsImpl();
		return chartSheets;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Datasets createDatasets() {
		DatasetsImpl datasets = new DatasetsImpl();
		return datasets;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public InputFile createInputFile() {
		InputFileImpl inputFile = new InputFileImpl();
		return inputFile;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Compute createCompute() {
		ComputeImpl compute = new ComputeImpl();
		return compute;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public DatasetType createDatasetTypeFromString(EDataType eDataType, String initialValue) {
		DatasetType result = DatasetType.get(initialValue);
		if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertDatasetTypeToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ScaveModelPackage getScaveModelPackage() {
		return (ScaveModelPackage)getEPackage();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @deprecated
	 * @generated
	 */
	public static ScaveModelPackage getPackage() {
		return ScaveModelPackage.eINSTANCE;
	}

} //ScaveModelFactoryImpl
