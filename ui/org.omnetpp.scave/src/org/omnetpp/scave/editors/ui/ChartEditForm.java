package org.omnetpp.scave.editors.ui;

import static org.omnetpp.scave.charting.ChartProperties.PROP_AXIS_TITLE_FONT;
import static org.omnetpp.scave.charting.ChartProperties.PROP_GRAPH_TITLE_FONT;
import static org.omnetpp.scave.charting.ChartProperties.PROP_LABEL_FONT;
import static org.omnetpp.scave.charting.ChartProperties.PROP_LEGEND_FONT;
import static org.omnetpp.scave.charting.ChartProperties.PROP_X_AXIS_LOGARITHMIC;
import static org.omnetpp.scave.charting.ChartProperties.PROP_X_AXIS_MAX;
import static org.omnetpp.scave.charting.ChartProperties.PROP_X_AXIS_MIN;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.omnetpp.scave.charting.ChartProperties;
import org.omnetpp.scave.charting.ChartProperties.LegendAnchor;
import org.omnetpp.scave.charting.ChartProperties.LegendPosition;
import org.omnetpp.scave.engine.ResultFileManager;
import org.omnetpp.scave.model.Chart;
import org.omnetpp.scave.model.Property;
import org.omnetpp.scave.model.ScaveModelPackage;

/**
 * Edit form of charts.
 *
 * The properties of the chart are organized into groups
 * each group is displayed in a tab of the main tab folder.
 * 
 * @author tomi
 */
public class ChartEditForm implements IScaveObjectEditForm {
	public static final String TAB_MAIN = "Main";
	public static final String TAB_TITLES = "Titles";
	public static final String TAB_AXES = "Axes";
	public static final String TAB_LEGEND = "Legend";

	public static final String PROP_DEFAULT_TAB = "default-page";

	protected static final ScaveModelPackage pkg = ScaveModelPackage.eINSTANCE;

	/**
	 * Features edited on this form.
	 */
	private static final EStructuralFeature[] features = new EStructuralFeature[] {
		pkg.getChart_Name(),
		pkg.getChart_Properties(),
	};

	/**
	 * The edited chart.
	 */
	protected Chart chart;
	protected EObject parent;
	protected Map<String, Object> formParameters;
	protected ResultFileManager manager;
	protected ChartProperties properties;

	// controls
	private Text nameText;

	private Text graphTitleText;
	private Text graphTitleFontText;
	private Text xAxisTitleText;
	private Text yAxisTitleText;
	private Text axisTitleFontText;
	private Text labelFontText;
	private Combo xLabelsRotateByCombo;

	private Text xAxisMinText;
	private Text xAxisMaxText;
	private Text yAxisMinText;
	private Text yAxisMaxText;
	private Button xAxisLogCheckbox;
	private Button yAxisLogCheckbox;
	private Button invertAxesCheckbox;
	private Button showGridCheckbox;

	private Button displayLegendCheckbox;
	private Button displayBorderCheckbox;
	private Text legendFontText;
	private Button[] legendPositionRadios;
	private Button[] legendAnchorRadios;


	/**
	 * Number of visible items in combo boxes.
	 */
	protected static final int VISIBLE_ITEM_COUNT = 15;

	protected static final String UNSET = "(no change)";

	protected static final String USER_DATA_KEY = "ChartEditForm";

	public ChartEditForm(Chart chart, EObject parent, Map<String,Object> formParameters, ResultFileManager manager) {
		this.chart = chart;
		this.parent = parent;
		this.formParameters = formParameters;
		this.manager = manager;
		this.properties = ChartProperties.createPropertySource(chart, manager);
	}

	/**
	 * Returns the title displayed on the top of the dialog.
	 */
	public String getTitle() {
		return "Chart";
	}

	/**
	 * Returns the description displayed below the title.
	 */
	public String getDescription() {
		return "Modify properties of the chart.";
	}

	/**
	 * Returns the number of features on this form.
	 */
	public int getFeatureCount() {
		return getFeatures().length;
	}

	/**
	 * Returns the features edited on this form.
	 */
	public EStructuralFeature[] getFeatures() {
		return features;
	}

