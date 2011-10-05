/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.launch.tabs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.swt.SWT;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.omnetpp.common.CommonPlugin;
import org.omnetpp.common.Debug;
import org.omnetpp.common.IConstants;
import org.omnetpp.common.project.ProjectUtils;
import org.omnetpp.common.util.CollectionUtils;
import org.omnetpp.common.util.ReflectionUtils;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.ide.OmnetppMainPlugin;
import org.omnetpp.inifile.editor.model.ConfigRegistry;
import org.omnetpp.inifile.editor.model.InifileParser;
import org.omnetpp.launch.IOmnetppLaunchConstants;
import org.omnetpp.launch.LaunchPlugin;

/**
 * Various utility methods for the launcher.
 */
public class OmnetppLaunchUtils {
    // copied from JavaCore.NATURE_ID (we don't want dependency on the JDT plugins)
    private static final String JAVA_NATURE_ID = "org.eclipse.jdt.core.javanature";
    private static final String CDT_CC_NATURE_ID = "org.eclipse.cdt.core.ccnature";

	/**
	 * Reads the ini file and enumerates all config sections. resolves include directives recursively
	 */
	public static class ConfigEnumeratorCallback extends InifileParser.ParserAdapter {
		class Section {
			String name;
			String network;
			String extnds;
			String descr;
			@Override
			public String toString() {
				String additional = (StringUtils.isEmpty(descr)? "" : " "+descr)+
				(StringUtils.isEmpty(extnds)? "" : " (extends: "+extnds+")")+
				(StringUtils.isEmpty(network)? "" : " (network: "+network+")");
				return name +(StringUtils.isEmpty(additional) ? "" : " --"+additional);
			}
		}

		IFile currentFile;
		Section currentSection;
		Map<String, Section> result;

		public ConfigEnumeratorCallback(IFile file, Map<String, Section> result) {
			this.currentFile = file;
			this.result = result;
		}

		@Override
		public void directiveLine(int lineNumber, int numLines, String rawLine, String directive, String args, String comment) {
			if (directive.equals("include")) {
				// recursively parse the included file
				try {
					IFile file = currentFile.getParent().getFile(new Path(args));
					new InifileParser().parse(file, new ConfigEnumeratorCallback(file, result));
				}
				catch (Exception e) {
					LaunchPlugin.logError("Error reading inifile: ", e);
				}
			}
		}

		@Override
		public void keyValueLine(int lineNumber, int numLines, String rawLine, String key, String value, String comment) {
			if ("extends".equals(key))
				currentSection.extnds = value;

			if ("description".equals(key))
				currentSection.descr = value;

			if ("network".equals(key))
				currentSection.network = value;
		}

		@Override
		public void sectionHeadingLine(int lineNumber, int numLines, String rawLine, String sectionName, String comment) {
			String name = StringUtils.removeStart(sectionName,"Config ");
			if (result.containsKey(name))
				currentSection = result.get(name);
			else {
				currentSection = new Section();
				currentSection.name = name;
				result.put(name, currentSection);
			}
		}

	}

	/**
	 * A workbench content provider that returns only files with a given extension
	 */
	public static class FilteredWorkbenchContentProvider extends WorkbenchContentProvider {
		private final String regexp;

		/**
		 * @param regexp The regular expression where matches should be displayed
		 */
		public FilteredWorkbenchContentProvider(String regexp) {
			super();
			this.regexp = regexp;
		}

		@Override
		public Object[] getChildren(Object element) {
			List<Object> filteredChildren = new ArrayList<Object>();
			for (Object child : super.getChildren(element)) {
				if (child instanceof IFile && ((IFile)child).getName().matches(regexp)
						|| getChildren(child).length > 0)
					filteredChildren.add(child);
			}
			return filteredChildren.toArray();
		}
	};

	/**
	 * Content provider displaying possible projects in the workspace
	 * @author rhornig
	 */
	public static class ProjectWorkbenchContentProvider extends WorkbenchContentProvider {
		@Override
		public Object[] getChildren(Object element) {
			List<Object> filteredChildren = new ArrayList<Object>();
			for (Object child : super.getChildren(element)) {
				if (child instanceof IProject && ((IProject)child).isAccessible())
					filteredChildren.add(child);
			}
			return filteredChildren.toArray();
		}
	};

