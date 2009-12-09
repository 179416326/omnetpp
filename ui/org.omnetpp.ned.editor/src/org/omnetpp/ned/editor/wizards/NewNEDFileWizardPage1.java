/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.editor.wizards;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.ide.IDE;
import org.omnetpp.common.util.LicenseUtils;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.ned.core.NEDResourcesPlugin;
import org.omnetpp.ned.editor.NedEditorPlugin;
import org.omnetpp.ned.model.interfaces.INEDTypeResolver;

/**
 * Wizard page for creating a new NED file
 *
 * @author rhornig, andras
 */
public class NewNEDFileWizardPage1 extends WizardNewFileCreationPage {
    private static final String COMMENT = "//\n// TODO auto-generated module\n//\n";

    private static final String[] NEDFILE_TEMPLATES = {
        "#PACKAGEDECL#",
        "#PACKAGEDECL#"+COMMENT+"simple #NAME#\n{\n}\n",
        "#PACKAGEDECL#"+COMMENT+"module #NAME#\n{\n}\n",
        "#PACKAGEDECL#"+COMMENT+"network #NAME#\n{\n}\n"
    };

	private IWorkbench workbench;
	private static int exampleCount = 1;

	private Button emptyButton = null;
    private Button simpleButton = null;
    private Button compoundButton = null;
    private Button networkButton = null;
	private int modelSelected = 0;

	public NewNEDFileWizardPage1(IWorkbench aWorkbench, IStructuredSelection selection) {
		super("page1", selection);
		setTitle("Create a NED file");
		setDescription("This wizard allows you to create a new network description file");
		setImageDescriptor(ImageDescriptor.createFromFile(getClass(),"/icons/newnedfile_wiz.png"));

		setFileExtension("ned");
		setFileName("new" + exampleCount + ".ned");
		workbench = aWorkbench;
	}

	@Override
    public void createControl(Composite parent) {
		super.createControl(parent);

		Composite composite = (Composite) getControl();

		// sample section generation group
		Group group = new Group(composite, SWT.NONE);
		group.setLayout(new GridLayout());
		group.setText("Content");
		group.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

		SelectionListener listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.getSource() == emptyButton) {
					modelSelected = 0;
				}
				else if (e.getSource() == simpleButton) {
					modelSelected = 1;
				}
				else if (e.getSource() == compoundButton) {
                    modelSelected = 2;
                }
                else if (e.getSource() == networkButton) {
                    modelSelected = 3;
                }
			}
		};

		// sample section generation checkboxes
		emptyButton = new Button(group, SWT.RADIO);
		emptyButton.setText("Em&pty file");
		emptyButton.addSelectionListener(listener);
		emptyButton.setSelection(true);

		simpleButton = new Button(group, SWT.RADIO);
		simpleButton.setText("A new &simple module");
		simpleButton.addSelectionListener(listener);

        compoundButton = new Button(group, SWT.RADIO);
        compoundButton.setText("A new &compound module");
        compoundButton.addSelectionListener(listener);

        networkButton = new Button(group, SWT.RADIO);
        networkButton.setText("A new &network");
        networkButton.addSelectionListener(listener);

        //new Label(composite, SWT.NONE);

		setPageComplete(validatePage());
	}

	@Override
    protected InputStream getInitialContents() {
        String name = getFileName();
        if (name == null || "".equals(name))
            return null;

        // make a valid identifier
        name = name.substring(0, name.lastIndexOf('.'));
        name = StringUtils.capitalize(StringUtils.makeValidIdentifier(name));

        // determine package
        IFile newFile = createFileHandle(getContainerFullPath().append(getFileName()));
        String packagedecl = NEDResourcesPlugin.getNEDResources().getExpectedPackageFor(newFile);
        packagedecl = StringUtils.isNotEmpty(packagedecl) ? "package "+packagedecl+";\n\n" : "";

        // substitute name and package into the template
        String contents = NEDFILE_TEMPLATES[modelSelected].replaceAll("#NAME#", name);
		contents = contents.replaceAll("#PACKAGEDECL#", packagedecl);

		// prefix with banner comment
		String license = NEDResourcesPlugin.getNEDResources().getSimplePropertyFor(newFile.getParent(), INEDTypeResolver.LICENSE_PROPERTY);
		if (license==null || !LicenseUtils.isAcceptedLicense(license))
		    license = LicenseUtils.getDefaultLicense();
        contents = LicenseUtils.getBannerComment(license, "//") + contents;
		return new ByteArrayInputStream(contents.getBytes());
	}

	public boolean finish() {
        IFile newFile = createNewFile();
		if (newFile == null)
			return false; // creation was unsuccessful

		// Since the file resource was created fine, open it for editing
		// if requested by the user
		try {
			IWorkbenchWindow dwindow = workbench.getActiveWorkbenchWindow();
			IWorkbenchPage page = dwindow.getActivePage();
			if (page != null)
				IDE.openEditor(page, newFile, true);
		} catch (org.eclipse.ui.PartInitException e) {
			NedEditorPlugin.logError(e);
			return false;
		}
		exampleCount++;
		return true;
	}

}