	/**
	 * Creates the controls of the dialog.
	 */
	public void populatePanel(Composite panel) {
		panel.setLayout(new GridLayout(1, false));
		TabFolder tabfolder = createTabFolder(panel);
		
		populateTabFolder(tabfolder);
		for (int i=0; i < tabfolder.getItemCount(); ++i)
			populateTabItem(tabfolder.getItem(i));
		
		// switch to the requested page 
		String defaultPage = (String) formParameters.get(PROP_DEFAULT_TAB);
		if (defaultPage != null)
			for (TabItem tabItem : tabfolder.getItems())
				if (tabItem.getText().equals(defaultPage)) {
					tabfolder.setSelection(tabItem); 
					break;
				}
	}
	
	/**
	 * Creates the tabs of the dialog.
	 */
	protected void populateTabFolder(TabFolder tabfolder) {
		createTab(TAB_MAIN, tabfolder, 2);
		createTab(TAB_TITLES, tabfolder, 1);
		createTab(TAB_AXES, tabfolder, 2);
		createTab(TAB_LEGEND, tabfolder, 1);
	}
	
	/**
	 * Creates the controls of the given tab.
	 */
	protected void populateTabItem(TabItem item) {
		Group group;
		String name = item.getText();
		Composite panel = (Composite)item.getControl();
		
		if (TAB_MAIN.equals(name)) {
			nameText = createTextField("Name", panel);
			nameText.setFocus();
		}
		else if (TAB_TITLES.equals(name)) {
			group = createGroup("Graph title", panel);
			graphTitleText = createTextField("Graph title", group);
			graphTitleFontText = createTextField("Title font", group);
			group = createGroup("Axis titles", panel);
			xAxisTitleText = createTextField("X axis title", group);
			yAxisTitleText = createTextField("Y axis title", group);
			axisTitleFontText = createTextField("Axis title font", group);
			labelFontText = createTextField("Label font", group);
			xLabelsRotateByCombo = createComboField("Rotate X labels by", group, new String[] {"0", "30", "45", "60", "90"});
		}
		else if (TAB_AXES.equals(name)) {
			group = createGroup("Axis bounds", panel, 3);
			group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			createLabel("", group);
			createLabel("Min", group);
			createLabel("Max", group);
			xAxisMinText = createTextField("X axis", group);
			xAxisMaxText = createTextField(null, group);
			yAxisMinText = createTextField("Y axis", group);
			yAxisMaxText = createTextField(null, group);
			group = createGroup("Axis options", panel, 1);
			xAxisLogCheckbox = createCheckboxField("Logarithmic X axis", group);
			yAxisLogCheckbox = createCheckboxField("Logarithmic Y axis", group);
			invertAxesCheckbox = createCheckboxField("Invert X,Y", group);
			group = createGroup("Grid", panel, 1);
			showGridCheckbox = createCheckboxField("Show grid", group);
		}
		else if (TAB_LEGEND.equals(name)) {
			displayLegendCheckbox = createCheckboxField("Display legend", panel);
			group = createGroup("Appearance", panel);
			displayBorderCheckbox = createCheckboxField("Border", group);
			displayBorderCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			legendFontText = createTextField("Legend font", group);
			legendPositionRadios = createRadioGroup("Position", panel, 3, LegendPosition.class, false);
			legendAnchorRadios = createRadioGroup("Anchoring", panel, 4, LegendAnchor.class, false);
		}
	}

	private TabFolder createTabFolder(Composite parent) {
		TabFolder tabfolder = new TabFolder(parent, SWT.NONE);
		tabfolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return tabfolder;

	}

	protected Composite createTab(String tabText, TabFolder tabfolder, int numOfColumns) {
		TabItem tabitem = new TabItem(tabfolder, SWT.NONE);
		tabitem.setText(tabText);
		Composite panel = new Composite(tabfolder, SWT.NONE);
		panel.setLayout(new GridLayout(numOfColumns, false));
		tabitem.setControl(panel);
		return panel;
	}

	protected Group createGroup(String text, Composite parent) {
		return createGroup(text, parent, 2);
	}

	protected Group createGroup(String text, Composite parent, int numOfColumns) {
		return createGroup(text, parent, 1, numOfColumns);
	}
	