	/**
	 * A workbench content provider that returns only executable files
	 */
	public static class ExecutableWorkbenchContentProvider extends WorkbenchContentProvider {
		@Override
		public Object[] getChildren(Object element) {
			List<Object> filteredChildren = new ArrayList<Object>();
			for (Object child : super.getChildren(element)) {
				if (child instanceof IFile && isExecutable((IFile)child)
						|| getChildren(child).length > 0)
					filteredChildren.add(child);
			}
			return filteredChildren.toArray();
		}
	};

	/**
	 * Checks whether the resource is an executable file.
	 */
	public static boolean isExecutable(IResource file) {
		if (!(file instanceof IFile) || file.getResourceAttributes() == null)
			return false;
		return file.getResourceAttributes().isExecutable() ||
		StringUtils.containsIgnoreCase("exe.cmd.bat",file.getFileExtension()) && SWT.getPlatform().equals("win32");
	}

    /**
     * Converts inputPath to be relative to referenceDir. This is not possible
     * if the two paths are from different devices, so in this case the method
     * returns the original inputPath unchanged.
     *
     * Copied from MakefileTools.
     *
     * @author andras
     */
	public static IPath makeRelativePathTo(IPath inputPath, IPath referenceDir) {
	    Assert.isTrue(inputPath.isAbsolute());
	    Assert.isTrue(referenceDir.isAbsolute());
	    if (referenceDir.equals(inputPath))
	        return new Path(".");
	    if (!StringUtils.equals(inputPath.getDevice(), referenceDir.getDevice()))
	        return inputPath;
	    int commonPrefixLen = inputPath.matchingFirstSegments(referenceDir);
	    int upLevels = referenceDir.segmentCount() - commonPrefixLen;
	    return new Path(StringUtils.removeEnd(StringUtils.repeat("../", upLevels), "/")).append(inputPath.removeFirstSegments(commonPrefixLen));
	}

	/**
	 * Creates a new temporary configuration from a simulation config type that is compatible with CDT.
	 * When an error happens during conversion, throws a CoreException.
	 */
	@SuppressWarnings("unchecked")
    public static ILaunchConfigurationWorkingCopy convertLaunchConfig(ILaunchConfiguration config, String mode) throws CoreException {
		ILaunchConfigurationWorkingCopy newCfg = config.copy("opp_run temporary configuration");
		newCfg.setAttributes(CollectionUtils.getDeepCopyOf(newCfg.getAttributes())); // otherwise attrs that are Collections themselves are not copied

		// working directory (converted from path to location)
		String workingdirStr = config.getAttribute(IOmnetppLaunchConstants.OPP_WORKING_DIRECTORY, "");
		if (StringUtils.isEmpty(workingdirStr))
            throw new CoreException(new Status(Status.ERROR, LaunchPlugin.PLUGIN_ID, "Working directory must be set"));
		newCfg.setAttribute(IOmnetppLaunchConstants.ATTR_WORKING_DIRECTORY, getLocationForWorkspacePath(workingdirStr, "/", false).toString());

		// absolute filesystem location of the working directory
		IPath workingdirLocation = getLocationForWorkspacePath(StringUtils.substituteVariables(workingdirStr),"/",false);
		
		// executable name
		String exeName = config.getAttribute(IOmnetppLaunchConstants.OPP_EXECUTABLE, "");

		if (StringUtils.isEmpty(exeName)) {  // this means opp_run
		    exeName = OmnetppMainPlugin.getOmnetppBinDir()+"/opp_run";
		    // detect if the current executable is release or debug
		    // if we run a release executable we have to use opp_run_release (instead of opp_run)
		    if (isOppRunReleaseRequired(workingdirStr))
		        exeName += "_release";
            // A CDT project is required for the launcher in debug mode to start the application (using gdb).
            IProject project = findFirstRelatedCDTProject(workingdirStr);
            newCfg.setAttribute(IOmnetppLaunchConstants.ATTR_PROJECT_NAME, project != null ? project.getName() : null);
		    if (mode.equals(ILaunchManager.DEBUG_MODE) && project == null)
		        throw new CoreException(new Status(IStatus.ERROR, LaunchPlugin.PLUGIN_ID, "Cannot launch simulation in debug mode: no related open C++ project"));
		}
		else {
            String projectName = new Path(exeName).segment(0);
            newCfg.setAttribute(IOmnetppLaunchConstants.ATTR_PROJECT_NAME, projectName);
			exeName = new Path(exeName).removeFirstSegments(1).toString(); // project-relative path
		    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		    if (mode.equals(ILaunchManager.DEBUG_MODE) && (!project.exists() || !project.hasNature(CDT_CC_NATURE_ID)))
                throw new CoreException(new Status(IStatus.ERROR, LaunchPlugin.PLUGIN_ID, "Cannot launch simulation in debug mode: the executable's project ("+projectName+") is not an open C++ project"));
		}

		if (Platform.getOS().equals(Platform.OS_WIN32) && !exeName.matches("(?i).*\\.(exe|cmd|bat)$"))
			exeName += ".exe";
		newCfg.setAttribute(IOmnetppLaunchConstants.ATTR_PROGRAM_NAME, exeName);

		String args = "";
		String envirStr = config.getAttribute(IOmnetppLaunchConstants.OPP_USER_INTERFACE, "").trim();
		if (StringUtils.isNotEmpty(envirStr))
			args += " -u "+envirStr;

		String configStr = config.getAttribute(IOmnetppLaunchConstants.OPP_CONFIG_NAME, "").trim();
		if (StringUtils.isNotEmpty(configStr))
			args += " -c "+configStr;

		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			String runStr = config.getAttribute(IOmnetppLaunchConstants.OPP_RUNNUMBER_FOR_DEBUG, "").trim();
			if (StringUtils.isNotEmpty(runStr))
				args += " -r "+runStr;
		}

