package org.omnetpp.ned.editor.graph.properties;

import org.omnetpp.ned.editor.graph.properties.util.DelegatingPropertySource;
import org.omnetpp.ned.editor.graph.properties.util.ExtendsListPropertySource;
import org.omnetpp.ned.editor.graph.properties.util.MergedPropertySource;
import org.omnetpp.ned.editor.graph.properties.util.NamePropertySource;
import org.omnetpp.ned.editor.graph.properties.util.ParameterListPropertySource;
import org.omnetpp.ned.model.ex.ChannelInterfaceNodeEx;

/**
 * @author rhornig
 * Property source for channel interfaces
 */
public class ChannelInterfacePropertySource extends MergedPropertySource {

    public ChannelInterfacePropertySource(ChannelInterfaceNodeEx nodeModel) {
    	super(nodeModel);
        mergePropertySource(new NamePropertySource(nodeModel));
        mergePropertySource(new DelegatingPropertySource(
                new ExtendsListPropertySource(nodeModel),
                ExtendsListPropertySource.CATEGORY,
                ExtendsListPropertySource.DESCRIPTION));
        mergePropertySource(new DelegatingPropertySource(
                new ParameterListPropertySource(nodeModel),
                ParameterListPropertySource.CATEGORY,
                ParameterListPropertySource.DESCRIPTION));
    }

}
