/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.
  
  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package com.simulcraft.test.gui.util;

import java.io.ByteArrayInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.omnetpp.common.util.FileUtils;
import org.omnetpp.common.util.StringUtils;

import com.simulcraft.test.gui.core.UIStep;
import com.simulcraft.test.gui.core.InBackgroundThread;

public class WorkspaceUtils
{
	@UIStep
	public static IProject assertProjectExists(String name) {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		Assert.assertTrue("project does not exist", project.exists());
		return project;
	}

	@UIStep
	public static IProject assertProjectNotExists(String name) {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		Assert.assertTrue("project still exists", !project.exists());
		return project;
	}

	@UIStep
	public static IFolder assertFolderExists(String path) {
		IFolder folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(new Path(path));
		Assert.assertTrue("folder does not exist", folder.exists());
		return folder;
	}

	@UIStep
	public static IFolder assertFolderNotExists(String path) {
		IFolder folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(new Path(path));
		Assert.assertTrue("folder still exists", !folder.exists());
		return folder;
	}

	@UIStep
	public static IFile assertFileExists(String path) {
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(path));
		Assert.assertTrue("file does not exist", file.exists());
		return file;
	}

	@UIStep
	public static IFile assertFileNotExists(String path) {
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(path));
		Assert.assertTrue("file still exists", !file.exists());
		return file;
	}

	@UIStep
	public static void assertFileExistsWithContent(String path, String expectedContent) throws Exception {
		IFile file = assertFileExists(path);
		String actualContent = FileUtils.readTextFile(file.getContents());
		Assert.assertTrue("file content: " + actualContent + " differs from expected: " + expectedContent, actualContent.equals(expectedContent));
	}

    @UIStep
    public static void assertFileExistsWithContentIgnoringWhiteSpace(String path, String expectedContent) throws Exception {
        IFile file = assertFileExists(path);
        String actualContent = FileUtils.readTextFile(file.getContents());
        Assert.assertTrue("file content: " + actualContent + " differs from expected: " + expectedContent, StringUtils.areEqualIgnoringWhiteSpace(actualContent, expectedContent));
    }

    @UIStep
    public static void assertFileExistsWithRegexpContent(String path, String expectedRegexpContent) throws Exception {
        IFile file = assertFileExists(path);
        String actualContent = FileUtils.readTextFile(file.getContents());
        Assert.assertTrue("file content: " + actualContent + " differs from expected: " + expectedRegexpContent, matchesRegexp(actualContent, expectedRegexpContent));
    }
    
	@InBackgroundThread
	public static void createFileWithContent(String path, String content) throws Exception {
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(path));
		if (file.exists()) 
		    file.delete(true, null);
		file.create(new ByteArrayInputStream(content.getBytes()), true, null);
		WorkspaceUtils.assertFileExistsWithContent(path, content); // wait until background job finishes
	}

	public static void ensureFileNotExists(String path) throws CoreException {
        IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(path));
	    if (file.exists())
	        file.delete(true, null);
	}

    public static void ensureFolderExists(String path) throws CoreException {
        IFolder folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(new Path(path));
        if (!folder.exists())
            folder.create(true, false, null);
    }

    public static void ensureFolderNotExists(String path) throws CoreException {
        IFolder folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(new Path(path));
        if (folder.exists())
            folder.delete(true, null);
    }

    public static void ensureFileNotExists(String projectName, String fileName) throws CoreException {
        IFile file = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName).getFile(fileName);
        if (file.exists())
            file.delete(true, null);
    }

    public static void ensureFolderExists(String projectName, String directoryName) throws CoreException {
        IFolder folder = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName).getFolder(directoryName);
        if (!folder.exists())
            folder.create(true, false, null);
    }

    public static void ensureProjectNotExists(String projectName) throws CoreException {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        if (project != null)
            project.delete(true, null);
    }
    
    //XXX this function probably not needed, can be replaced by prepending the regex with "(?s)"
    public static boolean matchesRegexp(String text, String regexp) {
        Pattern pattern = Pattern.compile(regexp, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        return matcher.matches();
    }
}