		String pathSep = System.getProperty("path.separator");

		// NED path
		String nedpathStr = config.getAttribute(IOmnetppLaunchConstants.OPP_NED_PATH, "").trim();
		nedpathStr = StringUtils.substituteVariables(nedpathStr);
		if (StringUtils.isNotBlank(nedpathStr)) {
			String[] nedPaths = StringUtils.split(nedpathStr, pathSep);
			for (int i = 0 ; i< nedPaths.length; i++)
				nedPaths[i] = makeRelativePathTo(getLocationForWorkspacePath(nedPaths[i], workingdirStr, false), workingdirLocation).toString();
			// always create ned path option if more than one path element is present. Do not create if it contains a single . only (that's the default)
			if (nedPaths.length>1 || !".".equals(nedPaths[0]))
			    args += " -n " + StringUtils.join(nedPaths, pathSep)+" ";
		}

		// shared libraries
		String shLibStr = config.getAttribute(IOmnetppLaunchConstants.OPP_SHARED_LIBS, "").trim();
		shLibStr = StringUtils.substituteVariables(shLibStr, "");
		if (StringUtils.isNotBlank(shLibStr)) {
			String[] libs = StringUtils.split(shLibStr);
			// convert to file system location
			for (int i = 0 ; i< libs.length; i++)
				libs[i] = makeRelativePathTo(getLocationForWorkspacePath(libs[i], workingdirStr, true), workingdirLocation).toString();
			args += " -l " + StringUtils.join(libs," -l ")+" ";
		}

		String recEventlog = config.getAttribute(IOmnetppLaunchConstants.OPP_RECORD_EVENTLOG, "").trim();
		if (StringUtils.isNotEmpty(recEventlog))
			args += " --"+ConfigRegistry.CFGID_RECORD_EVENTLOG.getName()+"="+recEventlog+" ";

		String dbgOnErr = config.getAttribute(IOmnetppLaunchConstants.OPP_DEBUG_ON_ERRORS, "").trim();
		if (StringUtils.isNotEmpty(dbgOnErr))
			args += " --"+ConfigRegistry.CFGID_DEBUG_ON_ERRORS.getName()+"="+dbgOnErr+" ";

