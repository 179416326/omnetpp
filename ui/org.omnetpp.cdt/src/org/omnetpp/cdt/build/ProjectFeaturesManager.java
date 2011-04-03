package org.omnetpp.cdt.build;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.parser.util.ArrayUtil;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICFolderDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSetting;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.WriteAccessException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.omnetpp.cdt.Activator;
import org.omnetpp.cdt.CDTUtils;
import org.omnetpp.common.Debug;
import org.omnetpp.common.project.NedSourceFoldersConfiguration;
import org.omnetpp.common.project.ProjectUtils;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.ned.core.INedResources;
import org.omnetpp.ned.core.NedResourcesPlugin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Stores the contents of an ".oppfeatures" as ProjectFeature objects, and provides
 * functionality to enable/disable features, to read and validate ".oppfeatures",
 * to check the project state, and other related functions.
 * 
 * @author Andras
 */
public class ProjectFeaturesManager {
    public static final String PROJECTFEATURES_FILENAME = ".oppfeatures";
    public static final QualifiedName PROP_FEATUREENABLEMENT = new QualifiedName(Activator.PLUGIN_ID, "featureEnablement");

    // XML element and attribute names for ".oppfeatures"
    private static final String ELMNT_FEATURE = "feature";
    protected static final String ATT_ID = "id";
    private static final String ATT_NAME = "name";
    private static final String ATT_DESCRIPTION = "description";
    private static final String ATT_INITIALLYENABLED = "initiallyEnabled";
    private static final String ATT_LABELS = "labels";
    private static final String ATT_REQUIRES = "requires";
    private static final String ATT_NEDPACKAGES = "nedPackages";
    private static final String ATT_EXTRASOURCEFOLDERS = "extraSourceFolders";
    private static final String ATT_COMPILEFLAGS = "compileFlags";
    private static final String ATT_LINKERFLAGS = "linkerFlags";
    private static final String ATT_CPPSOURCEROOTS = "cppSourceRoots"; // attribute of root element
    
    private static final String[] ALL_FEATURE_ATTRS = new String[] { 
        ATT_ID, ATT_NAME, ATT_DESCRIPTION, ATT_INITIALLYENABLED, ATT_LABELS, ATT_REQUIRES, 
        ATT_NEDPACKAGES, ATT_EXTRASOURCEFOLDERS, ATT_COMPILEFLAGS,  ATT_LINKERFLAGS 
    };

    // state
    private IProject project;
    private Document doc;  // DOM tree of ".oppfeatures"; only kept for validate()
    private Map<String, ProjectFeature> features;  // indexed by Id
    private Set<String> cppSourceRoots = null; // project relative paths of C++ source locations, e.g. "src" in INET; cppSourceRoots==null means the whole project is a C++ source location

    /**
     * Creates an empty object. Use loadFeaturesFile() to populate it with the 
     * contents of the project's feature description file.
     */
    public ProjectFeaturesManager(IProject project) {
        this.project = project;
        this.features = new HashMap<String, ProjectFeature>();
    }

    /**
     * Returns the project associated with this features collection.
     */
    public IProject getProject() {
        return project;
    }

    /**
     * Returns true if there are no project features in this object.
     */
    public boolean isEmpty() {
        return features.isEmpty();
    }

    /**
     * Returns the project features in this object.
     */
    public Collection<ProjectFeature> getFeatures() {
        return features.values();
    }

    /**
     * Returns the IDs of the stored project features.
     */
    public Set<String> getFeatureIds() {
        return features.keySet();
    }

    /**
     * Returns the project feature with the given ID, or null if there isn't one.
     */
    public ProjectFeature getFeature(String id) {
        return features.get(id);
    }

