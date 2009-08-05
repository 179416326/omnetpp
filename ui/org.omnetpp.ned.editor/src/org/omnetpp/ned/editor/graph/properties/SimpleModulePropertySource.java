package org.omnetpp.ned.editor.graph.properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource2;
import org.omnetpp.common.displaymodel.DisplayString;
import org.omnetpp.common.properties.CheckboxPropertyDescriptor;
import org.omnetpp.ned.core.NEDResourcesPlugin;
import org.omnetpp.ned.editor.graph.properties.util.DelegatingPropertySource;
import org.omnetpp.ned.editor.graph.properties.util.DisplayPropertySource;
import org.omnetpp.ned.editor.graph.properties.util.ExtendsPropertySource;
import org.omnetpp.ned.editor.graph.properties.util.GateListPropertySource;
import org.omnetpp.ned.editor.graph.properties.util.InterfacesListPropertySource;
import org.omnetpp.ned.editor.graph.properties.util.MergedPropertySource;
import org.omnetpp.ned.editor.graph.properties.util.NamePropertySource;
import org.omnetpp.ned.editor.graph.properties.util.ParameterListPropertySource;
import org.omnetpp.ned.model.ex.SimpleModuleNodeEx;

/**
 * TODO add documentation
 *
 * @author rhornig
 */
public class SimpleModulePropertySource extends MergedPropertySource {

    protected static class SimpleModuleDisplayPropertySource extends DisplayPropertySource {
        protected SimpleModuleNodeEx model;

        public SimpleModuleDisplayPropertySource(SimpleModuleNodeEx model) {
            super(model);
            this.model = model;
            setDisplayString(model.getDisplayString());
            supportedProperties.addAll( EnumSet.range(DisplayString.Prop.WIDTH, 
            										  DisplayString.Prop.IMAGESIZE));
        }

    }

    // simple module specific properties
    protected static class BasePropertySource implements IPropertySource2 {
        public static final String BASE_CATEGORY = "Base";
        public enum Prop { Network }
        protected IPropertyDescriptor[] descriptors;
        protected SimpleModuleNodeEx model;
        CheckboxPropertyDescriptor networkProp;

        public BasePropertySource(SimpleModuleNodeEx connectionNodeModel) {
            model = connectionNodeModel;
            
            // set up property descriptors
            networkProp = new CheckboxPropertyDescriptor(Prop.Network, "network");
            networkProp.setCategory(BASE_CATEGORY);
            networkProp.setDescription("Is this simple module used as a network instance?");

            descriptors = new IPropertyDescriptor[] { networkProp  };
        }

        public Object getEditableValue() {
            return null;
        }

        public IPropertyDescriptor[] getPropertyDescriptors() {
            return descriptors;
        }

        public Object getPropertyValue(Object propName) {
            if (Prop.Network.equals(propName))  
                return model.getIsNetwork(); 

            return null;
        }

        public void setPropertyValue(Object propName, Object value) {
            if (Prop.Network.equals(propName)) 
                model.setIsNetwork((Boolean)value);
        }

        public boolean isPropertySet(Object propName) {
            return true;
        }

        public void resetPropertyValue(Object propName) {
        }

        public boolean isPropertyResettable(Object propName) {
            return false;
        }
    }

    public SimpleModulePropertySource(SimpleModuleNodeEx nodeModel) {
        super(nodeModel);
        //create name
        mergePropertySource(new NamePropertySource(nodeModel));
        mergePropertySource(new BasePropertySource(nodeModel));
        // extends
        mergePropertySource(new ExtendsPropertySource(nodeModel) {
            @Override
            protected List<String> getPossibleValues() {
              List<String> moduleNames = new ArrayList<String>(NEDResourcesPlugin.getNEDResources().getModuleNames());
              Collections.sort(moduleNames);
              return moduleNames;
            }
        });
        // interfaces
        mergePropertySource(new DelegatingPropertySource(
                new InterfacesListPropertySource(nodeModel),
                InterfacesListPropertySource.CATEGORY,
                InterfacesListPropertySource.DESCRIPTION));
        // parameter list property
        mergePropertySource(new DelegatingPropertySource(
                new ParameterListPropertySource(nodeModel),
                ParameterListPropertySource.CATEGORY,
                ParameterListPropertySource.DESCRIPTION));
        mergePropertySource(new DelegatingPropertySource(
                new GateListPropertySource(nodeModel),
                GateListPropertySource.CATEGORY,
                GateListPropertySource.DESCRIPTION));
        // create a nested displayPropertySource
        mergePropertySource(new SimpleModuleDisplayPropertySource(nodeModel));
        
    }

}
