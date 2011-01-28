/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.inifile.editor.model;

import static org.omnetpp.inifile.editor.model.ConfigRegistry.CFGID_EXTENDS;
import static org.omnetpp.inifile.editor.model.ConfigRegistry.CFGID_NETWORK;
import static org.omnetpp.inifile.editor.model.ConfigRegistry.CFGID_VECTOR_RECORDING_INTERVALS;
import static org.omnetpp.inifile.editor.model.ConfigRegistry.CONFIG_;
import static org.omnetpp.inifile.editor.model.ConfigRegistry.EXTENDS;
import static org.omnetpp.inifile.editor.model.ConfigRegistry.GENERAL;
import static org.omnetpp.ned.model.NedElementConstants.NED_PARTYPE_BOOL;
import static org.omnetpp.ned.model.NedElementConstants.NED_PARTYPE_DOUBLE;
import static org.omnetpp.ned.model.NedElementConstants.NED_PARTYPE_INT;
import static org.omnetpp.ned.model.NedElementConstants.NED_PARTYPE_STRING;
import static org.omnetpp.ned.model.NedElementConstants.NED_PARTYPE_XML;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.ResettableIterator;
import org.apache.commons.lang.text.StrTokenizer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.omnetpp.common.Debug;
import org.omnetpp.common.collections.ProductIterator;
import org.omnetpp.common.engine.Common;
import org.omnetpp.common.engine.UnitConversion;
import org.omnetpp.common.markers.ProblemMarkerSynchronizer;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.inifile.editor.InifileEditorPlugin;
import org.omnetpp.inifile.editor.model.IInifileDocument.LineInfo;
import org.omnetpp.inifile.editor.model.ParamResolution.ParamResolutionType;
import org.omnetpp.ned.core.IModuleTreeVisitor;
import org.omnetpp.ned.core.NedResourcesPlugin;
import org.omnetpp.ned.core.NedTreeTraversal;
import org.omnetpp.ned.core.ParamUtil;
import org.omnetpp.ned.model.NedTreeUtil;
import org.omnetpp.ned.model.ex.ParamElementEx;
import org.omnetpp.ned.model.ex.PropertyElementEx;
import org.omnetpp.ned.model.ex.SubmoduleElementEx;
import org.omnetpp.ned.model.interfaces.IModuleTypeElement;
import org.omnetpp.ned.model.interfaces.INedTypeElement;
import org.omnetpp.ned.model.interfaces.INedTypeInfo;
import org.omnetpp.ned.model.interfaces.INedTypeResolver;
import org.omnetpp.ned.model.interfaces.ISubmoduleOrConnection;
import org.omnetpp.ned.model.pojo.ParamElement;

/**
 * This is a layer above IInifileDocument, and contains info about the
 * relationship of inifile contents and NED. For example, which inifile
 * parameter settings apply to which NED module parameters.
 *
 * Implementation note: there are several synchronized(doc) { } blocks in the
 * code. This is necessary because e.g. we need to prevent InifileDocument from
 * getting re-parsed while we are analyzing it. In particular, any two of the
 * following threads may collide: reconciler, content assist, update of the
 * Module Parameters view.
 *
 * @author Andras
 */
//XXX TODO: consider making a copy of the inifile, and analyze that from a background thread! This would eliminate UI lockup during analyzing.
//XXX Issue. Consider the following:
//  **.ppp[1].queueType = "Foo"
//  **.ppp[*].queueType = "DropTailQueue"
//  **.ppp[*].queue.frameCapacity = 10     ===> "unused entry", although DropTailQueue has that parameter
// because the queue type resolves to "Foo", and "DropTailQueue" is not considered.
// How to fix this? Maybe: resolveLikeType() returns a set of potential types
// (ie both "Foo" and "DropTailQueue", and NedTreeTraversal should recurse with **all** of them?
//
public class InifileAnalyzer {
	public static final String INIFILEANALYZERPROBLEM_MARKER_ID = InifileEditorPlugin.PLUGIN_ID + ".inifileanalyzerproblem";
	private IInifileDocument doc;
	private boolean changed = true;
	private Set<String> sectionsCausingCycles;
	private ProblemMarkerSynchronizer markerSynchronizer; // only used during analyze()

	// InifileDocument, InifileAnalyzer, and NedResources are all accessed from
	// background threads (must be synchronized), and the analyze procedure needs
	// NedResources -- so use NedResources as lock to prevent deadlocks
	private Object lock = NedResourcesPlugin.getNedResources();

	// too speed up validating of values. Matches boolean, number+unit, string literal
	private static final Pattern SIMPLE_EXPRESSION_REGEX = Pattern.compile("true|false|(-?\\d+(\\.\\d+)?\\s*[a-zA-Z]*)|\"[^\"]*\"");

	/**
	 * Classifies inifile keys; see getKeyType().
	 */
	public enum KeyType {
		CONFIG, // contains no dot (like sim-time-limit=, etc)
		PARAM,  // contains dot, but no hyphen: parameter setting (like **.app.delay)
		PER_OBJECT_CONFIG; // dotted, and contains hyphen (like **.partition-id, rng mapping, vector configuration, etc)
	};

    /**
     * Used internally: an iteration variable definition "${...}", stored as part of SectionData
     */
	static class IterationVariable {
        String varname; // printable variable name ("x"); null for an unnamed variable
        String value;   // "1,2,5..10"; never empty
        String parvar;  // "in parallel to" variable", as in the ${1,2,5..10 ! var} notation
        String section; // section where it was defined
        String key;     // key where it was defined
    };

    /**
	 * Used internally: class of objects attached to IInifileDocument entries
	 * (see getKeyData()/setKeyData())
	 */
	static class KeyData {
		List<ParamResolution> paramResolutions = new ArrayList<ParamResolution>();
	};

	/**
	 * Used internally: class of objects attached to IInifileDocument sections
	 * (see getSectionData()/setSectionData())
	 */
	static class SectionData {
		List<ParamResolution> allParamResolutions = new ArrayList<ParamResolution>();
		List<ParamResolution> unassignedParams = new ArrayList<ParamResolution>(); // subset of allParamResolutions
		List<ParamResolution> implicitlyAssignedParams = new ArrayList<ParamResolution>(); // subset of allParamResolutions
		List<PropertyResolution> propertyResolutions = new ArrayList<PropertyResolution>();
		List<IterationVariable> iterations = new ArrayList<IterationVariable>();
		Map<String,IterationVariable> namedIterations = new HashMap<String, IterationVariable>();
	}


	/**
	 * Constructor.
	 */
	public InifileAnalyzer(IInifileDocument doc) {
		this.doc = doc;

		// hook on inifile changes (unhooking is not necessary, because everything
		// will be gc'd when the editor closes)
		doc.addInifileChangeListener(new IInifileChangeListener() {
			public void modelChanged() {
				InifileAnalyzer.this.modelChanged();
			}
		});
	}

	protected void modelChanged() {
		synchronized (lock) {
			changed = true;
		}
	}

	/**
	 * Returns the underlying inifile document that gets analyzed.
	 */
	public IInifileDocument getDocument() {
		return doc;
	}

	/**
	 * Analyzes the inifile if it changed since last analyzed. Side effects:
	 * error/warning markers may be placed on the IFile, and parameter
	 * resolutions (see ParamResolution) are recalculated.
	 */
	public void analyzeIfChanged() {
		synchronized (lock) {
			if (changed)
				analyze();
		}
	}

