/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.omnetpp.scave.wizard;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.edit.ui.provider.ExtendedImageRegistry;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.omnetpp.scave.model.ScaveModelFactory;
import org.omnetpp.scave.model.ScaveModelPackage;
import org.omnetpp.scave.model.provider.ScaveEditPlugin;


/**
 * This is a simple wizard for creating a new model file.
 * Code is based on the EMF-created wizard.
 */
//XXX uses property file from the model package
public class ScaveModelWizard extends Wizard implements INewWizard {
	
	private static final String INITIAL_OBJECT_NAME = "Analysis";
	private static final String ENCODING = "UTF-8";
	private static final String FILENAME_EXTENSION = "scave";
	private static final String BASEFILENAME = "new"; // default filename: "new.scave"

	/**
	 * This caches an instance of the model package.
	 */
	protected ScaveModelPackage scaveModelPackage = ScaveModelPackage.eINSTANCE;

	/**
	 * This caches an instance of the model factory.
	 */
	protected ScaveModelFactory scaveModelFactory = scaveModelPackage.getScaveModelFactory();

	/**
	 * This is the file creation page.
	 */
	protected NewFileCreationPage newFileCreationPage;

	/**
	 * Remember the selection during initialization for populating the default container.
	 */
	protected IStructuredSelection selection;

	/**
	 * Remember the workbench during initialization.
	 */
	protected IWorkbench workbench;

	/**
	 * Caches the names of the types that can be created as the root object.
	 */
	protected List initialObjectNames;

	/**
	 * This just records the information.
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		this.selection = selection;
		setWindowTitle(ScaveEditPlugin.INSTANCE.getString("_UI_Wizard_label"));
		setDefaultPageImageDescriptor(ExtendedImageRegistry.INSTANCE.getImageDescriptor(ScaveEditPlugin.INSTANCE.getImage("full/wizban/NewScaveModel")));
	}

	/**
	 * Returns the names of the types that can be created as the root object.
	 */
	protected Collection getInitialObjectNames() {
		if (initialObjectNames == null) {
			initialObjectNames = new ArrayList();
			for (Iterator classifiers = scaveModelPackage.getEClassifiers().iterator(); classifiers.hasNext(); ) {
				EClassifier eClassifier = (EClassifier)classifiers.next();
				if (eClassifier instanceof EClass) {
					EClass eClass = (EClass)eClassifier;
					if (!eClass.isAbstract()) {
						initialObjectNames.add(eClass.getName());
					}
				}
			}
			Collections.sort(initialObjectNames, java.text.Collator.getInstance());
		}
		return initialObjectNames;
	}

	/**
	 * Create a new model.
	 */
	protected EObject createInitialModel() {
		EClass eClass = (EClass)scaveModelPackage.getEClassifier(INITIAL_OBJECT_NAME);
		EObject rootObject = scaveModelFactory.create(eClass);
		return rootObject;
	}

	/**
	 * Do the work after everything is specified.
	 */
	public boolean performFinish() {
		try {
			// Remember the file.
			//
			final IFile modelFile = getModelFile();

			// Do the work within an operation.
			//
			WorkspaceModifyOperation operation =
				new WorkspaceModifyOperation() {
					protected void execute(IProgressMonitor progressMonitor) {
						try {
							// Create a resource set
							//
							ResourceSet resourceSet = new ResourceSetImpl();

							// Get the URI of the model file.
							//
							URI fileURI = URI.createPlatformResourceURI(modelFile.getFullPath().toString());

							// Create a resource for this file.
							//
							Resource resource = resourceSet.createResource(fileURI);

							// Add the initial model object to the contents.
							//
							EObject rootObject = createInitialModel();
							if (rootObject != null) {
								resource.getContents().add(rootObject);
							}

							// Save the contents of the resource to the file system.
							//
							Map options = new HashMap();
							options.put(XMLResource.OPTION_ENCODING, ENCODING);
							resource.save(options);
						}
						catch (Exception exception) {
							ScaveEditPlugin.INSTANCE.log(exception);
						}
						finally {
							progressMonitor.done();
						}
					}
				};

			getContainer().run(false, false, operation);

			// Select the new file resource in the current view.
			//
			IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();
			IWorkbenchPage page = workbenchWindow.getActivePage();
			final IWorkbenchPart activePart = page.getActivePart();
			if (activePart instanceof ISetSelectionTarget) {
				final ISelection targetSelection = new StructuredSelection(modelFile);
				getShell().getDisplay().asyncExec
					(new Runnable() {
						 public void run() {
							 ((ISetSelectionTarget)activePart).selectReveal(targetSelection);
						 }
					 });
			}

			// Open an editor on the new file.
			//
			try {
				page.openEditor
					(new FileEditorInput(modelFile),
					 workbench.getEditorRegistry().getDefaultEditor(modelFile.getFullPath().toString()).getId());
			}
			catch (PartInitException exception) {
				MessageDialog.openError(workbenchWindow.getShell(), ScaveEditPlugin.INSTANCE.getString("_UI_OpenEditorError_label"), exception.getMessage());
				return false;
			}

			return true;
		}
		catch (Exception exception) {
			ScaveEditPlugin.INSTANCE.log(exception);
			return false;
		}
	}

