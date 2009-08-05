package org.omnetpp.ned.editor.graph.properties.util;
 
import java.util.List;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource2;
import org.omnetpp.common.properties.EditableComboBoxPropertyDescriptor;
import org.omnetpp.ned2.model.interfaces.IDerived;

/**
 * @author rhornig
 * A property source for displaying Extends info (using an editable combobox). The content of the
 * dropdown combo should be provided by overriding the getPossibleValues mathod.
 */
public abstract class ExtendsPropertySource implements IPropertySource2 {
        public enum Prop { Extends }
        protected IPropertyDescriptor[] descriptors;
        protected IDerived model;
        EditableComboBoxPropertyDescriptor extendsProp;

        public ExtendsPropertySource(IDerived nodeModel) {
            model = nodeModel;
            
            // set up property descriptors
            extendsProp = new EditableComboBoxPropertyDescriptor(Prop.Extends, "extends");
            extendsProp.setCategory("Base");
            extendsProp.setDescription("Which component is extended by this component");
            
            descriptors = new IPropertyDescriptor[] { extendsProp };
        }

        public Object getEditableValue() {
            return model.getFirstExtends();
        }

        public IPropertyDescriptor[] getPropertyDescriptors() {
            //fill the connection combobox with types
            extendsProp.setItems(getPossibleValues());
            return descriptors;
        }

        /**
         * @return The list of possible values that will be used to fill a dropdown box.
         */
        protected abstract List<String> getPossibleValues(); 
        
        public Object getPropertyValue(Object propName) {
            if (Prop.Extends.equals(propName))  
                return model.getFirstExtends(); 
            
            return null;
        }

        public void setPropertyValue(Object propName, Object value) {
            if (Prop.Extends.equals(propName)) 
                model.setFirstExtends((String)value);
        }

        public boolean isPropertySet(Object propName) {
            if (Prop.Extends.equals(propName)) 
                return !"".equals(model.getFirstExtends()) && (model.getFirstExtends() != null);

            return false;
        }

        public void resetPropertyValue(Object propName) {
            if (Prop.Extends.equals(propName)) 
                model.setFirstExtends(null);
        }

        public boolean isPropertyResettable(Object propName) {
            return true;
        }
}