	/**
	 * Analyzes the inifile. Side effects: error/warning markers may be placed
	 * on the IFile, and parameter resolutions (see ParamResolution) are
	 * recalculated.
	 */
	public void analyze() {
		synchronized (lock) {
			long startTime = System.currentTimeMillis();
			INedTypeResolver ned = NedResourcesPlugin.getNedResources();

			// collect errors/warnings in a ProblemMarkerSynchronizer
			markerSynchronizer = new ProblemMarkerSynchronizer(INIFILEANALYZERPROBLEM_MARKER_ID);
			markerSynchronizer.register(doc.getDocumentFile());
			for (IFile file : doc.getIncludedFiles())
				markerSynchronizer.register(file);

			//XXX catch all exceptions during analyzing, and set changed=false in finally{} ?

			// calculate parameter resolutions for each section
			// note: almost all time analyze() takes is spent in this method -- optimize here if slow
			calculateParamResolutions(ned);

			// collect iteration variables
			collectIterationVariables();

			// data structure is done
			changed = false;

			// check section names, detect cycles in the fallback chains
			validateSections();

			// validate config entries and parameter keys; this must be done AFTER changed=false
			for (String section : doc.getSectionNames()) {
				for (String key : doc.getKeys(section)) {
					switch (getKeyType(key)) {
					case CONFIG: validateConfig(section, key, ned); break;
					case PARAM:  validateParamKey(section, key, ned); break;
					case PER_OBJECT_CONFIG: validatePerObjectConfig(section, key, ned); break;
					}
				}
			}

			// make sure that an iteration variable isn't redefined in other sections
			for (String section : doc.getSectionNames()) {
				String[] sectionChain = InifileUtils.resolveSectionChain(doc, section);
				Map<String, IterationVariable> namedIterations = ((SectionData) doc.getSectionData(section)).namedIterations;
				for (String var : namedIterations.keySet())
					for (String ancestorSection : sectionChain)
						if (!section.equals(ancestorSection))
							if (((SectionData) doc.getSectionData(ancestorSection)).namedIterations.containsKey(var))
								addError(section, namedIterations.get(var).key, "Redeclaration of iteration variable $"+var+", originally defined in section ["+ancestorSection+"]");
			}

			// warn for unused param keys; this must be done AFTER changed=false
			for (String section : doc.getSectionNames())
				for (String key : getUnusedParameterKeys(section))
					addWarning(section, key, "Unused entry (does not match any parameters)");

			Debug.println("Inifile analyzed in "+(System.currentTimeMillis()-startTime)+"ms");

			// synchronize detected problems with the file's existing markers
			markerSynchronizer.runAsWorkspaceJob();
			markerSynchronizer = null;
		}
	}

	protected void addError(String section, String message) {
		LineInfo line = doc.getSectionLineDetails(section);
		addMarker(line.getFile(), INIFILEANALYZERPROBLEM_MARKER_ID, IMarker.SEVERITY_ERROR, message, line.getLineNumber());
	}

	protected void addError(String section, String key, String message) {
		LineInfo line = doc.getEntryLineDetails(section, key);
		addMarker(line.getFile(), INIFILEANALYZERPROBLEM_MARKER_ID, IMarker.SEVERITY_ERROR, message, line.getLineNumber());
	}

	protected void addWarning(String section, String message) {
		LineInfo line = doc.getSectionLineDetails(section);
		addMarker(line.getFile(), INIFILEANALYZERPROBLEM_MARKER_ID, IMarker.SEVERITY_WARNING, message, line.getLineNumber());
	}

	protected void addWarning(String section, String key, String message) {
		LineInfo line = doc.getEntryLineDetails(section, key);
		addMarker(line.getFile(), INIFILEANALYZERPROBLEM_MARKER_ID, IMarker.SEVERITY_WARNING, message, line.getLineNumber());
	}

	protected void addInfo(String section, String message) {
	    LineInfo line = doc.getSectionLineDetails(section);
	    addMarker(line.getFile(), INIFILEANALYZERPROBLEM_MARKER_ID, IMarker.SEVERITY_INFO, message, line.getLineNumber());
	}

	protected void addInfo(String section, String key, String message) {
	    LineInfo line = doc.getEntryLineDetails(section, key);
	    addMarker(line.getFile(), INIFILEANALYZERPROBLEM_MARKER_ID, IMarker.SEVERITY_INFO, message, line.getLineNumber());
	}

	protected void addMarker(final IFile file, final String type, int severity, String message, int line) {
	    Map<String, Object> map = new HashMap<String, Object>();
		map.put(IMarker.SEVERITY, severity);
		map.put(IMarker.LINE_NUMBER, line);
		map.put(IMarker.MESSAGE, message);
		markerSynchronizer.addMarker(file, type, map);
	}

	/**
	 * Check section names, and detect cycles in the fallback sequences ("extends=")
	 */
	protected void validateSections() {
		sectionsCausingCycles = new HashSet<String>();

		// check section names and extends= everywhere
		if (doc.getValue(GENERAL, EXTENDS) != null)
			addError(GENERAL, EXTENDS, "The extends= key cannot occur in the [General] section");

		for (String section : doc.getSectionNames()) {
			if (!section.equals(GENERAL)) {
				if (!section.startsWith(CONFIG_))
					addError(section, "Invalid section name: must be [General] or [Config <name>]");
				else if (section.contains("  "))
					addError(section, "Invalid section name: contains too many spaces");
                else if (!String.valueOf(section.charAt(CONFIG_.length())).matches("[a-zA-Z_]"))
					addError(section, "Invalid section name: config name must begin a letter or underscore");
				else if (!section.matches("[^ ]+ [a-zA-Z0-9_@-]+"))
					addError(section, "Invalid section name: contains illegal character(s)");
				String extendsName = doc.getValue(section, EXTENDS);
				if (extendsName != null && !doc.containsSection(CONFIG_+extendsName))
					addError(section, EXTENDS, "No such section: [Config "+extendsName+"]");
			}
		}

		// check fallback chain for every section
		for (String section : doc.getSectionNames()) {
			// follow section fallback sequence, and detect cycles in it
			Set<String> sectionChain = new HashSet<String>();
			String currentSection = section;
			while (true) {
				sectionChain.add(currentSection);
				currentSection = InifileUtils.resolveBaseSection(doc, currentSection);
				if (currentSection==null)
					break; // [General] reached
				if (sectionChain.contains(currentSection)) {
					sectionsCausingCycles.add(currentSection);
					break; // error: cycle in the fallback chain
				}
			}
		}

		// add error markers
		for (String section : sectionsCausingCycles)
			addError(section, "Cycle in the fallback chain at section ["+section+"]");
	}