	/**
	 * This is the one page of the wizard.
	 */
	public class NewFileCreationPage extends WizardNewFileCreationPage {
		/**
		 * Pass in the selection.
		 */
		public NewFileCreationPage(String pageId, IStructuredSelection selection) {
			super(pageId, selection);
		}

		/**
		 * The framework calls this to see if the file is correct.
		 */
		protected boolean validatePage() {
			if (super.validatePage()) {
				// Make sure the file ends in ".scavemodel".
				//
				String requiredExt = ScaveEditPlugin.INSTANCE.getString("_UI_ScaveModelEditorFilenameExtension");
				String enteredExt = new Path(getFileName()).getFileExtension();
				if (enteredExt == null || !enteredExt.equals(requiredExt)) {
					setErrorMessage(ScaveEditPlugin.INSTANCE.getString("_WARN_FilenameExtension", new Object [] { requiredExt }));
					return false;
				}
				else {
					return true;
				}
			}
			else {
				return false;
			}
		}

		/**
		 * 
		 */
		public IFile getModelFile() {
			return ResourcesPlugin.getWorkspace().getRoot().getFile(getContainerFullPath().append(getFileName()));
		}
	}

	/**
	 * The framework calls this to create the contents of the wizard.
	 */
	public void addPages() {
		// Create a page, set the title, and the initial model file name.
		//
		newFileCreationPage = new NewFileCreationPage("Whatever", selection);
		newFileCreationPage.setTitle(ScaveEditPlugin.INSTANCE.getString("_UI_ScaveModelModelWizard_label"));
		newFileCreationPage.setDescription(ScaveEditPlugin.INSTANCE.getString("_UI_ScaveModelModelWizard_description"));
		newFileCreationPage.setFileName(ScaveEditPlugin.INSTANCE.getString("_UI_ScaveModelEditorFilenameDefaultBase") + "." + ScaveEditPlugin.INSTANCE.getString("_UI_ScaveModelEditorFilenameExtension"));
		addPage(newFileCreationPage);

		// Try and get the resource selection to determine a current directory for the file dialog.
		//
		if (selection != null && !selection.isEmpty()) {
			// Get the resource...
			//
			Object selectedElement = selection.iterator().next();
			if (selectedElement instanceof IResource) {
				// Get the resource parent, if its a file.
				//
				IResource selectedResource = (IResource)selectedElement;
				if (selectedResource.getType() == IResource.FILE) {
					selectedResource = selectedResource.getParent();
				}

				// This gives us a directory...
				//
				if (selectedResource instanceof IFolder || selectedResource instanceof IProject) {
					// Set this for the container.
					//
					newFileCreationPage.setContainerFullPath(selectedResource.getFullPath());

					// Make up a unique new name here.
					//
					String defaultModelBaseFilename = BASEFILENAME;
					String defaultModelFilenameExtension = FILENAME_EXTENSION;
					String modelFilename = defaultModelBaseFilename + "." + defaultModelFilenameExtension;
					for (int i = 1; ((IContainer)selectedResource).findMember(modelFilename) != null; ++i) {
						modelFilename = defaultModelBaseFilename + i + "." + defaultModelFilenameExtension;
					}
					newFileCreationPage.setFileName(modelFilename);
				}
			}
		}
	}

	/**
	 * Get the file from the page.
	 */
	public IFile getModelFile() {
		return newFileCreationPage.getModelFile();
	}

}