		// ini files
		String iniStr = config.getAttribute(IOmnetppLaunchConstants.OPP_INI_FILES, "").trim();
		iniStr = StringUtils.substituteVariables(iniStr);
		if (StringUtils.isNotBlank(iniStr)) {
			String[] inifiles = StringUtils.split(iniStr);
			// convert to file system location
			for (int i = 0 ; i< inifiles.length; i++)
				inifiles[i] = makeRelativePathTo(getLocationForWorkspacePath(inifiles[i], workingdirStr, true), workingdirLocation).toString();
			args += " " + StringUtils.join(inifiles," ")+" ";
		}

		// set the program arguments
		newCfg.setAttribute(IOmnetppLaunchConstants.ATTR_PROGRAM_ARGUMENTS,
				config.getAttribute(IOmnetppLaunchConstants.OPP_ADDITIONAL_ARGS, "")+args);

		// handle environment: add OMNETPP_BIN and (DY)LD_LIBRARY_PATH
        Map<String, String> envir = newCfg.getAttribute("org.eclipse.debug.core.environmentVariables", new HashMap<String, String>());
        String path = envir.get("PATH");
        // if the path was not set by hand, generate automatically
        if (StringUtils.isBlank(path)) {
		String win32_ld_library_path = Platform.getOS().equals(Platform.OS_WIN32) ? StringUtils.substituteVariables("${opp_ld_library_path_loc:"+workingdirStr+"}" + pathSep) : "";
            envir.put("PATH",win32_ld_library_path +
        			         StringUtils.substituteVariables("${opp_bin_dir}" + pathSep) +
        			         StringUtils.substituteVariables("${opp_additional_path}" + pathSep) +  // msys/bin, mingw/bin, etc
        			         StringUtils.substituteVariables("${env_var:PATH}"));
        }

        if (!Platform.getOS().equals(Platform.OS_WIN32)) {
        	String ldLibPathVar = Platform.getOS().equals(Platform.OS_MACOSX) ? "DYLD_LIBRARY_PATH" : "LD_LIBRARY_PATH";
        	String ldLibPath = envir.get(ldLibPathVar);
        	// if the path was not set by hand, generate automatically
        	if (StringUtils.isBlank(ldLibPath))
        		envir.put(ldLibPathVar, StringUtils.substituteVariables("${opp_lib_dir}"+pathSep) +
					StringUtils.substituteVariables("${opp_ld_library_path_loc:"+workingdirStr+"}"+pathSep) +
        				StringUtils.substituteVariables("${env_var:"+ldLibPathVar+"}"));
        }

        // gdb is using Python for pretty printing. On Windows we have to add the HOME
        // environment variable so gdb can find the .gdbinit file in the OMNETPP_ROOT directory.
        // on Linux HOME is always present and the user has to create this file manually 
        if (Platform.getOS().equals(Platform.OS_WIN32)){
            String homePath = envir.get("HOME");
            if (StringUtils.isBlank(homePath)) {
                homePath = OmnetppMainPlugin.getOmnetppRootDir();
                if (StringUtils.isNotBlank(homePath))
                    envir.put("HOME", homePath);
            }
        }
        
        String imagePath = envir.get("OMNETPP_IMAGE_PATH");
        if (StringUtils.isBlank(imagePath)) {
            imagePath = CommonPlugin.getConfigurationPreferenceStore().getString(IConstants.PREF_OMNETPP_IMAGE_PATH);
            if (StringUtils.isNotBlank(imagePath))
            	envir.put("OMNETPP_IMAGE_PATH", imagePath);
        }

        // Java CLASSPATH
        //FIXME do not overwrite CLASSPATH if it's already set by the user!
        //FIXME use the inifile's project, not mappedResources!
        IResource[] resources = newCfg.getMappedResources();
        if (resources != null && resources.length != 0) {
            String javaClasspath = getJavaClasspath(resources[0].getProject());
            if (javaClasspath != null)
                envir.put("CLASSPATH", javaClasspath);
        }

        newCfg.setAttribute("org.eclipse.debug.core.environmentVariables", envir);
        // do we need this ??? : configuration.setAttribute("org.eclipse.debug.core.appendEnvironmentVariables", true);