	/**
	 * Validate a configuration entry (key+value) using ConfigRegistry.
	 */
	protected void validateConfig(String section, String key, INedTypeResolver ned) {
		// check key and if it occurs in the right section
		ConfigOption e = ConfigRegistry.getOption(key);
		if (e == null) {
		    if (!key.matches("[a-zA-Z0-9-_]+"))
		        addError(section, key, "Syntax error in configuration key: "+key);
		    else
		        addError(section, key, "Unknown configuration key: "+key);
			return;
		}
		else if (e.isGlobal() && !section.equals(GENERAL)) {
			addError(section, key, "Key \""+key+"\" can only be specified globally, in the [General] section");
		}
		else if (key.equals(CFGID_NETWORK.getName()) && !section.equals(GENERAL)) {
			// it does not make sense to override "network=" in another section, warn for it
			String[] sectionChain = InifileUtils.resolveSectionChain(doc, section);
			for (String sec : sectionChain)
				if (!sec.equals(section) && doc.containsKey(sec, key))
					addWarning(section, key, "Network is already specified in section ["+sec+"], as \""+doc.getValue(sec, key)+"\"");
		}

		// check value
		String value = doc.getValue(section, key);

		// if it contains "${...}" variables, check that those variables exist. Any more
		// validation would be significantly more complex, and not done at the moment
		if (value.indexOf('$') != -1) {
			if (validateValueWithIterationVars(section, key, value))
				return;
		}

		// check if value has the right type
		String errorMessage = validateConfigValueByType(value, e);
		if (errorMessage != null) {
			addError(section, key, errorMessage);
			return;
		}

		if (e.getDataType()==ConfigOption.DataType.CFG_STRING && value.startsWith("\""))
			value =	Common.parseQuotedString(value); // cannot throw exception: value got validated above

		// check validity of some settings, like network=, preload-ned-files=, etc
		if (e==CFGID_EXTENDS) {
			// note: we do not validate "extends=" here -- that's all done in validateSections()!
		}
		else if (e==CFGID_NETWORK) {
			INedTypeInfo network = resolveNetwork(ned, value);
			if (network == null) {
				addError(section, key, "No such NED type: "+value);
				return;
			}
			INedTypeElement node = network.getNedElement();
            if (!(node instanceof IModuleTypeElement)) {
                addError(section, key, "Not a module type: "+value);
                return;
            }
			if (!((IModuleTypeElement)node).isNetwork()) {
				addError(section, key, "Module type '"+value+"' was not declared to be a network");
				return;
			}
		}
	}

	public INedTypeInfo resolveNetwork(INedTypeResolver ned, String value) {
		INedTypeInfo network = null;
		IFile iniFile = doc.getDocumentFile();
		String inifilePackage = ned.getExpectedPackageFor(iniFile);
		IProject contextProject = iniFile.getProject();
		if (inifilePackage != null && value != null) {
			String networkName = inifilePackage + (inifilePackage.length()!=0 && value.length()!=0 ? "." : "")+value;
			network = ned.getToplevelNedType(networkName, contextProject);
		}
		if (network == null)
			network = ned.getToplevelNedType(value, contextProject);

		return network;
	}