	protected Group createGroup(String text, Composite parent, int colSpan, int numOfColumns) {
		Group group = new Group(parent, SWT.NONE);
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, colSpan, 1));
		group.setLayout(new GridLayout(numOfColumns, false));
		group.setText(text);
		return group;
	}

	protected Label createLabel(String text, Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(text);
		return label;
	}

	protected Text createTextField(String labelText, Composite parent) {
		if (labelText != null)
			createLabel(labelText, parent);
		Text text = new Text(parent, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return text;
	}

	protected Combo createComboField(String labelText, Composite parent, String[] items) {
		return createComboField(labelText, parent, items, false);
	}

	protected Combo createComboField(String labelText, Composite parent, String[] items, boolean optional) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(labelText);
		int style = SWT.BORDER; //type == null ? SWT.BORDER : SWT.BORDER | SWT.READ_ONLY;
		Combo combo = new Combo(parent, style);
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		combo.setVisibleItemCount(VISIBLE_ITEM_COUNT);
		combo.setItems(items);
		if (optional) combo.add(UNSET, 0);
		return combo;
	}

	protected Combo createComboField(String labelText, Composite parent, Class<? extends Enum<?>> type, boolean optional) {
		Enum<?>[] values = type.getEnumConstants();
		String[] items = new String[values.length];
		for (int i = 0; i < values.length; ++i)
			items[i] = values[i].name();
		return createComboField(labelText, parent, items, optional);
	}

	protected Button createCheckboxField(String labelText, Composite parent) {
		return createCheckboxField(labelText, parent, null);
	}
	
	protected Button createCheckboxField(String labelText, Composite parent, Object value) {
		Button checkbox = new Button(parent, SWT.CHECK);
		checkbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		checkbox.setText(labelText);
		checkbox.setData(USER_DATA_KEY, value);
		return checkbox;
	}

	protected Button createRadioField(String labelText, Composite parent, Object value) {
		Button radio = new Button(parent, SWT.RADIO);
		radio.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		radio.setText(labelText);
		radio.setData(USER_DATA_KEY, value);
		return radio;
	}

	protected Button[] createRadioGroup(String groupLabel, Composite parent, int numOfColumns, Class<? extends Enum<?>> type, boolean optional, String... radioLabels) {
		Group group = createGroup(groupLabel, parent, numOfColumns);
		Enum<?>[] values = type.getEnumConstants();
		int numOfRadios = optional ? values.length + 1 : values.length;
		Button[] radios = new Button[numOfRadios];
		int i = 0;
		if (optional) {
			radios[i++] = createRadioField(UNSET, group, null);
		}
		for (int j = 0; j < values.length; ++j) {
			Enum<?> value = values[j];
			String radioLabel = radioLabels != null && j < radioLabels.length ?	radioLabels[j] :
								value.name().toLowerCase();
			radios[i++] = createRadioField(radioLabel, group, value);
		}
		return radios;
	}

	/**
	 * Reads the value of the given feature from the corresponding control.
	 */
	public Object getValue(EStructuralFeature feature) {
		switch (feature.getFeatureID()) {
		case ScaveModelPackage.CHART__NAME:
			return nameText.getText();
		case ScaveModelPackage.CHART__PROPERTIES:
			ChartProperties newProps = ChartProperties.createPropertySource(chart, new ArrayList<Property>(), manager);
			collectProperties(newProps);
			return newProps.getProperties();
		}
		return null;
	}
	
	/**
	 * Sets the value of the given feature in the corresponding control.
	 */
	@SuppressWarnings("unchecked")
	public void setValue(EStructuralFeature feature, Object value) {
		switch (feature.getFeatureID()) {
		case ScaveModelPackage.CHART__NAME:
			nameText.setText(value == null ? "" : (String)value);
			break;
		case ScaveModelPackage.CHART__PROPERTIES:
			if (value != null) {
				List<Property> properties = (List<Property>)value;
				ChartProperties props = ChartProperties.createPropertySource(chart, properties, manager);
				setProperties(props);
			}
			break;
		}
	}

	/**
	 * Sets the properties in <code>newProps</code> from the values of the controls. 
	 */
	protected void collectProperties(ChartProperties newProps) {
		// Titles
		newProps.setGraphTitle(graphTitleText.getText());
		newProps.setProperty(PROP_GRAPH_TITLE_FONT, graphTitleFontText.getText()); // XXX font
		newProps.setXAxisTitle(xAxisTitleText.getText());
		newProps.setYAxisTitle(yAxisTitleText.getText());
		newProps.setProperty(PROP_AXIS_TITLE_FONT, axisTitleFontText.getText()); // XXX font
		newProps.setProperty(PROP_LABEL_FONT, labelFontText.getText()); // XXX font
		newProps.setXLabelsRotate(xLabelsRotateByCombo.getText());
		// Axes
		newProps.setProperty(PROP_X_AXIS_MIN, xAxisMinText.getText()); // XXX
		newProps.setProperty(PROP_X_AXIS_MAX, xAxisMaxText.getText()); // XXX
		newProps.setYAxisMin(yAxisMinText.getText());
		newProps.setYAxisMax(yAxisMaxText.getText());
		newProps.setProperty(PROP_X_AXIS_LOGARITHMIC, xAxisLogCheckbox.getSelection()); // XXX
		newProps.setYAxisLogarithmic(yAxisLogCheckbox.getSelection());
		newProps.setXYInvert(invertAxesCheckbox.getSelection());
		newProps.setXYGrid(showGridCheckbox.getSelection());
		// Legend
		newProps.setDisplayLegend(displayLegendCheckbox.getSelection());
		newProps.setLegendBorder(displayBorderCheckbox.getSelection());
		newProps.setProperty(PROP_LEGEND_FONT, legendFontText.getText()); // XXX font
		newProps.setLegendPosition(getSelection(legendPositionRadios, LegendPosition.class));
		newProps.setLegendAnchoring(getSelection(legendAnchorRadios, LegendAnchor.class));
	}

	/**
	 * Returns the selected radio button as the enum value it represents.
	 */
	@SuppressWarnings("unchecked")
	protected <T extends Enum<T>> T getSelection(Button[] radios, Class<T> type) {
		for (int i = 0; i < radios.length; ++i)
			if (radios[i].getSelection())
				return (T)radios[i].getData(USER_DATA_KEY);
		return null;
	}

	protected <T extends Enum<T>> T getSelection(Combo combo, Class<T> type) {
		T[] values = type.getEnumConstants();
		String selection = combo.getText();
		for (int i = 0; i < values.length; ++i)
			if (values[i].name().equals(selection))
				return values[i];
		return null;
	}

	protected String getSelection(Combo combo) {
		String text = combo.getText();
		return UNSET.equals(text) ? null : text;
	}

	/**
	 * Sets the values of the controls from the given <code>props</code>.
	 * @param props
	 */
	protected void setProperties(ChartProperties props) {
		// Titles
		graphTitleText.setText(props.getGraphTitle());
		graphTitleFontText.setText(props.getStringProperty(PROP_GRAPH_TITLE_FONT)); // XXX font
		xAxisTitleText.setText(props.getXAxisTitle());
		yAxisTitleText.setText(props.getYAxisTitle());
		axisTitleFontText.setText(props.getStringProperty(PROP_AXIS_TITLE_FONT)); // XXX font
		labelFontText.setText(props.getStringProperty(PROP_LABEL_FONT)); // XXX font
		xLabelsRotateByCombo.setText(props.getXLabelsRotate());
		// Axes
		xAxisMinText.setText(props.getStringProperty(PROP_X_AXIS_MIN)); // XXX for vector chart only
		xAxisMaxText.setText(props.getStringProperty(PROP_X_AXIS_MAX)); // XXX for vector chart only
		yAxisMinText.setText(props.getYAxisMin());
		yAxisMaxText.setText(props.getYAxisMax());
		xAxisLogCheckbox.setSelection(props.getBooleanProperty(PROP_X_AXIS_LOGARITHMIC)); // XXX for vector charts only
		yAxisLogCheckbox.setSelection(props.getYAxisLogarithmic());
		invertAxesCheckbox.setSelection(props.getXYInvert());
		showGridCheckbox.setSelection(props.getXYGrid());
		// Legend
		displayLegendCheckbox.setSelection(props.getDisplayLegend());
		displayBorderCheckbox.setSelection(props.getLegendBorder());
		legendFontText.setText(props.getStringProperty(PROP_LEGEND_FONT)); // XXX font
		setSelection(legendPositionRadios, props.getLegendPosition());
		setSelection(legendAnchorRadios, props.getLegendAnchoring());
	}

	/**
	 * Select the radio button representing the enum value.
	 */
	protected void setSelection(Button[] radios, Enum<?> value) {
		for (int i = 0; i < radios.length; ++i)
			radios[i].setSelection(radios[i].getData(USER_DATA_KEY) == value);
	}

	protected void setSelection(Combo combo, Enum<?> value) {
		if (value != null)
			combo.setText(value.name());
		else
			combo.setText(UNSET);
	}

	protected void setSelection(Combo combo, String value) {
		if (value != null && value.length() > 0)
			combo.setText(value);
		else
			combo.setText(UNSET);
	}

}