		return newCfg;
	}

    /**
     * Detect if the active configuration of the CDT project is built with release mode omnetpp toolchain.
     * Returns null if the detection was unsuccessful.
     * @throws CoreException
     */
    private static Boolean isReleaseModeCDTProject(IProject project) throws CoreException {
        Assert.isTrue(project.hasNature(CDT_CC_NATURE_ID));

        IConfiguration cfg = ManagedBuildManager.getBuildInfo(project).getDefaultConfiguration();
        while (cfg != null) {
            // compare with the release toolchain names. (they must match with the IDs defined in the plugin.xml)
            if (cfg.getId().equals("org.omnetpp.cdt.gnu.config.release") || cfg.getId().equals("org.omnetpp.cdt.msvc.config.release"))
                return Boolean.TRUE;
            // for a debug toolchain we must use opp_run (which is also built in debug mode)
            if (cfg.getId().equals("org.omnetpp.cdt.gnu.config.debug") || cfg.getId().equals("org.omnetpp.cdt.msvc.config.debug"))
                return Boolean.FALSE;
            cfg = cfg.getParent();
        }
        // we cannot detect our own toolchain
        return null;
    }

    /**
     * Check if we have to use opp_run_release for the project specified by the path.
     * Checks all dependent CDT projects and throws an exception if either of them use different
     * build modes. (i.e. all of the CDT projects must be built as debug OR release)
     */
    private static boolean isOppRunReleaseRequired(String workingDirPath) throws CoreException {
        String resolvedWorkingDir = StringUtils.substituteVariables(workingDirPath);
        IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(resolvedWorkingDir);
        if (resource == null)
            throw new CoreException(new Status(IStatus.ERROR, LaunchPlugin.PLUGIN_ID, "Cannot launch simulation: the working directory path is not accessible."));

        IProject[] projects = ProjectUtils.getAllReferencedProjects(resource.getProject(), false, true);
        Boolean commonReleaseMode = null;
        for (IProject project : projects)
            if (project.hasNature(CDT_CC_NATURE_ID)) {
                Boolean projectReleaseMode = isReleaseModeCDTProject(project);
                if (projectReleaseMode != null && commonReleaseMode != null && projectReleaseMode != commonReleaseMode)
                    throw new CoreException(new Status(IStatus.ERROR, LaunchPlugin.PLUGIN_ID, "Cannot launch simulation: all of the projects must be compiled either in debug or relase mode."));

                if (projectReleaseMode != null)
                    commonReleaseMode = projectReleaseMode;
            }
        return commonReleaseMode == null ? false : commonReleaseMode;  // if mode nt detected, use debug mode
    }

    /**
     * Returns a related CDT project or null if there is no open and related CDT project.
     */
    private static IProject findFirstRelatedCDTProject(String workspacePath) throws CoreException {
	    String resolvedWorkingDir = StringUtils.substituteVariables(workspacePath);
	    IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(resolvedWorkingDir);
	    if (resource == null)
	        return null;
	    IProject[] projects = ProjectUtils.getAllReferencedProjects(resource.getProject(), false, true);
	    for (IProject project : projects)
	        if (project.hasNature(CDT_CC_NATURE_ID))
	            return project;
	    return null;
	}

    /**
	 * Returns the Java CLASSPATH based on the project settings where the class files are generated.
	 */
    public static String getJavaClasspath(IProject project) throws CoreException {
        if (!project.hasNature(JAVA_NATURE_ID))
	        return null;

        // We want to add the output folder (.class files) to the CLASSPATH.
        // We can get it from the nature object, which is also an IJavaProject.
        // We use reflection because we don't want to add JDT to our plugin dependencies.
        // If JDT is not loaded, getNature() will throw a CoreException.
        IProjectNature javaNature;
        try {
            javaNature = project.getNature(JAVA_NATURE_ID);
        } catch (CoreException e) {
            //TODO should this become a show-once dialog?
            LaunchPlugin.logError("Cannot determine CLASSPATH for project with Java nature: JDT not available. Install JDT or set CLASSPATH manually.", e);
            return null;
        }

        // FIXME return also exported libraries (JAR files) from the project
        IPath javaOutputLocation = (IPath)ReflectionUtils.invokeMethod(javaNature, "getOutputLocation");
        String result = ResourcesPlugin.getWorkspace().getRoot().getFile(javaOutputLocation).getLocation().toOSString();

        for (IProject referencedProject : project.getReferencedProjects())
            result += ";" + getJavaClasspath(referencedProject);  // FIXME use platform dependent path separator

        return result;
	}

    /**
     * Convert a workspace path to absolute filesystem path. If pathString is a relative path,
     * it is prepended with the basePath before conversion. The named resource may be a file,
     * folder or project; and the file or folder does not need to exist in the workspace.
     * Throws CoreException if the path cannot be determined (see IResource.getLocation()).
     */
	public static final IPath getLocationForWorkspacePath(String pathString, String basePathString, boolean isFile) throws CoreException {
	    IPath path = new Path(pathString);
	    IPath basePath = StringUtils.isEmpty(basePathString) ? null : new Path(basePathString);
	    if (!path.isAbsolute() && basePath != null)
	        path = basePath.append(path);
	    IPath location = null;
	    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        if (isFile)
	        location = workspaceRoot.getFile(path).getLocation();
	    else if (path.segmentCount()>=2)
	        location = workspaceRoot.getFolder(path).getLocation();
	    else if (path.segmentCount()==1)
	        location = workspaceRoot.getProject(path.toString()).getLocation();
	    if (location == null)
	        throw new CoreException(new Status(Status.ERROR, LaunchPlugin.PLUGIN_ID, "Cannot determine location for "+pathString));
	    return location;
	}

	/**
	 * Expands and returns the working directory attribute of the given launch
	 * configuration. Returns <code>null</code> if a working directory is not
	 * specified. If specified, the working is verified to point to an existing
	 * directory in the local file system.
	 *
	 * @return an absolute path to a directory in the local file system, or
	 * <code>null</code> if unspecified
	 *
	 * @throws CoreException if unable to retrieve the associated launch
	 * configuration attribute, if unable to resolve any variables, or if the
	 * resolved location does not point to an existing directory in the local
	 * file system
	 */
	public static IPath getWorkingDirectoryPath(ILaunchConfiguration config){
		String location = "${workspace_loc}";
		try {
			location = config.getAttribute(IOmnetppLaunchConstants.ATTR_WORKING_DIRECTORY, location);

			if (location != null) {
				String expandedLocation = StringUtils.substituteVariables(location);
				if (expandedLocation.length() > 0) {
					IPath newPath = new Path(expandedLocation);
					return newPath.makeAbsolute();
				}
			}
		} catch (CoreException e) {
			LaunchPlugin.logError("Error getting working directory from configuration", e);
		}
		return new Path(location);
	}

	/**
	 * Expands the provided run numbers into a array for example 1..4,7,9 to 1,2,3,4,7,9.
	 * The "*" means ALL run number: 0-(maxRunNo-1). Empty runPar means: 0.
	 */
	public static boolean containsMultipleRuns(String runPar) throws CoreException  {
		return doParseRuns(runPar, -1).length > 1;
	}

	/**
	 * Expands the provided run numbers into a array for example 1..4,7,9 to 1,2,3,4,7,9.
	 * The "*" means ALL run number: 0-(maxRunNo-1). Empty runPar means: 0.
	 */
	public static int[] parseRuns(String runPar, int maxRunNo) throws CoreException {
		Assert.isTrue(maxRunNo > 0);
		return doParseRuns(runPar, maxRunNo);
	}

	// Does not throw range error if maxRunNo = -1 and returns only the first two runs
	private static int[] doParseRuns(String runPar, int maxRunNo) throws CoreException {
		List<Integer> result = new ArrayList<Integer>();
		runPar = StringUtils.deleteWhitespace(runPar);
		if (StringUtils.isEmpty(runPar)) {
			// empty means: just the first run (0)
			result.add(0);
		}
		else if ("*".equals(runPar)) {
			if (maxRunNo == -1)
				return new int[] {0, 1};
			// create ALL run numbers scenario
			for (int i=0; i<maxRunNo; i++)
				result.add(i);
		}
		else {
			// parse hand specified numbers
			for (String current : StringUtils.split(runPar, ',')) {
				try {
					if (current.contains("..")) {
						String lowerUpper[] = StringUtils.splitByWholeSeparator(current, "..");
						int low = 0;
						int high = maxRunNo < 0 ? Integer.MAX_VALUE : maxRunNo-1;
						if (lowerUpper.length > 0 && lowerUpper[0].length() > 0)
							low = Integer.parseInt(lowerUpper[0]);

						// if we have an upper bound too
						if (lowerUpper.length > 1 && lowerUpper[1].length() > 0)
							high = Integer.parseInt(lowerUpper[1]);

						if (low < 0 || low > high)
							throw new CoreException(new Status(IStatus.ERROR, LaunchPlugin.PLUGIN_ID, "Invalid run number or range: "+current));

						if (maxRunNo != -1 && high >= maxRunNo)
							throw new CoreException(new Status(IStatus.ERROR, LaunchPlugin.PLUGIN_ID, "Run number ("+current+") is greater than the number of runs ("+maxRunNo+") supported by the current configuration"));


						// add all integers in the interval to the list
						int limitedHigh = maxRunNo == -1 ? low + 1 : high;

						for (int i = low; i<=limitedHigh; ++i)
							result.add(i);
					}
					else {
						int number = Integer.parseInt(current);
						if (maxRunNo != -1 && number >= maxRunNo)
							throw new CoreException(new Status(IStatus.ERROR, LaunchPlugin.PLUGIN_ID, "Run number ("+current+") is greater than the number of runs ("+maxRunNo+") supported by the current configuration"));
						result.add(number);
					}
				} catch (NumberFormatException e) {
					throw new CoreException(new Status(IStatus.ERROR, LaunchPlugin.PLUGIN_ID, "Invalid run number or range: "+current+". The run number syntax: 0,2,7,9..11 or use * for all runs"));
				}
			}
		}

		int[] resArray = new int [result.size()];
		for (int i = 0; i< resArray.length; i++)
			resArray[i] = result.get(i);

		// check for duplicate run numbers
		int[] sortedRes = ArrayUtils.clone(resArray);
		Arrays.sort(sortedRes);
		for(int i = 0; i< sortedRes.length-1; i++)
			if (sortedRes[i] == sortedRes[i+1])
				throw new CoreException(new Status(IStatus.ERROR, LaunchPlugin.PLUGIN_ID, "Duplicate run number: "+sortedRes[i]));

		return resArray;
	}

	/**
	 * Starts the simulation program.
	 */
	public static Process startSimulationProcess(ILaunchConfiguration configuration, 
	                     String[] cmdLine, boolean requestInfo) throws CoreException {
		// Debug.println("starting with command line: "+StringUtils.join(cmdLine," "));

		if (requestInfo) {
			int i = ArrayUtils.indexOf(cmdLine, "-c");
			if (i >= 0)
				cmdLine[i] = "-x";   // replace the -c with -x
			else {
				cmdLine = (String[]) ArrayUtils.add(cmdLine, 1, "-x");
				cmdLine = (String[]) ArrayUtils.add(cmdLine, 2, "General");
			}

			// replace the envir command line option to use Cmdenv (or add if not exist)
			i = ArrayUtils.indexOf(cmdLine, "-u");
			if (i >= 0 && cmdLine.length > i+1)
				cmdLine[i+1] = "Cmdenv";
			else {
				cmdLine = (String[]) ArrayUtils.add(cmdLine, 1, "-u");
				cmdLine = (String[]) ArrayUtils.add(cmdLine, 2, "Cmdenv");
			}

			cmdLine = (String[]) ArrayUtils.add(cmdLine, 1, "-g");
		}

        String environment[] = DebugPlugin.getDefault().getLaunchManager().getEnvironment(configuration);
		return DebugPlugin.exec(cmdLine, new File(getWorkingDirectoryPath(configuration).toString()), environment);
	}

	/**
	 * Create a full command line (including executable path/name) from the provided configuration.
	 * Additional arguments are right after the exe name so they can override the parameters
	 * specified in the configuration.
	 */
	public static String[] createCommandLine(ILaunchConfiguration configuration, String additionalArgs) throws CoreException {
		String projAttr = configuration.getAttribute(IOmnetppLaunchConstants.ATTR_PROJECT_NAME, "");
		String progAttr = configuration.getAttribute(IOmnetppLaunchConstants.ATTR_PROGRAM_NAME, "");
		String argAttr = configuration.getAttribute(IOmnetppLaunchConstants.ATTR_PROGRAM_ARGUMENTS, "");
		String expandedProj = StringUtils.substituteVariables(projAttr);
		String expandedProg = StringUtils.substituteVariables(progAttr);
		String expandedArg = StringUtils.substituteVariables(argAttr);
		IPath projPath = new Path(expandedProj);
		IPath progPath = new Path(expandedProg);
		// put the additional arguments at the beginning so they override the other arguments
		expandedArg = additionalArgs +" "+expandedArg;
		String programLocation = expandedProg;
		// if it is workspace relative path, resolve it against the workspace and get the physical location
		if (!progPath.isAbsolute() ) {
			IFile executableFile = ResourcesPlugin.getWorkspace().getRoot().getFile(projPath.append(progPath));
			if (executableFile == null)
				throw new CoreException(Status.CANCEL_STATUS);
			programLocation = executableFile.getRawLocation().makeAbsolute().toString();
		}
		String cmdLine[] =DebugPlugin.parseArguments(programLocation + " " + expandedArg);
		return cmdLine;
	}

	/**
	 * Returns a string describing all runs in the scenario, or "" if an error occurred
	 */
	public static String getSimulationRunInfo(ILaunchConfiguration configuration) {
		try {
		    // launch the program
		    long startTime = System.currentTimeMillis();
			configuration = convertLaunchConfig(configuration, ILaunchManager.RUN_MODE);
			Process proc = OmnetppLaunchUtils.startSimulationProcess(configuration, createCommandLine(configuration, ""), true);

			// read its standard output
			final int BUFFERSIZE = 8192;
			byte bytes[] = new byte[BUFFERSIZE];
			StringBuffer stringBuffer = new StringBuffer(BUFFERSIZE);
			BufferedInputStream is = new BufferedInputStream(proc.getInputStream(), BUFFERSIZE);
			int lastRead = 0;
			while ((lastRead = is.read(bytes)) > 0)
				stringBuffer.append(new String(bytes, 0, lastRead));
			Debug.println("getSimulationRunInfo: read " + stringBuffer.length() + " bytes of program output in " + (System.currentTimeMillis() - startTime) + "ms");

			// wait until it exits
			proc.waitFor();
			Debug.println("getSimulationRunInfo: program exited after total " + (System.currentTimeMillis() - startTime) + "ms");

			String simulationInfo = stringBuffer.toString().replace("\r", ""); // CRLF to LF conversion

			//FIXME parse out errors: they are the lines that start with "<!>" -- e.g. inifile might contain a syntax error etc --Andras
			if (proc.exitValue() == 0)
				return "Number of runs: "+StringUtils.trimToEmpty(StringUtils.substringBetween(simulationInfo, "Number of runs:", "\n\n"));
			
		} 
		catch (CoreException e) {
			// do not litter the log with the error: this method often fails because it is often
		    // called while the user edits the configuration, and moreover it is only used to 
		    // bring up a tooltip
		} 
		catch (IOException e) {
			LaunchPlugin.logError("Error getting output stream from the executable", e);
		} 
		catch (InterruptedException e) {
			LaunchPlugin.logError("Error: thread interrupted while waiting for process to exit", e);
		}
		return "";
	}

	/**
	 * Returns the number of runs available in the given scenario
	 */
	public static int getMaxNumberOfRuns(ILaunchConfiguration configuration) {
		return NumberUtils.toInt(StringUtils.trimToEmpty(
				StringUtils.substringBetween(
						getSimulationRunInfo(configuration), "Number of runs:", "\n")), 1);
	}

	/**
	 * TODO document
	 * @param text The text to be parsed for progress information
	 * @return The process progress reported in the text or -1 if no progress info found
	 */
	public static int getProgressInPercent(String text) {
		String tag = "% completed";
		if (!StringUtils.contains(text, tag))
			return -1;
		String pctStr = StringUtils.substringAfterLast(StringUtils.substringBeforeLast(text, tag)," ");
		return NumberUtils.toInt(pctStr, -1);
	}

	/**
	 * @param text The process output
	 * @return Whether the end of the text indicates that the process is waiting fore user input
	 */
	public static boolean isWaitingForUserInput(String text) {
		// FIXME this is not correct because the user can specify any prompt text.
		// we should have a consistent marker char/tag during user input
		return text.contains("Enter parameter");
	}


}