    /**
     * Returns the list of enabled project features for the project, using saved state.
     * For features that have no enablement state saved, it uses their getInitiallyEnabled() method.
     */
    public List<ProjectFeature> getEnabledFeatures() throws CoreException {
        String value = project.getPersistentProperty(PROP_FEATUREENABLEMENT);
        Map<String, Boolean> featureEnablements = new HashMap<String, Boolean>();
        if (value != null) {
            for (String s : value.split("\\s+")) {
                if (s.endsWith(":0") || s.endsWith(":1"))
                    featureEnablements.put(s.substring(0, s.length()-2), s.charAt(s.length()-1)=='1');
                else
                    Activator.log(IMarker.SEVERITY_WARNING, "Invalid item in " + PROP_FEATUREENABLEMENT + " project property: " + s);
            }
        }
        List<ProjectFeature> result = new ArrayList<ProjectFeature>();
        for (ProjectFeature f : getFeatures()) {
            Boolean savedEnabled = featureEnablements.get(f.getId());
            boolean enabled = (savedEnabled != null) ? savedEnabled : f.getInitiallyEnabled();
            if (enabled)
                result.add(f);
        }
        return result;
    }

    /**
     * Stores the enablement state of project features for this project. 
     */
    public void saveFeatureEnablement(List<ProjectFeature> enabledFeatures) throws CoreException {
        String value = "";
        for (ProjectFeature f : getFeatures())
            value += f.getId() + ":" + (enabledFeatures.contains(f) ? "1" : "0") + " ";
        project.setPersistentProperty(PROP_FEATUREENABLEMENT, value.trim());
    }

    /**
     * Populates the object with the contents of the project's feature 
     * description file, if one exists. Returns true on success. 
     * If the file doesn't exist, it returns false. If the file exists 
     * but could not be read or parsed, it throws an exception.
     */    
    public boolean loadFeaturesFile() throws CoreException {
        doc = readFeaturesFile();
        if (doc == null)
            return false;
        this.features = extractFeatures(doc);
        return true;
    }

    /**
     * Returns the handle for the features description file of the given project.
     */
    public IFile getFeatureDescriptionFile() {
        return project.getFile(PROJECTFEATURES_FILENAME);
    }