	private final static Pattern
		DOLLAR_BRACES_PATTERN = Pattern.compile("\\$\\{\\s*(.*?)\\s*\\}"),      // ${...}
		VARIABLE_DEFINITION_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9@_-]*?\\s*=\\s*(.*)"), // name = values
		VARIABLE_REFERENCE_PATTERN = Pattern.compile("([a-zA-Z_][a-zA-Z0-9@_-]*)"),             // name only
		START_END_STEP_VALUE_PATTERN = Pattern.compile("(.*?)\\s*\\.\\.\\s*(.*?)\\s*step\\s*(.*)"),
		START_END_VALUE_PATTERN = Pattern.compile("(.*?)\\s*\\.\\.\\s*(.*?)"),
		ANY_VALUE_PATTERN = Pattern.compile("(.*)");


	protected boolean validateValueWithIterationVars(String section, String key, String value) {
		Matcher iterationVarMatcher = DOLLAR_BRACES_PATTERN.matcher(value);

		// check referenced iteration variables
		boolean foundAny = false;
		boolean validateValues = true;
		while (iterationVarMatcher.find()) {
			foundAny = true;

			Matcher variableReferenceMatcher = VARIABLE_REFERENCE_PATTERN.matcher(iterationVarMatcher.group(1));
			if (variableReferenceMatcher.matches()) {
				String varName = variableReferenceMatcher.group(1);
				if (!isValidIterationVariable(section, varName))
					addError(section, key, "${"+varName+"} is undefined");
				validateValues = false; // because references are not followed
			}
		}

		// validate the first 100 values that come from iterating the constants in the variable definitions
		if (foundAny && validateValues) {
			IterationVariablesIterator values = new IterationVariablesIterator(value);
			int count = 0;
			while (values.hasNext() && count < 100) {
				String v = values.next();
				//System.out.format("Validating: %s%n", v);
				validateParamKey(section, key, v);
				count++;
			}
		}

		return foundAny;
	}

	protected boolean isValidIterationVariable(String section, String varName) {
	    // is it a predefined variable like ${configname}?
	    if (Arrays.asList(ConfigRegistry.getConfigVariableNames()).contains(varName))
	        return true;

	    // is it defined in this section or any fallback section?
        String[] sectionChain = InifileUtils.resolveSectionChain(doc, section);
        for (String sec : sectionChain) {
            SectionData sectionData = (SectionData) doc.getSectionData(sec);
            if (sectionData.namedIterations.containsKey(varName))
                return true;
        }
        return false;
    }

	/**
	 * Iterates on the values, that comes from the substitutions of iteration variables
	 * with the constants found in their definition.
	 *
	 * Example: "x${i=1..5}y${2 .. 8 step 3}" gives ["x1y2", "x5y2", "x1y8", "x2y8", "x1y3", "x2y3"].
	 *
	 * Note: variable references are not followed, therefore the iterator will be empty
	 *       if the parameter value contained referenced variables.
	 */
	static class IterationVariablesIterator implements ResettableIterator
	{
		String value;
		List<Object> format;
		ResettableIterator iterator;
		StringBuilder sb;

		public IterationVariablesIterator(String value) {
			this.value = value;
			this.format = new ArrayList<Object>();
			this.sb = new StringBuilder(100);

			List<String> tokens = StringUtils.splitPreservingSeparators(value, DOLLAR_BRACES_PATTERN);
			List<ResettableIterator> valueIterators = new ArrayList<ResettableIterator>();
			int i = 0;
			for (String token : tokens) {
				if (i % 2 == 0)
					format.add(token);
				else {
					format.add(valueIterators.size());
					valueIterators.add(new IterationVariableIterator(token));
				}
				i++;
			}

			iterator = new ProductIterator(valueIterators.toArray(new ResettableIterator[valueIterators.size()]));
		}

		public void reset() {
			iterator.reset();
		}

		public boolean hasNext() {
			return iterator.hasNext();
		}

		public String next() {
			Object[] values = (Object[])iterator.next();
			if (values == null)
				return null;

			sb.setLength(0);
			for (Object obj : format) {
				if (obj instanceof String)
					sb.append(obj);
				else if (obj instanceof Integer) {
					int index = (Integer)obj;
					sb.append(values[index]);
				}
			}
			return sb.toString();
		}

		public void remove() {
			iterator.remove();
		}
	}

	/**
	 * Iterates on the constants in one iteration variable.
	 *
	 * Example: ${x=1,3..10 step 2} gives [1,3,10,2].
	 *
	 */
	static class IterationVariableIterator implements ResettableIterator
	{
		StrTokenizer tokenizer;
		Matcher matcher;
		int groupIndex;

		public IterationVariableIterator(String iteration) {
			Matcher m = DOLLAR_BRACES_PATTERN.matcher(iteration);
			if (!m.matches())
				throw new IllegalArgumentException("Illegal iteration");

			String content = m.group(1);
			String values;
			if ((m = VARIABLE_DEFINITION_PATTERN.matcher(content)).matches())
				values = m.group(1);
			else if ((m=VARIABLE_REFERENCE_PATTERN.matcher(content)).matches())
				// TODO follow the reference?
				values = "";
			else // anonymous iteration
				values = content;
			tokenizer = StrTokenizer.getCSVInstance(values);
		}

		public void reset() {
			tokenizer.reset();
			matcher = null;
			groupIndex = 0;
		}

		public boolean hasNext() {
			return tokenizer.hasNext() || matcher != null && groupIndex <= matcher.groupCount();
		}

		public Object next() {
			if (matcher == null || groupIndex > matcher.groupCount()) {
				if (!tokenizer.hasNext())
					return null;
				String token = tokenizer.nextToken();
				match(token);
			}
			if (matcher != null && groupIndex <= matcher.groupCount()) {
				String next = matcher.group(groupIndex);
				groupIndex++;
				return next;
			}
			return null;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		private boolean match(String token) {
			if (matcher == null)
				matcher = START_END_STEP_VALUE_PATTERN.matcher(token);
			else {
				matcher.reset(token);
				matcher.usePattern(START_END_STEP_VALUE_PATTERN);
			}
			groupIndex = 1;
			if (matcher.matches())
				return true;
			matcher.usePattern(START_END_VALUE_PATTERN);
			if (matcher.matches())
				return true;
			matcher.usePattern(ANY_VALUE_PATTERN);
			if (matcher.matches())
				return true;
			return false;
		}
	}

	/**
	 * Validate a configuration entry's value.
	 */
	protected static String validateConfigValueByType(String value, ConfigOption e) {
		switch (e.getDataType()) {
		case CFG_BOOL:
			if (!value.equals("true") && !value.equals("false"))
				return "Value should be a boolean constant: true or false";
			break;
		case CFG_INT:
			try {
				Integer.parseInt(value);
			} catch (NumberFormatException ex) {
				return "Value should be an integer constant";
			}
			break;
		case CFG_DOUBLE:
			if (e.getUnit()==null) {
				try {
					Double.parseDouble(value);
				} catch (NumberFormatException ex) {
					return "Value should be a numeric constant (a double)";
				}
			}
			else {
				try {
					UnitConversion.parseQuantity(value, e.getUnit());
				} catch (RuntimeException ex) {
					return StringUtils.capitalize(ex.getMessage());
				}
			}
			break;
		case CFG_STRING:
			try {
				if (value.startsWith("\""))
					Common.parseQuotedString(value);  //XXX wrong: what if it's an expression like "foo"+"bar" ?
			} catch (RuntimeException ex) {
				return "Error in string constant: "+ex.getMessage();
			}
			break;
		case CFG_FILENAME:
		case CFG_FILENAMES:
		case CFG_PATH:
			//XXX
			break;
		}
		return null;
	}

	protected void validateParamKey(String section, String key, INedTypeResolver ned) {
		String value = doc.getValue(section, key).trim();
		validateParamKey(section, key, value);
	}

	protected void validateParamKey(String section, String key, String value) {
		// value cannot be empty
		if (value.equals("")) {
			addError(section, key, "Value cannot be empty");
			return;
		}

		// if value contains "${...}" variables, check that those variables exist. Any more
		// validation would be significantly more complex, and not done at the moment
		if (DOLLAR_BRACES_PATTERN.matcher(value).find()) {
		    if (validateValueWithIterationVars(section, key, value))
		        return;
		}

		if (value.equals(ConfigRegistry.DEFAULT) || value.equals(ConfigRegistry.ASK)) {
		    // nothing to check, actually
		}
		else {
		    // check syntax. note: regex is faster in most cases than parsing
		    if (!SIMPLE_EXPRESSION_REGEX.matcher(value).matches() && !NedTreeUtil.isExpressionValid(value)) {
                addError(section, key, "Syntax error in expression");
                return;
		    }

		    // check parameter data types are consistent with each other
		    ParamResolution[] resList = getParamResolutionsForKey(section, key);
		    int paramType = -1;
		    for (ParamResolution res : resList) {
		        if (paramType == -1)
		            paramType = res.paramDeclaration.getType();
		        else if (paramType != res.paramDeclaration.getType()) {
		            addError(section, key, "Entry matches parameters of different data types");
		            return;
		        }
		    }

		    // check units are consistent with each other
		    String paramUnit = null;
		    for (ParamResolution res : resList) {
		        PropertyElementEx unitProperty = res.paramDeclaration.getLocalProperties().get("unit");
		        String unit = unitProperty==null ? "" : StringUtils.nullToEmpty(unitProperty.getSimpleValue());
		        if (paramUnit == null)
		            paramUnit = unit;
		        else if (!paramUnit.equals(unit)) {
		            addError(section, key, "Entry matches parameters with different units: " +
		                    (paramUnit.equals("") ? "none" : paramUnit) + ", " + (unit.equals("") ? "none" : unit));
		            return;
		        }
		    }

		    // check value is consistent with the data type
		    if (paramType != -1) {
		        // determine value's data type
		        int valueType = -1;
		        String valueUnit = null;
		        if (value.equals("true") || value.equals("false"))
		            valueType = NED_PARTYPE_BOOL;
		        else if (value.startsWith("\""))
		            valueType = NED_PARTYPE_STRING;
		        else if (value.startsWith("xmldoc"))
		            valueType = NED_PARTYPE_XML;
		        else {
		            try {
		                valueUnit = UnitConversion.parseQuantityForUnit(value); // throws exception if not a quantity
		                Assert.isNotNull(valueUnit);
		            } catch (RuntimeException e) {}

		            if (valueUnit != null)
		                valueType = NED_PARTYPE_DOUBLE;
		        }

		        // provided we could figure out the value's data type, check it's the same as parameter's data type
		        int tmpParamType = paramType==NED_PARTYPE_INT ? NED_PARTYPE_DOUBLE : paramType; // replace "int" with "double"
		        if (valueType != -1 && valueType != tmpParamType) {
		            String typeName = resList[0].paramDeclaration.getAttribute(ParamElement.ATT_TYPE);
		            addError(section, key, "Wrong data type: "+typeName+" expected");
		        }

		        // if value is numeric, check units
		        if (valueUnit!=null) {
		            try {
		                UnitConversion.parseQuantity(value, paramUnit); // throws exception on incompatible units
		            }
		            catch (RuntimeException e) {
		                addError(section, key, e.getMessage());
		            }
		        }

		        // mark line if value is the same as the NED default
		        Assert.isTrue(resList.length > 0);
	            boolean allAreIniNedDefault = true;
	            for (ParamResolution res : resList)
	                if (res.type != ParamResolutionType.INI_NEDDEFAULT)
	                    allAreIniNedDefault = false;
	            if (allAreIniNedDefault)
	                addInfo(section, key, "Value is same as the NED default");
		    }
        }
	}

	protected void validatePerObjectConfig(String section, String key, INedTypeResolver ned) {
		Assert.isTrue(key.lastIndexOf('.') > 0);
		String configName = key.substring(key.lastIndexOf('.')+1);
		ConfigOption e = ConfigRegistry.getPerObjectEntry(configName);
		if (e == null) {
            if (!configName.matches("[a-zA-Z0-9-_]+"))
                addError(section, key, "Syntax error in per-object configuration key: "+configName);
            else
                addError(section, key, "Unknown per-object configuration key: "+configName);
			return;
		}
		else if (e.isGlobal() && !section.equals(GENERAL)) {
			addError(section, key, "Per-object configuration \""+configName+"\" can only be specified globally, in the [General] section");
		}

		// check value
		String value = doc.getValue(section, key);

		// if it contains "${...}" variables, check that those variables exist. Any more
		// validation would be significantly more complex, and not done at the moment
		if (value.indexOf('$') != -1) {
			if (validateValueWithIterationVars(section, key, value))
				return;
		}

		// check if value has the right type
		String errorMessage = validateConfigValueByType(value, e);
		if (errorMessage != null) {
			addError(section, key, errorMessage);
			return;
		}

		if (e.getDataType()==ConfigOption.DataType.CFG_STRING && value.startsWith("\""))
			value =	Common.parseQuotedString(value); // cannot throw exception: value got validated above

		// check validity of some settings, like record-interval=, etc
		if (e==CFGID_VECTOR_RECORDING_INTERVALS) {
			// validate syntax
			StringTokenizer tokenizer = new StringTokenizer(value, ",");
			while (tokenizer.hasMoreTokens()) {
				String interval = tokenizer.nextToken();
				if (!interval.contains(".."))
					addError(section, key, "Syntax error in output vector intervals");
				else {
					try {
						String from = StringUtils.substringBefore(interval, "..").trim();
						String to = StringUtils.substringAfter(interval, "..").trim();
						if (!from.equals("") && !from.contains("${"))
							Double.parseDouble(from);  // check format
						if (!to.equals("") && !to.contains("${"))
							Double.parseDouble(to);  // check format
					}
					catch (NumberFormatException ex) {
						addError(section, key, "Syntax error in output vector interval");
					}
				}
			}
		}
	}

	protected void collectIterationVariables() {
		for (String section : doc.getSectionNames()) {
			SectionData sectionData = (SectionData) doc.getSectionData(section);
			sectionData.namedIterations.clear();
			for (String key : doc.getKeys(section))
				if (doc.getValue(section, key).indexOf('$') != -1)
					parseIterationVariables(section, key);
		}
	}

	protected void parseIterationVariables(String section, String key) {
		Pattern p = Pattern.compile(
				"\\$\\{" +   // opening dollar+brace
				"(\\s*([a-zA-Z0-9@_-]+)" + // variable name (opt)
				"\\s*=)?" +  // equals (opt)
				"\\s*(.*?)" +  // value string
				"\\s*(!\\s*([a-zA-Z0-9@_-]+))?" + // optional trailing "! variable"
				"\\s*\\}");  // closing brace

		String value = doc.getValue(section, key);
		Matcher m = p.matcher(value);
		SectionData sectionData = (SectionData) doc.getSectionData(section);

		// find all occurrences of the pattern in the input string
		while (m.find()) {
			IterationVariable v = new IterationVariable();
			v.varname = m.group(2);
			v.value = m.group(3);
			v.parvar = m.group(5);
			v.section = section;
			v.key = key;
			//Debug.println("itervar found: $"+v.varname+" = ``"+v.value+"'' ! "+v.parvar);
			if (Arrays.asList(ConfigRegistry.getConfigVariableNames()).contains(v.varname))
				addError(section, key, "${"+v.varname+"} is a predefined variable and cannot be changed");
			else if (sectionData.namedIterations.containsKey(v.varname))
				// Note: checking that it doesn't redefine a variable in a base section can only be done
				// elsewhere, after all sections have been processed
				addError(section, key, "Redefinition of iteration variable ${"+v.varname+"}");
			else {
				sectionData.iterations.add(v);
				if (v.varname != null)
					sectionData.namedIterations.put(v.varname, v);
			}
		}
	}

	/**
	 * Calculate how parameters get assigned when the given section is the active one.
	 */
	protected void calculateParamResolutions(INedTypeResolver ned) {
		// initialize SectionData and KeyData objects
		for (String section : doc.getSectionNames()) {
			doc.setSectionData(section, new SectionData());
			for (String key : doc.getKeys(section))
				if (getKeyType(key)!=KeyType.CONFIG)
					doc.setKeyData(section, key, new KeyData());
		}

		// calculate parameter resolutions for each section
		for (String activeSection : doc.getSectionNames()) {
			// calculate param resolutions
			List<ParamResolution> paramResoultions = collectParameters(activeSection);
			List<PropertyResolution> propertyResolutions = collectSignalResolutions(activeSection);

			// store with the section the list of all parameter resolutions (including unassigned params)
			// store with every key the list of parameters it resolves
			for (ParamResolution res : paramResoultions) {
				SectionData sectionData = ((SectionData)doc.getSectionData(activeSection));
				sectionData.propertyResolutions = propertyResolutions;
				sectionData.allParamResolutions.add(res);
				if (res.type == ParamResolutionType.UNASSIGNED)
					sectionData.unassignedParams.add(res);
				else if (res.type == ParamResolutionType.IMPLICITDEFAULT)
                    sectionData.implicitlyAssignedParams.add(res);

				if (res.key != null) {
					((KeyData)doc.getKeyData(res.section, res.key)).paramResolutions.add(res);
				}
			}
		}
	}

    protected List<ParamResolution> collectParameters(final String activeSection) {
	    // resolve section chain and network
		INedTypeResolver res = NedResourcesPlugin.getNedResources();
		final String[] sectionChain = InifileUtils.resolveSectionChain(doc, activeSection);
		String networkName = InifileUtils.lookupConfig(sectionChain, CFGID_NETWORK.getName(), doc);
		if (networkName == null)
			networkName = CFGID_NETWORK.getDefaultValue();
		if (networkName == null)
			return new ArrayList<ParamResolution>();
		INedTypeInfo network = resolveNetwork(res, networkName);
		if (network == null)
			return new ArrayList<ParamResolution>();

		// traverse the network and collect resolutions meanwhile
		ArrayList<ParamResolution> list = new ArrayList<ParamResolution>();
		IProject contextProject = doc.getDocumentFile().getProject();
		NedTreeTraversal treeTraversal = new NedTreeTraversal(res, createParamCollectingNedTreeVisitor(list, res, sectionChain, doc), contextProject);
		treeTraversal.traverse(network.getFullyQualifiedName());

		return list;
	}

/*
    // TODO: move?
    // testParamAssignments("C:\\Workspace\\Repository\\omnetpp\\test\\param\\param.out", list);
    public void testParamAssignments(String fileName, ArrayList<ParamResolution> list) {
        try {
            int index = 0;
            Properties properties = new Properties();
            properties.load(new FileInputStream(fileName));

            for (Object key : CollectionUtils.toSorted((Set)properties.keySet(), new DictionaryComparator())) {
                String paramName = (String)key;
                String runtimeParamValue = properties.getProperty(paramName);
                boolean iniDefault = false;

                for (ParamResolution paramResolution : list) {
                    String paramPattern;

                    if (paramResolution.key != null)
                        paramPattern = paramResolution.key;
                    else {
                        String fullPath = paramResolution.fullPath;
                        String paramAssignment = paramResolution.paramAssignment != null ? paramResolution.paramAssignment.getName() : paramResolution.paramDeclaration.getName();

                        if (paramAssignment.indexOf('.') != -1)
                            fullPath = fullPath.substring(0, fullPath.lastIndexOf('.'));

                        paramPattern = fullPath + "." + paramAssignment;
                    }

                    PatternMatcher m = new PatternMatcher(paramPattern, true, true, true);

                    if (m.matches(paramName)) {
                        String ideParamValue = null;

                        switch (paramResolution.type) {
                            case INI_ASK:
                                if (iniDefault)
                                    continue;
                            case UNASSIGNED:
                                ideParamValue = "\"" + index + "\"";
                                index++;
                                break;
                            case INI_DEFAULT:
                                iniDefault = true;
                                continue;
                            case NED:
                            case IMPLICITDEFAULT:
                                ideParamValue = paramResolution.paramAssignment.getValue();
                                break;
                            case INI:
                            case INI_OVERRIDE:
                            case INI_NEDDEFAULT:
                                if (iniDefault)
                                    continue;
                                ideParamValue = doc.getValue(paramResolution.section, paramResolution.key);
                                break;
                            default:
                                throw new RuntimeException();
                        }

                        if (!runtimeParamValue.equals(ideParamValue))
                            Debug.println("*** Mismatch *** for name: " + paramName + ", runtime value: " + runtimeParamValue + ", ide value: " + ideParamValue + ", pattern: " + paramPattern);
                        else
                            Debug.println("Match for name: " + paramName + ", value: " + runtimeParamValue);

                        break;
                    }
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
*/
    
    /**
     * Collects parameters of a module type (recursively), *without* an inifile present.
     */
    public static List<ParamResolution> collectParameters(INedTypeInfo moduleType) {
        return collectParameters(moduleType, moduleType.getProject());
    }
    
	/**
	 * Collects parameters of a module type (recursively), *without* an inifile present.
	 * The contextProject parameter affects the resolution of parametric submodule types ("like").
	 */
	public static List<ParamResolution> collectParameters(INedTypeInfo moduleType, IProject contextProject) {
		ArrayList<ParamResolution> list = new ArrayList<ParamResolution>();
		INedTypeResolver res = NedResourcesPlugin.getNedResources();
		NedTreeTraversal treeTraversal = new NedTreeTraversal(res, createParamCollectingNedTreeVisitor(list, res, null, null), contextProject);
		treeTraversal.traverse(moduleType);
		return list;
	}

    /**
     * Collects parameters of a submodule subtree, *without* an inifile present.
     */
    public static List<ParamResolution> collectParameters(SubmoduleElementEx submodule) {
        IProject contextProject = submodule.getEnclosingTypeElement().getNedTypeInfo().getProject();
        return collectParameters(submodule, contextProject);
    }
    
    /**
	 * Collects parameters of a submodule subtree, *without* an inifile present.
     * The contextProject parameter affects the resolution of parametric submodule types ("like").
	 */
	public static List<ParamResolution> collectParameters(SubmoduleElementEx submodule, IProject contextProject) {
		List<ParamResolution> list = new ArrayList<ParamResolution>();
		INedTypeResolver res = NedResourcesPlugin.getNedResources();
		// TODO: this ignores deep parameter settings from the compound module above the submodule
		NedTreeTraversal treeTraversal = new NedTreeTraversal(res, createParamCollectingNedTreeVisitor(list, res, null, null), contextProject);
		treeTraversal.traverse(submodule);
		return list;
	}

    protected static IModuleTreeVisitor createParamCollectingNedTreeVisitor(final List<ParamResolution> resultList, INedTypeResolver res, final String[] sectionChain, final IInifileDocument doc) {
        return new ParamUtil.RecursiveParamDeclarationVisitor() {
            @Override
            protected boolean visitParamDeclaration(String fullPath, Stack<INedTypeInfo> typeInfoPath, Stack<ISubmoduleOrConnection> elementPath, ParamElementEx paramDeclaration) {
                resolveParameter(resultList, fullPath, typeInfoPath, elementPath, sectionChain, doc, paramDeclaration);
                return true;
            }

            @Override
            public String resolveLikeType(ISubmoduleOrConnection element) {
                // Note: we cannot use InifileUtils.resolveLikeParam(), as that calls
                // resolveLikeParam() which relies on the data structure we are currently building

                // get like parameter name
                String likeParamName = element.getLikeParam();
                if (likeParamName != null && !likeParamName.matches("[A-Za-z_][A-Za-z0-9_]*"))
                    return null;  // sorry, we are only prepared to resolve parent module parameters (but not expressions)

                // look up parameter value (note: we cannot use resolveLikeParam() here yet)
                String fullPath = StringUtils.join(fullPathStack, ".");
                ParamResolution res = null;
                for (ParamResolution r : resultList)
                    if (r.paramDeclaration.getName().equals(likeParamName) && r.fullPath.equals(fullPath))
                        {res = r; break;}
                if (res == null)
                    return null; // likely no such parameter
                String value = getParamValue(res, doc);
                if (value == null)
                    return null; // likely unassigned
                try {
                    value = Common.parseQuotedString(value);
                } catch (RuntimeException e) {
                    return null; // something is wrong: value is not a string constant?
                }
                // note: value is likely a simple (unqualified) name, it'll be resolved
                // to fully qualified name in the caller (NedTreeTraversal)
                return value;
            }
        };
    }

    /**
     * Resolve parameters of a module type or submodule, based solely on NED information,
     * without inifile. This is useful for views when a NED editor is active.
     */
    public static void resolveModuleParameters(List<ParamResolution> resultList, String fullPath, Vector<INedTypeInfo> typeInfoPath, Vector<ISubmoduleOrConnection> elementPath) {
        for (ParamElementEx paramDeclaration : typeInfoPath.lastElement().getParamDeclarations().values())
            resolveParameter(resultList, fullPath, typeInfoPath, elementPath, null, null, paramDeclaration);
    }

	/**
	 * Determines how a NED parameter gets assigned (inifile, NED file, etc).
	 * The sectionChain and doc parameters may be null, which means that only parameter
	 * assignments given in NED will be taken into account.
	 *
     * This method adds one or more ParamResolution objects to resultList. For example,
     * if the inifile contains lines like:
     *     Network.node[0].address = value1
     *     Network.node[1].address = value2
     *     Network.node[*].address = valueX
     * then this method will add three ParamResolutions.
	 */
    // TODO: normalize param resolutions in terms of vector indices, that is the resulting param resolutions should be disjunct
    //       this makes the order of resolutions unimportant, it also helps the user to find the actual value of a particluar parameter
    //       since indices are always individual constants, or constant ranges, or wildcards,
    //       and vector lower bound is always 0, while vector upper bound is either constant or unknown
    //       it is quite doable even if not so simple
	public static void resolveParameter(List<ParamResolution> resultList, String fullPath, Vector<INedTypeInfo> typeInfoPath, Vector<ISubmoduleOrConnection> elementPath, String[] sectionChain, IInifileDocument doc, ParamElementEx paramDeclaration)
	{
	    // look up parameter assignments in NED
        ArrayList<ParamElementEx> paramAssignments = ParamUtil.findParamAssignmentsForParamDeclaration(typeInfoPath, elementPath, paramDeclaration);
        boolean hasNedUnassigned = false;
        boolean hasNedTotalAssignment = false;
        boolean hasNedDefaultAssignment = false;
        for (ParamElementEx paramAssignment : paramAssignments) {
            hasNedUnassigned |= StringUtils.isEmpty(paramAssignment.getValue());
            hasNedTotalAssignment |= ParamUtil.isTotalParamAssignment(paramAssignment);
            hasNedDefaultAssignment |= paramAssignment.getIsDefault();
        }

        // look up parameter assignments in INI
        String activeSection = null;
        List<SectionKey> sectionKeys = null;
        boolean hasIniTotalAssignment = false;

        if (doc != null) {
            activeSection = sectionChain[0];

            // TODO: avoid calling lookupParameter twice
            sectionKeys = InifileUtils.lookupParameter(fullPath + "." + paramDeclaration.getName(), false, sectionChain, doc);

            for (SectionKey sectionKey : sectionKeys)
                hasIniTotalAssignment |= ParamUtil.isTotalParamAssignment(sectionKey.key);

            sectionKeys = InifileUtils.lookupParameter(fullPath + "." + paramDeclaration.getName(), hasNedDefaultAssignment, sectionChain, doc);
        }

        // process non default parameter assignments from NED
        for (ParamElementEx paramAssignment : paramAssignments) {
            if (!paramAssignment.getIsDefault() && !StringUtils.isEmpty(paramAssignment.getValue())) {
                resultList.add(new ParamResolution(fullPath, elementPath, paramDeclaration, paramAssignment, ParamResolutionType.NED, activeSection, null, null));

                if (ParamUtil.isTotalParamAssignment(paramAssignment))
                    return;
            }
        }

        // process parameter assignments from INI
        if (doc != null) {
    	    for (SectionKey sectionKey : sectionKeys) {
    	        String iniSection = sectionKey.section;
    	        String iniKey = sectionKey.key;
    	        String iniValue = doc.getValue(iniSection, iniKey);
    	        Assert.isTrue(iniValue != null); // must exist

    	        // so, find out how the parameter's going to be assigned...
    	        ParamResolutionType type;
                if (iniValue.equals(ConfigRegistry.DEFAULT)) {
                    if (!hasNedDefaultAssignment)
                        continue;
                    else
                        type = ParamResolutionType.INI_DEFAULT;
                }
                else if (iniValue.equals(ConfigRegistry.ASK))
                    type = ParamResolutionType.INI_ASK;
                else if (paramAssignments.size() == 1 && hasNedUnassigned)
    	            type = ParamResolutionType.INI;
    	        else if (paramAssignments.size() == 1 && paramAssignments.get(0).getValue().equals(iniValue))
    	            type = ParamResolutionType.INI_NEDDEFAULT;
    	        else
    	            type = ParamResolutionType.INI_OVERRIDE;

                ParamElementEx paramAssignment = paramAssignments.size() > 0 ? paramAssignments.get(0) : null;
                resultList.add(new ParamResolution(fullPath, elementPath, paramDeclaration, paramAssignment, type, activeSection, iniSection, iniKey));
    	    }
        }

        // process default parameter assignments from NED (this is already in reverse order)
        for (ParamElementEx paramAssignment : paramAssignments) {
            if (StringUtils.isEmpty(paramAssignment.getValue())) {
                if (hasIniTotalAssignment)
                    continue;
                else
                    resultList.add(new ParamResolution(fullPath, elementPath, paramDeclaration, null, ParamResolutionType.UNASSIGNED, activeSection, null, null));
            }
            else if (paramAssignment.getIsDefault() && !hasIniTotalAssignment)
                resultList.add(new ParamResolution(fullPath, elementPath, paramDeclaration, paramAssignment, ParamResolutionType.IMPLICITDEFAULT, activeSection, null, null));
        }
	}

	public List<PropertyResolution> collectSignalResolutions(final String activeSection) {
        INedTypeResolver res = NedResourcesPlugin.getNedResources();
        final String[] sectionChain = InifileUtils.resolveSectionChain(doc, activeSection);
        String networkName = InifileUtils.lookupConfig(sectionChain, CFGID_NETWORK.getName(), doc);
        if (networkName == null)
            networkName = CFGID_NETWORK.getDefaultValue();
        INedTypeInfo network = resolveNetwork(res, networkName);
        if (networkName == null)
            return new ArrayList<PropertyResolution>();
        if (network == null )
            return new ArrayList<PropertyResolution>();

        // traverse the network and collect resolutions meanwhile
        final ArrayList<PropertyResolution> list = new ArrayList<PropertyResolution>();
        IProject contextProject = doc.getDocumentFile().getProject();
        NedTreeTraversal treeTraversal = new NedTreeTraversal(res, new IModuleTreeVisitor() {
            protected Stack<ISubmoduleOrConnection> elementPath = new Stack<ISubmoduleOrConnection>();
            protected Stack<String> fullPathStack = new Stack<String>();  //XXX performance: use cumulative names, so that StringUtils.join() can be eliminated (like: "Net", "Net.node[*]", "Net.node[*].ip" etc)

            public boolean enter(ISubmoduleOrConnection element, INedTypeInfo typeInfo) {
                elementPath.push(element);
                fullPathStack.push(element == null ? typeInfo.getName() : ParamUtil.getParamPathElementName(element));
                for (String propertyName : new String[] {"signal", "statistic"}) {
                    Map<String, PropertyElementEx> propertyMap = typeInfo.getProperties().get(propertyName);
                    String fullPath = StringUtils.join(fullPathStack, ".");
                    if (propertyMap != null)
                        for (PropertyElementEx property : propertyMap.values())
                            list.add(new PropertyResolution(fullPath + "." + property.getIndex(), elementPath, property, activeSection));
                }
                return true;
            }

            public void leave() {
                elementPath.pop();
                fullPathStack.pop();
            }

            public void recursiveType(ISubmoduleOrConnection element, INedTypeInfo typeInfo) {
            }

            public String resolveLikeType(ISubmoduleOrConnection element) {
                return null;
            }

            public void unresolvedType(ISubmoduleOrConnection element, String typeName) {
            }
        }, contextProject);
        treeTraversal.traverse(network.getFullyQualifiedName());
        return list;
	}

    public static void resolveModuleProperties(String propertyName, List<PropertyResolution> list, String fullPath, Vector<INedTypeInfo> typeInfoPath, Vector<ISubmoduleOrConnection> elementPath) {
        INedTypeInfo typeInfo = typeInfoPath.lastElement();
        Map<String, PropertyElementEx> propertyMap = typeInfo.getProperties().get(propertyName);
        if (propertyMap != null)
            for (PropertyElementEx property : propertyMap.values())
                list.add(new PropertyResolution(fullPath + "." + property.getIndex(), elementPath, property, null));
    }

    public boolean containsSectionCycles() {
		analyzeIfChanged();
		return !sectionsCausingCycles.isEmpty();
	}

	public boolean isCausingCycle(String section) {
		analyzeIfChanged();
		return sectionsCausingCycles.contains(section);
	}

	/**
	 * Classify an inifile key, based on its syntax.
	 */
	public static KeyType getKeyType(String key) {
		if (!key.contains("."))
			return KeyType.CONFIG;  // contains no dot
		else if (!key.contains("-"))
			return KeyType.PARAM; // contains dot, but no hyphen
		else
			return KeyType.PER_OBJECT_CONFIG; // contains both dot and hyphen
	}

	public boolean isUnusedParameterKey(String section, String key) {
		synchronized (lock) {
			analyzeIfChanged();
			if (getKeyType(key)!=KeyType.PARAM)
				return false;
			KeyData data = (KeyData) doc.getKeyData(section,key);
			return data!=null && data.paramResolutions!=null && data.paramResolutions.isEmpty();
		}
	}

	/**
	 * Returns the parameter resolutions for the given key. If the returned array is
	 * empty, this key is not used to resolve any module parameters.
	 */
	public ParamResolution[] getParamResolutionsForKey(String section, String key) {
		synchronized (lock) {
			analyzeIfChanged();
			KeyData data = (KeyData) doc.getKeyData(section,key);
			return (data!=null && data.paramResolutions!=null) ? data.paramResolutions.toArray(new ParamResolution[]{}) : new ParamResolution[0];
		}
	}

	public String[] getUnusedParameterKeys(String section) {
		synchronized (lock) {
			analyzeIfChanged();
			ArrayList<String> list = new ArrayList<String>();
			for (String key : doc.getKeys(section))
				if (isUnusedParameterKey(section, key))
					list.add(key);
			return list.toArray(new String[list.size()]);
		}
	}

	/**
	 * Returns parameter resolutions from the given section that correspond to the
	 * parameters of the given module.
	 */
	public ParamResolution[] getParamResolutionsForModule(ISubmoduleOrConnection element, String section) {
		synchronized (lock) {
			analyzeIfChanged();
			SectionData data = (SectionData) doc.getSectionData(section);
			List<ParamResolution> pars = data==null ? null : data.allParamResolutions;
			if (pars == null || pars.isEmpty())
				return new ParamResolution[0];

			// Note: linear search -- can be made more efficient with some lookup table if needed
			ArrayList<ParamResolution> result = new ArrayList<ParamResolution>();
			for (ParamResolution par : pars)
				if (element == par.elementPath[par.elementPath.length - 1])
					result.add(par);
			return result.toArray(new ParamResolution[]{});
		}
	}

	/**
	 * Returns the resolution of the given module parameter from the given section,
	 * or null if not found.
	 */
	public ParamResolution getParamResolutionForModuleParam(String fullPath, String paramName, String section) {
		synchronized (lock) {
			analyzeIfChanged();
			SectionData data = (SectionData) doc.getSectionData(section);
			List<ParamResolution> pars = data==null ? null : data.allParamResolutions;
			if (pars == null || pars.isEmpty())
				return null;

			// Note: linear search -- can be made more efficient with some lookup table if needed
			for (ParamResolution par : pars)
				if (par.paramDeclaration.getName().equals(paramName) && par.fullPath.equals(fullPath))
					return par;
			return null;
		}
	}

	/**
	 * Returns all parameter resolutions for the given inifile section; this includes
	 * unassigned parameters as well.
	 */
	public ParamResolution[] getParamResolutions(String section) {
		synchronized (lock) {
			analyzeIfChanged();
			SectionData sectionData = (SectionData) doc.getSectionData(section);
			return sectionData.allParamResolutions.toArray(new ParamResolution[]{});
		}
	}

	/**
	 * Returns unassigned parameters for the given inifile section.
	 * (This is a filtered subset of the objects returned by getParamResolutions().)
	 */
	public ParamResolution[] getUnassignedParams(String section) {
		synchronized (lock) {
			analyzeIfChanged();
			SectionData sectionData = (SectionData) doc.getSectionData(section);
			return sectionData.unassignedParams.toArray(new ParamResolution[]{});
		}
	}

	/**
     * Returns implicitly assigned parameters for the given inifile section.
     * (This is a filtered subset of the objects returned by getParamResolutions().)
     */
    public ParamResolution[] getImplicitlyAssignedParams(String section) {
        synchronized (lock) {
            analyzeIfChanged();
            SectionData sectionData = (SectionData) doc.getSectionData(section);
            return sectionData.implicitlyAssignedParams.toArray(new ParamResolution[]{});
        }
    }

    public static String getParamValue(ParamResolution res, IInifileDocument doc) {
        return getParamValue(res, doc, true);
    }

	public static String getParamValue(ParamResolution res, IInifileDocument doc, boolean allowNull) {
		switch (res.type) {
			case UNASSIGNED:
			    if (allowNull)
			        return null;
			    else
			        return "(unassigned)";
			case INI_ASK:
                if (allowNull)
                    return null;
                else
                    return "(ask)";
			case NED: case INI_DEFAULT: case IMPLICITDEFAULT:
				return res.paramAssignment.getValue();
			case INI: case INI_OVERRIDE: case INI_NEDDEFAULT:
				return doc.getValue(res.section, res.key);
			default: throw new IllegalArgumentException("invalid param resolution type: "+res.type);
		}
	}

	public static String getParamRemark(ParamResolution res, IInifileDocument doc) {
	    String remark;
	    String nedDefaultIfPresent = res.paramAssignment != null ? " (NED default: " + res.paramAssignment.getValue() + ")" : "";
		switch (res.type) {
		    case UNASSIGNED: remark = "unassigned" + nedDefaultIfPresent; break;
		    case NED: remark = "NED"; break;
		    case INI: remark = "ini"; break;
		    case INI_ASK: remark = "ask" + nedDefaultIfPresent; break;
		    case INI_DEFAULT: remark = "NED default applied"; break;
		    case INI_OVERRIDE: remark = "ini (overrides NED default: " + res.paramAssignment.getValue() + ")"; break;
		    case INI_NEDDEFAULT: remark = "ini (sets same value as NED default)"; break;
		    case IMPLICITDEFAULT: remark = "NED default applied implicitly"; break;
		    default: throw new IllegalStateException("invalid param resolution type: "+res.type);
		}
		if (res.key!=null)
			remark += "; see [" + res.section + "] / " + res.key + "=" + doc.getValue(res.section, res.key);
		else if (res.paramAssignment != null && res.paramAssignment.getIsPattern())
		    remark += "; see (" + res.paramAssignment.getEnclosingTypeElement().getName() + ") / " + res.paramAssignment.getNedSource();
		return remark;
	}

    public PropertyResolution[] getPropertyResolutions(String section) {
        synchronized (lock) {
            analyzeIfChanged();
            SectionData sectionData = (SectionData) doc.getSectionData(section);
            return sectionData.propertyResolutions.toArray(new PropertyResolution[]{});
        }
    }

    public PropertyResolution[] getPropertyResolutionsForModule(String propertyName, ISubmoduleOrConnection element, String section) {
        synchronized (lock) {
            analyzeIfChanged();
            SectionData data = (SectionData)doc.getSectionData(section);
            List<PropertyResolution> propertyResolutions = data == null ? null : data.propertyResolutions;
            if (propertyResolutions == null || propertyResolutions.isEmpty())
                return new PropertyResolution[0];

            // Note: linear search -- can be made more efficient with some lookup table if needed
            ArrayList<PropertyResolution> result = new ArrayList<PropertyResolution>();
            for (PropertyResolution propertyResolution : propertyResolutions)
                if (propertyName.equals(propertyResolution.propertyDeclaration.getName()) && element == propertyResolution.elementPath[propertyResolution.elementPath.length - 1])
                    result.add(propertyResolution);
            return result.toArray(new PropertyResolution[]{});
        }
    }

    /**
	 * Returns names of declared iteration variables ("${variable=...}") from
	 * the given section and all its fallback sections. Note: unnamed iterations
	 * are not in the list.
	 */
	public String[] getIterationVariableNames(String activeSection) {
		synchronized (lock) {
			analyzeIfChanged();
			List<String> result = new ArrayList<String>();
			String[] sectionChain = InifileUtils.resolveSectionChain(doc, activeSection);
			for (String section : sectionChain) {
				SectionData sectionData = (SectionData) doc.getSectionData(section);
				result.addAll(sectionData.namedIterations.keySet());
			}
			String[] array = result.toArray(new String[]{});
			Arrays.sort(array);
			return array;
		}
	}

	/**
	 * Returns true if the given section or any of its fallback sections
	 * contain an iteration, like "${1,2,5}" or "${x=1,2,5}".
	 */
	public boolean containsIteration(String activeSection) {
		synchronized (lock) {
			analyzeIfChanged();
			String[] sectionChain = InifileUtils.resolveSectionChain(doc, activeSection);
			for (String section : sectionChain) {
				SectionData sectionData = (SectionData) doc.getSectionData(section);
				if (sectionData == null)
				    // XXX Note: sectionData is NOT supposed to be null here. However, it sometimes is;
				    // this occurs with large models and ini files (e.g.INETMANET), and seems to be timing dependent;
				    // and also with greater probability when typing introduces a syntax error in the file.
				    // Workaround: check for null manually here.
				    Debug.println("WARNING: no sectionData for section " + section + " in InifileAnalyzer.containsIteration()!");
				if (sectionData != null && sectionData.iterations.isEmpty())
					return true;
			}
			return false;
		}
	}

	/**
	 * Returns the value string (e.g. "1,2,6..10") for an iteration variable
	 * from the given section and its fallback sections.
	 */
	public String getIterationVariableValueString(String activeSection, String variable) {
		synchronized (lock) {
			analyzeIfChanged();
			String[] sectionChain = InifileUtils.resolveSectionChain(doc, activeSection);
			for (String section : sectionChain) {
				SectionData sectionData = (SectionData) doc.getSectionData(section);
				if (sectionData.namedIterations.containsKey(variable))
					return sectionData.namedIterations.get(variable).value;
			}
			return null;
		}
	}
}

