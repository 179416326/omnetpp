package org.omnetpp.ned.editor.graph.properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.omnetpp.common.displaymodel.DisplayString;
import org.omnetpp.ned.editor.graph.properties.util.DelegatingPropertySource;
import org.omnetpp.ned.editor.graph.properties.util.DisplayPropertySource;
import org.omnetpp.ned.editor.graph.properties.util.ExtendsPropertySource;
import org.omnetpp.ned.editor.graph.properties.util.InterfacesListPropertySource;
import org.omnetpp.ned.editor.graph.properties.util.MergedPropertySource;
import org.omnetpp.ned.editor.graph.properties.util.NamePropertySource;
import org.omnetpp.ned.editor.graph.properties.util.ParameterListPropertySource;
import org.omnetpp.ned2.model.ex.ChannelNodeEx;
import org.omnetpp.resources.NEDResourcesPlugin;

/**
 * @author rhornig
 * Property source for channels
 */
public class ChannelPropertySource extends MergedPropertySource {

    protected static class ChannelDisplayPropertySource extends DisplayPropertySource {
        protected ChannelNodeEx model;

        public ChannelDisplayPropertySource(ChannelNodeEx model) {
            super(model);
            this.model = model;
            setDisplayString(model.getDisplayString());
            // define which properties should be displayed in the property sheet
            // we do not support all properties currently, just colow, width ans style
            supportedProperties.addAll(EnumSet.range(DisplayString.Prop.CONNECTION_COL, 
                    								 DisplayString.Prop.CONNECTION_STYLE));
            supportedProperties.addAll(EnumSet.range(DisplayString.Prop.TEXT, DisplayString.Prop.TEXTPOS));
            supportedProperties.add(DisplayString.Prop.TOOLTIP);
        }

    }

    /**
     * Constructor
     * @param nodeModel
     */
    public ChannelPropertySource(ChannelNodeEx nodeModel) {
    	super(nodeModel);
        mergePropertySource(new NamePropertySource(nodeModel));
        // extends
        mergePropertySource(new ExtendsPropertySource(nodeModel) {
            @Override
            protected List<String> getPossibleValues() {
              List<String> names = new ArrayList<String>(NEDResourcesPlugin.getNEDResources().getChannelNames());
              Collections.sort(names);
              return names;
            }
        });
        // interfaces
        mergePropertySource(new DelegatingPropertySource(
                new InterfacesListPropertySource(nodeModel),
                InterfacesListPropertySource.CATEGORY,
                InterfacesListPropertySource.DESCRIPTION));
        // parameters
        mergePropertySource(new DelegatingPropertySource(
                new ParameterListPropertySource(nodeModel),
                ParameterListPropertySource.CATEGORY,
                ParameterListPropertySource.DESCRIPTION));
        // create a displayPropertySource
        mergePropertySource(new ChannelDisplayPropertySource(nodeModel));
    }

}
