/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.
  
  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.editor.graph.properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.eclipse.core.resources.IProject;

import org.omnetpp.ned.core.NEDResourcesPlugin;
import org.omnetpp.ned.editor.graph.properties.util.DelegatingPropertySource;
import org.omnetpp.ned.editor.graph.properties.util.DisplayPropertySource;
import org.omnetpp.ned.editor.graph.properties.util.GateListPropertySource;
import org.omnetpp.ned.editor.graph.properties.util.MergedPropertySource;
import org.omnetpp.ned.editor.graph.properties.util.NamePropertySource;
import org.omnetpp.ned.editor.graph.properties.util.ParameterListPropertySource;
import org.omnetpp.ned.editor.graph.properties.util.SubmoduleNameValidator;
import org.omnetpp.ned.editor.graph.properties.util.TypePropertySource;
import org.omnetpp.ned.model.DisplayString;
import org.omnetpp.ned.model.ex.SubmoduleElementEx;

/**
 * Properties of the submodule element
 *
 * @author rhornig
 */
public class SubmodulePropertySource extends MergedPropertySource {

	// submodule specific display property description
    protected static class SubmoduleDisplayPropertySource extends DisplayPropertySource {

        public SubmoduleDisplayPropertySource(SubmoduleElementEx model) {
            super(model);
            supportedProperties.addAll( EnumSet.range(DisplayString.Prop.X,
            										  DisplayString.Prop.TOOLTIP));
        }

    }

    public SubmodulePropertySource(final SubmoduleElementEx submoduleNodeModel) {
        super(submoduleNodeModel);
        // create a nested displayPropertySource
        // name
        mergePropertySource(new NamePropertySource(submoduleNodeModel,
                new SubmoduleNameValidator(submoduleNodeModel)));
        // type
        mergePropertySource(new TypePropertySource(submoduleNodeModel) {
            @Override
            protected List<String> getPossibleValues() {
                IProject project = NEDResourcesPlugin.getNEDResources().getNedFile(submoduleNodeModel.getContainingNedFileElement()).getProject();
                List<String> moduleNames = new ArrayList<String>(NEDResourcesPlugin.getNEDResources().getModuleQNames(project));
                Collections.sort(moduleNames);
                return moduleNames;
            }
        });
        // parameters
        mergePropertySource(new DelegatingPropertySource(
                new ParameterListPropertySource(submoduleNodeModel),
                ParameterListPropertySource.CATEGORY,
                ParameterListPropertySource.DESCRIPTION));
        // gates
        mergePropertySource(new DelegatingPropertySource(
                new GateListPropertySource(submoduleNodeModel),
                GateListPropertySource.CATEGORY,
                GateListPropertySource.DESCRIPTION));
        // display
        mergePropertySource(new SubmoduleDisplayPropertySource(submoduleNodeModel));
    }

}
