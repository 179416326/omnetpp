/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.editor.graph.misc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.swt.widgets.Display;
import org.omnetpp.common.util.DisplayUtils;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.ned.core.NedResourcesPlugin;
import org.omnetpp.ned.editor.graph.parts.NedEditPart;
import org.omnetpp.ned.editor.graph.parts.NedEditPartFactory;
import org.omnetpp.ned.editor.graph.parts.NedFileEditPart;
import org.omnetpp.ned.model.INedElement;
import org.omnetpp.ned.model.ex.NedFileElementEx;
import org.omnetpp.ned.model.interfaces.INedTypeElement;

import de.unikassel.imageexport.providers.AbstractFigureProvider;

/**
 * Figure provider for image export plugins, returns all compound module figures
 * in the given file, along with their names.
 *
 * @author rhornig
 */
public class NedFigureProvider extends AbstractFigureProvider {
    public static ScrollingGraphicalViewer createNedViewer(INedElement model) {
        ScrollingGraphicalViewer viewer = new ScrollingGraphicalViewer();
        viewer.setEditPartFactory(new NedEditPartFactory());
        viewer.setContents(model);
        NedFileEditPart nedFilePart = (NedFileEditPart)viewer.getEditPartRegistry().get(model);
        if (nedFilePart == null)
            throw new IllegalArgumentException("Invalid NED file.");
        // root figure is not added to the viewer because of off screen rendering
        // we have to pretend the addition otherwise add notification will not be sent to children
        IFigure rootFigure = nedFilePart.getFigure();
        rootFigure.addNotify();
        rootFigure.setBounds(new Rectangle(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE));
        rootFigure.setBorder(null);
        rootFigure.setFont(Display.getDefault().getSystemFont());
        rootFigure.validate();

        return viewer;
    }

    @SuppressWarnings("unchecked")
    public Map<IFigure, String> provideExportImageFigures(final IFile diagramFile) {
        final Map<IFigure, String>[] results = new Map[1];

        DisplayUtils.runNowOrSyncInUIThread(new java.lang.Runnable() {
            public void run() {
                Map<IFigure, String> result = new HashMap<IFigure, String>();
                NedFileElementEx modelRoot = NedResourcesPlugin.getNedResources().getNedFileElement(diagramFile);
                ScrollingGraphicalViewer viewer = createNedViewer(modelRoot);

                // count the number of type. if only a single type
                // present and its name is the same as the filename, we will use only that name
                // otherwise filename_modulename is the syntax
                List<INedTypeElement> typeElements = modelRoot.getTopLevelTypeNodes();

                for (INedTypeElement typeElement : typeElements) {
                    NedEditPart editPart = (NedEditPart)viewer.getEditPartRegistry().get(typeElement);
                    String filebasename = StringUtils.chomp(diagramFile.getName(), "."+diagramFile.getFileExtension());
                    String imageName = getFigureName(typeElements, typeElement, filebasename);
                    result.put(editPart.getFigure(),  imageName);
                }

                results[0] = result;
            }
        });

        return results[0];
    }

    public static String getFigureName(List<INedTypeElement> typeElements, INedTypeElement typeElement, String fileBaseName) {
        return typeElements.size() == 1 && typeElement.getName().endsWith(fileBaseName) ?
            typeElement.getName() : fileBaseName + "_" + typeElement.getName();
    }
}
