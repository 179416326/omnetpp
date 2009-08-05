package org.omnetpp.ned.editor.graph.misc;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;

public abstract class DesktopLayoutEditPolicy extends XYLayoutEditPolicy {

    /** 
     * Returns a MODEL object constraint equivalent to the DRAW2D constraint. We transform the
     * draw2d constraint here back to model constraint (ie. the location is the refpoint of the
     * figure in the model, but the top left corner in draw2d) The model to draw2d trasform is done
     * in  DesktopLayout
     *  @param rect Draw2d constraint
     *  @return The equivalent model constraint (location pointing to the figure's refpoint)
     * @see org.eclipse.gef.editpolicies.ConstrainedLayoutEditPolicy#getConstraintFor(org.eclipse.draw2d.geometry.Rectangle)
     */
    public Object getConstraintFor(Rectangle rect) {
        return new Rectangle(rect.getCenter(), rect.getSize());
    }

}
