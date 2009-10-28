/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.
  
  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.editor.graph.parts;

import java.util.List;

import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.commands.Command;
import org.omnetpp.common.displaymodel.IDisplayString;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.figures.SubmoduleFigure;
import org.omnetpp.figures.anchors.GateAnchor;
import org.omnetpp.ned.editor.graph.commands.ReconnectCommand;
import org.omnetpp.ned.editor.graph.figures.SubmoduleFigureEx;
import org.omnetpp.ned.editor.graph.properties.util.SubmoduleNameValidator;
import org.omnetpp.ned.model.DisplayString;
import org.omnetpp.ned.model.INEDElement;
import org.omnetpp.ned.model.ex.SubmoduleElementEx;


/**
 * Controls the submodule figure according to the model changes.
 *
 * @author rhornig
 */
public class SubmoduleEditPart extends ModuleEditPart {
    protected GateAnchor gateAnchor;

    @Override
    public void activate() {
        super.activate();
        renameValidator = new SubmoduleNameValidator(getSubmoduleModel());
    }

    @Override
    protected void createEditPolicies() {
        super.createEditPolicies();
    }
    /**
     * Returns a newly created Figure of this.
     */
    @Override
    protected IFigure createFigure() {
        SubmoduleFigure fig = new SubmoduleFigureEx();
        // set the pin decoration image for the image (The compound module requests an auto-layout
        // if we add an figure without pin. ie. submodule created in the text editor without 
        // a display string
        fig.setPinVisible(getSubmoduleModel().getDisplayString().getLocation(1.0f) != null);

        gateAnchor = new GateAnchor(fig);
        return fig;
    }

    /**
     * Returns the Figure for this as an SubmoduleFigure.
     */
    public SubmoduleFigure getSubmoduleFigure() {
        return (SubmoduleFigure)getFigure();
    }

    /**
     * Helper function to return the model object with correct type
     */
    public SubmoduleElementEx getSubmoduleModel() {
        return (SubmoduleElementEx)getModel();
    }

	/**
	 * Compute the source connection anchor to be assigned based on the current mouse
	 * location and available gates.
	 * @param p current mouse coordinates
	 * @return The selected connection anchor
	 */
	@Override
    public ConnectionAnchor getConnectionAnchorAt(Point p) {
        return gateAnchor;
	}

	/**
	 * Returns a connection anchor registered for the given gate
	 */
	@Override
    public GateAnchor getConnectionAnchor(String gate) {
        return gateAnchor;
	}

    /**
     * Returns a list of connections for which this is the srcModule.
     */
    @SuppressWarnings("unchecked")
    @Override
    protected List getModelSourceConnections() {
        // get the connections from out controller parent's model
        return getCompoundModulePart().getCompoundModuleModel().getSrcConnectionsFor(getSubmoduleModel().getName());
    }

    /**
     * Returns a list of connections for which this is the destModule.
     */
    @SuppressWarnings("unchecked")
    @Override
    protected List getModelTargetConnections() {
        // get the connections from out controller parent's model
        return getCompoundModulePart().getCompoundModuleModel().getDestConnectionsFor(getSubmoduleModel().getName());
    }

    /**
     * Updates the visual aspect of this.
     */
    @Override
    protected void refreshVisuals() {
        super.refreshVisuals();
        // define the properties that determine the visual appearance
    	SubmoduleElementEx submNode = getSubmoduleModel();

    	// set module name and vector size
    	String nameToDisplay = submNode.getName();
    	// add [size] if it's a module vector
    	if (StringUtils.isNotEmpty(submNode.getVectorSize()))
    		nameToDisplay += "["+submNode.getVectorSize()+"]";
    	SubmoduleFigure submoduleFigure = getSubmoduleFigure();
        submoduleFigure.setName(nameToDisplay);
        submoduleFigure.setAlpha(submNode.isDynamic() ? 64 : 255);

    	// parse a display string, so it's easier to get values from it.
    	// for other visual properties
        DisplayString dps = submNode.getDisplayString();

        // get the scale factor for this submodule (coming from the containing compound module's display string)
        // set it in the figure, so size and range indicator can use it
        float scale = getScale();

        // set the rest of the display properties
        submoduleFigure.setDisplayString(scale, dps);
        
        submoduleFigure.setQueueText(StringUtils.isNotBlank(dps.getAsString(IDisplayString.Prop.QUEUE)) ? "#" : "");

        // show/hide the pin marker
        submoduleFigure.setPinVisible(dps.getLocation(scale) != null);
    }

    @Override
    public float getScale() {
        // get the container compound module's scaling factor
        return getCompoundModulePart().getScale();
    }

    @Override
    public boolean isEditable() {
        // editable only if the parent controllers model is the same as the model's parent
        // i.e. the submodule is defined in this compound module (not inherited)
        return super.isEditable() && getParent().getModel() == ((SubmoduleElementEx)getModel()).getCompoundModule();
    }

    @Override
    protected Command validateCommand(Command command) {
        // connection creation is allowed even if the submodule is non editable (but the containing
        // compound module is editable)
        if (command instanceof ReconnectCommand && getCompoundModulePart().isEditable())
            return command;

        return super.validateCommand(command);
    }

    @Override
    public CompoundModuleEditPart getCompoundModulePart() {
        return (CompoundModuleEditPart)getParent();
    }

    @Override
    protected INEDElement getNEDElementToOpen() {
        return getSubmoduleModel().getEffectiveTypeRef();
    }
}
