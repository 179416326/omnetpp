package org.omnetpp.ned.editor.graph.properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource2;
import org.omnetpp.common.displaymodel.DisplayString;
import org.omnetpp.common.properties.CheckboxPropertyDescriptor;
import org.omnetpp.ned.editor.graph.properties.util.DelegatingPropertySource;
import org.omnetpp.ned.editor.graph.properties.util.DisplayPropertySource;
import org.omnetpp.ned.editor.graph.properties.util.ExtendsPropertySource;
import org.omnetpp.ned.editor.graph.properties.util.GateListPropertySource;
import org.omnetpp.ned.editor.graph.properties.util.InterfacesListPropertySource;
import org.omnetpp.ned.editor.graph.properties.util.MergedPropertySource;
import org.omnetpp.ned.editor.graph.properties.util.NamePropertySource;
import org.omnetpp.ned.editor.graph.properties.util.ParameterListPropertySource;
import org.omnetpp.ned.editor.graph.properties.util.SubmoduleListPropertySource;
import org.omnetpp.ned.model.ex.CompoundModuleNodeEx;
import org.omnetpp.resources.NEDResourcesPlugin;

public class CompoundModulePropertySource extends MergedPropertySource {
    // compound module display properties
    protected static class CompoundModuleDisplayPropertySource extends DisplayPropertySource {
        protected CompoundModuleNodeEx model;

        public CompoundModuleDisplayPropertySource(CompoundModuleNodeEx model) {
            super(model);
            this.model = model;
            setDisplayString(model.getDisplayString());
            // submodule inherited properties
            supportedProperties.addAll(EnumSet.range(DisplayString.Prop.WIDTH, 
                                                	 DisplayString.Prop.OVIMAGECOLORPCT));
            // direct compound module properties
            supportedProperties.addAll(EnumSet.range(DisplayString.Prop.MODULE_X, 
               	 									 DisplayString.Prop.MODULE_UNIT));
        }

    }
    
    // compound module specific properties
    protected static class BasePropertySource implements IPropertySource2 {
        public static final String BASE_CATEGORY = "Base";
        public enum Prop { Network }
        protected IPropertyDescriptor[] descriptors;
        protected CompoundModuleNodeEx model;
        CheckboxPropertyDescriptor networkProp;

        public BasePropertySource(CompoundModuleNodeEx connectionNodeModel) {
            model = connectionNodeModel;
            
            // set up property descriptors
            networkProp = new CheckboxPropertyDescriptor(Prop.Network, "network");
            networkProp.setCategory(BASE_CATEGORY);
            networkProp.setDescription("Is this compound module used as a network instance?");

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

    // constructor
    public CompoundModulePropertySource(CompoundModuleNodeEx nodeModel) {
        super(nodeModel);
        // create a nested displayPropertySource
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
        // parameters
        mergePropertySource(new DelegatingPropertySource(
                new ParameterListPropertySource(nodeModel),
                ParameterListPropertySource.CATEGORY,
                ParameterListPropertySource.DESCRIPTION));
        // gates
        mergePropertySource(new DelegatingPropertySource(
                new GateListPropertySource(nodeModel),
                GateListPropertySource.CATEGORY,
                GateListPropertySource.DESCRIPTION));
        // submodules
        mergePropertySource(new DelegatingPropertySource(
                new SubmoduleListPropertySource(nodeModel),
                SubmoduleListPropertySource.CATEGORY,
                SubmoduleListPropertySource.DESCRIPTION));
        // display
        mergePropertySource(new CompoundModuleDisplayPropertySource(nodeModel));
    }

}