    /**
     * Reads the feature description file from the project into an XML document,
     * without interpreting it. Returns null if the file does not exist. 
     * Throws CoreException on parse error, file read error, etc. 
     */
    protected Document readFeaturesFile() throws CoreException {
        IFile featuresFile = getFeatureDescriptionFile();
        if (!featuresFile.exists()) {
            return null;
        }
        else {
            try {
                if (!project.getWorkspace().isTreeLocked())
                    featuresFile.refreshLocal(IResource.DEPTH_ZERO, null);
                DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                return docBuilder.parse(featuresFile.getContents());
            }
            catch (Exception e) { // catches ParserConfigurationException, SAXException, IOException
                throw Activator.wrapIntoCoreException(featuresFile.getFullPath() + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Extracts the features from the given document. Makes an attempt NOT to throw an exception on any input. 
     */
    protected Map<String, ProjectFeature> extractFeatures(Document doc) {
        Map<String, ProjectFeature> result = new LinkedHashMap<String, ProjectFeature>();
        Element root = doc.getDocumentElement();
        cppSourceRoots = root.getAttributeNode(ATT_CPPSOURCEROOTS)==null ? null : getListAttribute(root, ATT_CPPSOURCEROOTS);
        NodeList featureElements = root.getElementsByTagName(ELMNT_FEATURE);
        int unnamedCount = 0; 
        for (int i=0; i<featureElements.getLength(); i++) {
            Element e = (Element)featureElements.item(i);
            String id = getAttribute(e, ATT_ID);
            if (id.equals(""))
                id = "unnamed" + (++unnamedCount);
            ProjectFeature feature = new ProjectFeature(
                    id,
                    getAttribute(e, ATT_NAME),
                    getAttribute(e, ATT_DESCRIPTION),
                    getBooleanAttribute(e, ATT_INITIALLYENABLED, true),
                    getListAttribute(e, ATT_LABELS),
                    getListAttribute(e, ATT_REQUIRES),
                    getListAttribute(e, ATT_NEDPACKAGES),
                    getListAttribute(e, ATT_EXTRASOURCEFOLDERS),
                    getAttribute(e, ATT_COMPILEFLAGS),
                    getAttribute(e, ATT_LINKERFLAGS)
            );
            result.put(feature.getId(), feature);
        }
        return result;
    }

    private Set<String> getListAttribute(Element parent, String name) {
        Set<String> result = new LinkedHashSet<String>();
        String content = getAttribute(parent, name);
        if (StringUtils.isNotBlank(content))
            result.addAll(Arrays.asList(content.split("\\s+")));
        return result;
    }

    private boolean getBooleanAttribute(Element parent, String name, boolean defaultValue) {
        String value = getAttribute(parent, name);
        if (StringUtils.isEmpty(value))
            return defaultValue;
        return value.equals("true");
    }
    
    private String getAttribute(Element parent, String name) {
        String value = parent.getAttribute(name);
        value = value.trim().replaceAll("\\s+", " ");
        return value;
    }

    /**
     * Validates the feature description file, and returns the list of errors found in it as text.
     */
    public List<String> validateFeatureDescriptions() {
        List<String> errors = new ArrayList<String>();

        if (doc != null)
            validateDomTree(errors);
        
        for (ProjectFeature f : getFeatures())
            validateFeatures(f, errors);
       
        return errors;
    }

    protected void validateDomTree(List<String> errors) {
        // check the DOM tree
        Element root = doc.getDocumentElement();
        for (Node child : toArray(root.getAttributes()))
            if (!child.getNodeName().equals(ATT_CPPSOURCEROOTS))
                errors.add("<" + root.getNodeName() + ">: ignoring unknown attribute \"" + child.getNodeName() + "\"");
        for (Node child : toArray(root.getChildNodes()))
            if (child instanceof Element && !child.getNodeName().equals(ELMNT_FEATURE))
                errors.add("<" + root.getNodeName() + ">: ignoring unknown child element <" + child.getNodeName() + ">");

        // check <feature> elements
        for (Node elem : toArray(root.getElementsByTagName(ELMNT_FEATURE))) {
            Element feature = (Element) elem;
            String id = feature.getAttribute(ATT_ID);
            String prefix = "<" + feature.getNodeName() + " " + ATT_ID + "=\"" + id + "\">: ";
            
            // check attributes
            if (StringUtils.isEmpty(id))
                errors.add(prefix + ATT_ID + " attribute cannot be empty");
            else if (!ProjectFeature.isValidId(id))
                errors.add(prefix + ATT_ID + " attribute contains invalid characters");
            if (StringUtils.isEmpty(feature.getAttribute(ATT_NAME)))
                errors.add(prefix + ATT_NAME + " attribute cannot be empty");
            String enable = feature.getAttribute(ATT_INITIALLYENABLED);
            if (!StringUtils.isEmpty(enable) && !enable.equals("true") && !enable.equals("false"))
                errors.add(prefix + ATT_INITIALLYENABLED + " attribute must be 'true' or 'false'");

            // check for extra attributes and child elements
            for (Node child : toArray(feature.getAttributes()))
                if (!ArrayUtil.contains(ALL_FEATURE_ATTRS, child.getNodeName()))
                    errors.add(prefix + "ignoring unknown attribute \"" + child.getNodeName() + "\"");
            for (Node child : toArray(feature.getChildNodes()))
                if (child instanceof Element)
                    errors.add(prefix + "ignoring child element <" + child.getNodeName() + ">");
        }
    }

    protected static Node[] toArray(NodeList nodes) {
        Node[] result = new Node[nodes.getLength()];
        for (int i = 0; i < nodes.getLength(); i++)
            result[i] = nodes.item(i);
        return result;
    }

    protected static Node[] toArray(NamedNodeMap nodes) {
        Node[] result = new Node[nodes.getLength()];
        for (int i = 0; i < nodes.getLength(); i++)
            result[i] = nodes.item(i);
        return result;
    }
    
    protected void validateFeatures(ProjectFeature f, List<String> errors) {
        // check that features mentioned as dependencies really exist
        String prefix = f.getId() + ": ";
        Set<String> unrecognized = new LinkedHashSet<String>();
        for (String dep : f.getDependencies())
            if (!features.containsKey(dep))
                unrecognized.add(dep);
        if (!unrecognized.isEmpty())
            errors.add(prefix + "unknown feature(s) in dependency list: " + StringUtils.join(unrecognized, ", "));

        // check for circular dependencies
        checkForCircularDependencies(f, new Stack<ProjectFeature>(), errors);
        
        // check compile flags. Note: keep this code in sync with addFeatureCFlagsTo()
        for (String cflag : f.getCompileFlags().split("\\s+")) {
            if (cflag.length()>0 && !cflag.startsWith("-D") && !cflag.startsWith("-I"))
                errors.add(prefix + "unknown compiler option \"" + cflag + "\": only -D and -I are supported here");
            if (cflag.equals("-D") || cflag.equals("-I"))
                errors.add(prefix + "compiler options: arguments of -D and -I are expected to be written together with the option name (i.e. without space)");
        }

        // check linker flags. Note: keep this code in sync with addFeatureLDFlagsTo()
        for (String ldflag : f.getLinkerFlags().split("\\s+")) {
            if (ldflag.length()>0 && !ldflag.startsWith("-l") && !ldflag.startsWith("-L"))
                errors.add(prefix + "unknown linker option \"" + ldflag + "\": only -l and -L are supported here");
            if (ldflag.equals("-l") || ldflag.equals("-L"))
                errors.add(prefix + "linker options: arguments of -l and -L are expected to be written together with the option name (i.e. without space)");
        }
        
        // check for nonexistent NED packages
        INedResources nedResources = NedResourcesPlugin.getNedResources();
        for (String pkg : f.getNedPackages()) {
            IContainer[] folders = nedResources.getFoldersForPackage(project, pkg);
            if (folders.length == 0)
                errors.add(prefix + "no such NED package: " + pkg);
            boolean found = false;
            for (IContainer folder : folders)
                if (folder.exists())
                    found = true;

            if (!found)
                errors.add(prefix + "no folder corresponds to NED package " + pkg);
        }
        
        // check for C++ source folders
        for (String extraSrcFolder : f.getExtraSourceFolders())
            if (!project.getFolder(new Path(extraSrcFolder)).exists())
                errors.add(prefix + "no such folder: \"" + extraSrcFolder + "\"");

        // check that directories in features don't overlap (otherwise exclusion won't work properly)
        for (String pkg : f.getNedPackages())
            for (ProjectFeature f2 : getFeatures())
                if (f != f2)
                    for (String pkg2 : f2.getNedPackages())
                        if (pkg.equals(pkg2) || pkg2.startsWith(pkg+"."))
                            errors.add(prefix + "has overlapping NED packages with " + f2.getId() + " (" + pkg + " vs. " + pkg2 + ")");
        for (IContainer folder : getExtraCxxSourceFolders(f))
            for (ProjectFeature f2 : getFeatures())
                if (f != f2)
                    for (IContainer folder2 : getExtraCxxSourceFolders(f2))
                        if (folder.equals(folder2) || folder.getFullPath().isPrefixOf(folder2.getFullPath()))
                            errors.add(prefix + "has overlapping extra C++ source folders with " + f2.getId() + " (" + folder.getFullPath() + " vs. " + folder2.getFullPath() + ")");
        for (IContainer folder : getNedBasedCxxSourceFolders(f))
            for (ProjectFeature f2 : getFeatures())
                for (IContainer folder2 : getExtraCxxSourceFolders(f2))
                    if (folder.equals(folder2) || folder.getFullPath().isPrefixOf(folder2.getFullPath()) || folder2.getFullPath().isPrefixOf(folder.getFullPath()))
                        errors.add(prefix + "has NED package based C++ source folder overlapping with extra C++ source folder of " + f2.getId() + " (" + folder.getFullPath() + " vs. " + folder2.getFullPath() + ")");
    }

    protected void checkForCircularDependencies(ProjectFeature f, Stack<ProjectFeature> stack, List<String> errors) {
        if (stack.contains(f)) {
            if (stack.get(0) == f) // only report the error for cycle members, not for all that refer to the cycle
                errors.add("Circular dependency: " + StringUtils.join(stack, " -> ") + " -> " + f);
            return;
        }
        stack.push(f);
        for (String id : f.getDependencies())
            if (getFeature(id) != null)
                checkForCircularDependencies(getFeature(id), stack, errors);
        stack.pop();
    }

    /**
     * Adds compile options contributed by the enabled features to makemakeOptions. 
     * Invoked by MetaMakemake to process "--meta:feature-cflags". 
     */
    public void addFeatureCFlagsTo(MakemakeOptions makemakeOptions) throws CoreException {
        List<String> cflags = new ArrayList<String>();
        for (ProjectFeature f : getEnabledFeatures())
            cflags.addAll(Arrays.asList(f.getCompileFlags().split("\\s+")));

        // process the compile options. NOTE: must be kept in sync with validation code in validateFeatures()!
        for (String cflag : cflags) {
            // we only need to handle -I here, and can simply ignore the rest: -D's are automatically 
            // added to the normal CDT config, and validateFeatures() reports all other options as errors.
            // We can also reject "-I <path>" (i.e. with a space), because validateFeatures() also complains about it.
            if (cflag.startsWith("-I") && cflag.length()>2)
                makemakeOptions.includeDirs.add(cflag.substring(2));
        }
    }

    /**
     * Adds linker options contributed by the enabled features to makemakeOptions. 
     * Invoked by MetaMakemake to process "--meta:feature-ldflags". 
     */
    public void addFeatureLDFlagsTo(MakemakeOptions makemakeOptions) throws CoreException {
        List<String> ldflags = new ArrayList<String>();
        for (ProjectFeature f : getEnabledFeatures())
            ldflags.addAll(Arrays.asList(f.getLinkerFlags().split("\\s+")));

        // process the linker options. NOTE: must be kept in sync with validation code in validateFeatures()!
        for (String ldflag : ldflags) {
            // we only need to handle -l and -L here, and can simply ignore the rest:
            // validateFeatures() already reports them as errors. We can also reject "-L <path>" 
            // (i.e. with a space), because validateFeatures() also complains about it.
            if (ldflag.startsWith("-l") && ldflag.length()>2)
                makemakeOptions.libs.add(ldflag.substring(2));
            else if (ldflag.startsWith("-L")  && ldflag.length()>2)
                makemakeOptions.libDirs.add(ldflag.substring(2));
        }
    }

    /**
     * Initializes the project state (CDT, NED) according to the enabled features, using getEnabledFeatures())
     */
    public void initializeProjectState() throws CoreException {
        fixupProjectState();
    }
    
    /**
     * Updates project configuration (CDT and NED) so that it reflects the currently enabled features.
     */
    public void fixupProjectState() throws CoreException {
        // load NED and CDT configuration
        ICProjectDescription projectDescription = CoreModel.getDefault().getProjectDescription(project);
        ICConfigurationDescription[] configurations = projectDescription.getConfigurations();
        NedSourceFoldersConfiguration nedSourceFoldersConfig = ProjectUtils.readNedFoldersFile(project);
        
        // fix them up
        fixupProjectState(configurations, nedSourceFoldersConfig, getEnabledFeatures());
        
        // save them
        ProjectUtils.saveNedFoldersFile(project, nedSourceFoldersConfig);
        CoreModel.getDefault().setProjectDescription(project, projectDescription);
    }

    public void fixupProjectState(ICConfigurationDescription[] configurations, NedSourceFoldersConfiguration nedSourceFoldersConfig, List<ProjectFeature> enabledFeatures) throws CoreException {
        // refresh all features
        for (ProjectFeature f : getFeatures())
            setFeatureEnabled(configurations, nedSourceFoldersConfig, f, enabledFeatures.contains(f));
    }
    
    /**
     * Computes and returns the set of features that the given feature directly or indirectly 
     * depends on (requires). 
     */
    public Set<ProjectFeature> collectDependencies(ProjectFeature feature) {
        Assert.isTrue(getFeature(feature.getId()) == feature, "Alien feature!");
        Set<ProjectFeature> dependencies = new LinkedHashSet<ProjectFeature>();
        dependencies.add(feature); // temporarily

        boolean changed;
        do {
            changed = false;
            for (ProjectFeature f : dependencies.toArray(new ProjectFeature[]{})) {
                for (String id : f.getDependencies()) {
                    ProjectFeature dep = getFeature(id);
                    if (dep != null && !dependencies.contains(dep)) {
                        dependencies.add(dep);
                        changed = true;
                    }
                }
            }
        } while (changed);

        dependencies.remove(feature);
        
        return dependencies;
    }

    /**
     * Computes and returns the set of features that directly or indirectly depend on (require) 
     * the given feature.
     */
    public Set<ProjectFeature> collectDependentFeatures(ProjectFeature feature) {
        Assert.isTrue(getFeature(feature.getId()) == feature, "Alien feature!");
        Set<ProjectFeature> dependentFeatures = new LinkedHashSet<ProjectFeature>();
        dependentFeatures.add(feature); // temporarily

        boolean changed;
        do {
            changed = false;
            for (ProjectFeature f : dependentFeatures.toArray(new ProjectFeature[]{})) {
                for (ProjectFeature dep : getDependentFeatures(f)) {
                    if (!dependentFeatures.contains(dep)) {
                        dependentFeatures.add(dep);
                        changed = true;
                    }
                }
            }
        } while (changed);

        dependentFeatures.remove(feature);
        
        return dependentFeatures;
    }

    /**
     * Returns the set of features that directly list the given feature among their dependencies.
     */
    public Set<ProjectFeature> getDependentFeatures(ProjectFeature feature) {
        Set<ProjectFeature> result = new LinkedHashSet<ProjectFeature>();
        for (ProjectFeature f : getFeatures())
            if (f.getDependencies().contains(feature.getId())) 
                result.add(f);
        return result;
    }

    public void setFeatureEnabled(ICConfigurationDescription[] configurations, NedSourceFoldersConfiguration nedSourceFoldersConfig, ProjectFeature feature, boolean enable) throws CoreException {
        Assert.isTrue(getFeature(feature.getId()) == feature, "Alien feature!");
        Debug.println((enable ? "enabling" : "disabling") + " feature " + feature.getId());
        
        // modify list of excluded NED packages
        Set<String> excludedPackages = new HashSet<String>();
        excludedPackages.addAll(Arrays.asList(nedSourceFoldersConfig.getExcludedPackages()));
        if (enable)
            excludedPackages.removeAll(feature.getNedPackages());
        else
            excludedPackages.addAll(feature.getNedPackages());
        nedSourceFoldersConfig.setExcludedPackages(excludedPackages.toArray(new String[]{}));

        // exclude/include C++ source folders that correspond to the feature's NED packages
        List<IContainer> folders = getAllCxxSourceFolders(feature);
        for (IContainer folder : folders)
            setFolderExcluded(configurations, folder, !enable);
        
        // add/remove preprocessor symbols
        // Note: we set it on each source folder (only setting on the root does not seem to be enough)
        for (String cflag : feature.getCompileFlags().split("\\s+")) {
            if (cflag.startsWith("-D") && cflag.length()>2) {
                String symbol = cflag.substring(2).replaceAll("=.*", "");
                String value = cflag.replaceAll("[^=]*=?(.*)", "$1");  
                setMacroInAllConfigurationsAndFoldersAndLanguages(project, configurations, symbol, enable ? value : null);
            }
        }
    }

    protected List<IContainer> getAllCxxSourceFolders(ProjectFeature feature) {
        List<IContainer> result = new ArrayList<IContainer>();
        result.addAll(getNedBasedCxxSourceFolders(feature));
        result.addAll(getExtraCxxSourceFolders(feature));
        return result;
    }

    protected List<IContainer> getNedBasedCxxSourceFolders(ProjectFeature feature) {
        // NED packages locations
        List<IContainer> result = new ArrayList<IContainer>();
        INedResources nedResources = NedResourcesPlugin.getNedResources();
        for (String nedPackage: feature.getNedPackages())
            for (IContainer folder : nedResources.getFoldersForPackage(project, nedPackage))
                if (folder.exists() && isCxxSourceFolder(folder))
                    result.add(folder);
        return result;
    }

    protected List<IContainer> getExtraCxxSourceFolders(ProjectFeature feature) {
        // extra C++ source folders
        List<IContainer> result = new ArrayList<IContainer>();
        for (String sourceFolder : feature.getExtraSourceFolders()) {
            IContainer folder = project.getFolder(new Path(sourceFolder));
            if (folder.exists())
                result.add(folder);
        }
        return result;
    }

    protected boolean isCxxSourceFolder(IContainer folder) {
        if (cppSourceRoots == null)
            return true;
        for (String cppSourceRoot : cppSourceRoots)
            if (new Path(cppSourceRoot).isPrefixOf(folder.getProjectRelativePath()))
                return true;
        return false;
    }
    
    protected static void setMacroInAllConfigurationsAndFoldersAndLanguages(IProject project, ICConfigurationDescription[] configurations, String name, String value) throws WriteAccessException, CoreException {
        for (ICConfigurationDescription configuration : configurations) {
            // set it on all source folders
            List<IContainer> sourceLocations = CDTUtils.getSourceLocations(project, configuration.getSourceEntries());
            for (IContainer folder : sourceLocations) {
                ICFolderDescription folderDescription = CDTUtils.getOrCreateFolderDescription(configuration, folder);
                ICLanguageSetting[] folderLanguageSettings = folderDescription.getLanguageSettings();
                for (ICLanguageSetting languageSetting : folderLanguageSettings)
                    if (languageSetting.supportsEntryKind(ICSettingEntry.MACRO))
                        CDTUtils.setMacro(languageSetting, name, value);
            }
            // also set it on the root folder (this setting may not be effective, but it's more discoverable for the user than folder settings)
            ICLanguageSetting[] rootLanguageSettings = configuration.getRootFolderDescription().getLanguageSettings();
            for (ICLanguageSetting languageSetting : rootLanguageSettings)
                if (languageSetting.supportsEntryKind(ICSettingEntry.MACRO))
                    CDTUtils.setMacro(languageSetting, name, value);
        }
    }

    protected static Boolean isMacroSet(IProject project, ICConfigurationDescription[] configurations, String name, String value) throws CoreException {
        int setCount=0, unsetCount=0;
        for (ICConfigurationDescription configuration : configurations) {
            // check it on all source folders
            List<IContainer> sourceLocations = CDTUtils.getSourceLocations(project, configuration.getSourceEntries());
            for (IContainer folder : sourceLocations) {
                ICFolderDescription folderDescription = CDTUtils.getOrCreateFolderDescription(configuration, folder);
                ICLanguageSetting[] folderLanguageSettings = folderDescription.getLanguageSettings();
                for (ICLanguageSetting languageSetting : folderLanguageSettings) {
                    if (languageSetting.supportsEntryKind(ICSettingEntry.MACRO)) {
                        ICLanguageSettingEntry macro = CDTUtils.getMacro(languageSetting, name);
                        if (macro != null && ObjectUtils.equals(macro.getValue(), value))
                            setCount++;
                        else
                            unsetCount++;
                    }
                }
            }
            // also check it on the root folder (this setting may not be effective, but it's more discoverable for the user than folder settings)
            ICLanguageSetting[] rootLanguageSettings = configuration.getRootFolderDescription().getLanguageSettings();
            for (ICLanguageSetting languageSetting : rootLanguageSettings) {
                if (languageSetting.supportsEntryKind(ICSettingEntry.MACRO)) {
                    ICLanguageSettingEntry macro = CDTUtils.getMacro(languageSetting, name);
                    if (macro != null && ObjectUtils.equals(macro.getValue(), value))
                        setCount++;
                    else
                        unsetCount++;
                }
            }
        }
        return (setCount>0 && unsetCount>0) ? null /*misc*/ : setCount>0;
    }

    protected void setFolderExcluded(ICConfigurationDescription[] configurations, IContainer folder, boolean exclude) throws CoreException {
        if (!folder.exists())
            return;
        for (ICConfigurationDescription configuration : configurations) {
            ICSourceEntry[] newEntries = CDTUtils.setExcluded(folder, exclude, configuration.getSourceEntries());
            configuration.setSourceEntries(newEntries);
        }
    }

    protected Boolean isFolderExcluded(ICConfigurationDescription[] configurations, IContainer folder) throws CoreException {
        Assert.isTrue(folder.exists());
        int excludeCount = 0, includeCount = 0;
        for (ICConfigurationDescription configuration : configurations) {
            if (CDTUtils.isExcluded(folder, configuration.getSourceEntries())) 
                excludeCount++; 
            else 
                includeCount++;
        }
        return (excludeCount>0 && includeCount>0) ? null /*misc*/ : excludeCount>0;
    }

    protected static String joinNamesOf(Collection<ProjectFeature> features) {
        List<String> names = new ArrayList<String>();
        for (ProjectFeature f : features)
            names.add(f.getName());
        return StringUtils.join(names, ", ", " and ");
    }

    public static class Problem {
        public ProjectFeature feature;
        public String message;
        public String toString() {return "\"" + feature.getName() + "\": " + message;}
    }

    public List<Problem> validateProjectState() throws CoreException {
        // load NED and CDT configuration
        ICProjectDescription projectDescription = CoreModel.getDefault().getProjectDescription(project);
        ICConfigurationDescription[] configurations = projectDescription.getConfigurations();
        NedSourceFoldersConfiguration nedSourceFoldersConfig = ProjectUtils.readNedFoldersFile(project);
        
        // fix them up
        return validateProjectState(configurations, nedSourceFoldersConfig, getEnabledFeatures());
    }

    /**
     * Checks that the project state, as represented by the configurations and nedSouerceFoldersConfig arguments,
     * corresponds to enablement state of the project features, that is. It checks that NED folders and C++ source folders
     * covered by the features are correctly enabled/disabled, symbols are defined, etc. Errors are returned
     * in a string list.
     */
    public List<Problem> validateProjectState(ICConfigurationDescription[] configurations, NedSourceFoldersConfiguration nedSourceFoldersConfig, List<ProjectFeature> enabledFeatures) throws CoreException {
        List<Problem> problems = new ArrayList<Problem>();
        List<String> excludedPackages = Arrays.asList(nedSourceFoldersConfig.getExcludedPackages());
        
        for (ProjectFeature feature : getFeatures()) {
            boolean enabled = enabledFeatures.contains(feature);
            
            // check NED excluded folders
            if (enabled) {
                // feature is enabled: make sure its NED packages are NOT excluded
                //FIXME check for prefixes not equality!!! i.e. if feature contains "inet.netw.ipv6", then neither "inet.netw" nor "inet.netw.ipv6.util" should be excluded!
                @SuppressWarnings("unchecked")
                Collection<String> wronglyExcludedPackages = CollectionUtils.intersection(excludedPackages, feature.getNedPackages());
                if (!wronglyExcludedPackages.isEmpty())
                    addProblem(problems, feature, "Feature is enabled but some of its NED packages are excluded: " + StringUtils.join(wronglyExcludedPackages, ", "));
            }
            else {
                @SuppressWarnings("unchecked")
                Collection<String> wronglyIncludedPackages = CollectionUtils.subtract(feature.getNedPackages(), excludedPackages);
                if (!wronglyIncludedPackages.isEmpty())
                    addProblem(problems, feature, "Feature is disabled but some of its NED packages are not excluded: " + StringUtils.join(wronglyIncludedPackages, ", "));
            }

            
            // check exclusion state of C++ source folders that correspond to the feature's NED packages
            List<IContainer> folders = getAllCxxSourceFolders(feature);
            for (IContainer folder : folders) {
                Boolean isFolderExcluded = isFolderExcluded(configurations, folder); // null if mixed
                if (enabled) {
                    if (isFolderExcluded==null || isFolderExcluded)
                        addProblem(problems, feature, "Feature is enabled but folder is excluded from C++ build in at least one configuration: " + folder.getFullPath());
                }
                else {
                    if (isFolderExcluded==null || !isFolderExcluded)
                        addProblem(problems, feature, "Feature is disabled but folder is not excluded from C++ build in at least one configuration: " + folder.getFullPath());
                }
            }

            // check preprocessor symbols
            // Note: we set it on each source folder (only setting on the root does not seem to be enough)
            for (String cflag : feature.getCompileFlags().split("\\s+")) {
                if (cflag.startsWith("-D") && cflag.length()>2) {
                    String symbol = cflag.substring(2).replaceAll("=.*", "");
                    String value = cflag.replaceAll("[^=]*=?(.*)", "$1");  
                    Boolean isMacroSet = isMacroSet(project, configurations, symbol, enabled ? value : null);
                    if (enabled) {
                        if (isMacroSet==null || !isMacroSet)
                            addProblem(problems, feature, "Feature is enabled but macro is not set in at least one configuration, folder or language: " + cflag.substring(2));
                    }
                    else {
                        if (isMacroSet==null || isMacroSet)
                            addProblem(problems, feature, "Feature is disabled but macro is set in at least one configuration, folder or language: " + cflag.substring(2));
                    }
                }
            }
        }
        return problems;
    }

    protected void addProblem(List<Problem> problems, ProjectFeature feature, String message) {
        Problem problem = new Problem();
        problem.feature = feature;
        problem.message = message;
        problems.add(problem);
    }
    
    @Override
    public String toString() {
        return StringUtils.join(getFeatureIds(), ",");
    }
}
