package org.omnetpp.ned.editor.graph.edit;

import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.requests.DropRequest;
import org.omnetpp.figures.misc.GateAnchor;
import org.omnetpp.ned.editor.graph.edit.policies.NedNodeEditPolicy;
import org.omnetpp.ned.model.ex.ConnectionNodeEx;

/**
 * Base abstract controller for NedModel and NedFigures. Provides support for
 * connection handling and common display attributes.
 */
abstract public class ModuleEditPart extends BaseEditPart implements NodeEditPart {

    @Override
	protected void createEditPolicies() {
		super.createEditPolicies();
		installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE, new NedNodeEditPolicy());
	}


	/**
	 * Compute the source connection anchor to be assigned based on the current mouse
	 * location and available gates.
	 * @param p current mouse coordinates
	 * @return The selected connection anchor
	 */
	public abstract ConnectionAnchor getConnectionAnchorAt(Point p);

	/**
	 * Returns a connection anchor registered for the given gate
	 * @param gate
	 * @return
	 */
	public abstract GateAnchor getConnectionAnchor(String gate);

	public ConnectionAnchor getSourceConnectionAnchor(ConnectionEditPart connEditPart) {
		ConnectionNodeEx conn = (ConnectionNodeEx) connEditPart.getModel();
		return getConnectionAnchor(conn.getSrcGateWithIndex());
	}

	public ConnectionAnchor getSourceConnectionAnchor(Request request) {
		Point pt = new Point(((DropRequest) request).getLocation());
		return getConnectionAnchorAt(pt);
	}

	public ConnectionAnchor getTargetConnectionAnchor(ConnectionEditPart connEditPart) {
		ConnectionNodeEx conn = (ConnectionNodeEx) connEditPart.getModel();
		return getConnectionAnchor(conn.getDestGateWithIndex());
	}

	public ConnectionAnchor getTargetConnectionAnchor(Request request) {
		Point pt = new Point(((DropRequest) request).getLocation());
		return getConnectionAnchorAt(pt);
	}

    /**
     * @return The scale factor of the module
     */
    public abstract float getScale();

    /**
     * @return The compound module itself or the compound module controller
     *  which contains this controller
     */
    public abstract CompoundModuleEditPart getCompoundModulePart();

    /* (non-Javadoc)
     * @see org.eclipse.gef.editparts.AbstractGraphicalEditPart#createOrFindConnection(java.lang.Object)
     * We must override this method, because the original implementation has a global (per viewer) MAP
     * to store MODEL - PART associations. This is a problem if we want to display a compound module which
     * inherits some submodules and connections from an other one (that is also displayed in this viewer)
     * In those case the original implementation would not create a new PART for the connection in the
     * derived module but would return the controller from the base module (which is of course wrong)
     * and leads to very strange bugs.
     */
    @Override
    protected ConnectionEditPart createOrFindConnection(Object model) {
        // get the model - controller cache from the containing compound module
        ConnectionEditPart conx = getCompoundModulePart().getModelToConnectionPartsRegistry().get(model);
        if (conx != null)
            return conx;
        return createConnection(model);
    }

}
