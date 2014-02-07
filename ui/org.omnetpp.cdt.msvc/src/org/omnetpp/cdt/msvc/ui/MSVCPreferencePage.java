/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.cdt.msvc.ui;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.omnetpp.cdt.Activator;

/**
 * Preference page to specify Visual Studio and Platform SDK locations.
 */
public class MSVCPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String PREFKEY_VCDIR = "vcdir";
    public static final String PREFKEY_VSDIR = "vsdir";
    public static final String PREFKEY_SDKDIR = "sdkdir";
    public static final String PREFKEY_APPENDPATH = "appendpath";

    public MSVCPreferencePage() {
        super(GRID);
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription("Specify the location of Microsoft Visual C++ and the Platform SDK. " +
                "This will affect the build-time INCLUDE, LIB and PATH environment variables, " +
                "and the discovered include paths.");
    }

    public void createFieldEditors() {
        // TODO explanation, tooltips
        addField(new DirectoryFieldEditor(PREFKEY_VSDIR, "Visual Studio install location:", getFieldEditorParent()));
        addField(new DirectoryFieldEditor(PREFKEY_VCDIR, "Visual C++ location:", getFieldEditorParent()));
        addField(new DirectoryFieldEditor(PREFKEY_SDKDIR, "Platform SDK location:", getFieldEditorParent()));
        addField(new BooleanFieldEditor(PREFKEY_APPENDPATH, "Append PATH environment variable to build-time path", getFieldEditorParent()));
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
    }

}