package org.omnetpp.runtimeenv.editors;

import org.eclipse.jface.viewers.StructuredSelection;
import org.omnetpp.experimental.simkernel.swig.cObject;
import org.omnetpp.runtimeenv.Activator;
import org.omnetpp.runtimeenv.ISimulationListener;

/**
 * Default implementation for IInspectorPart, base class for inspector classes
 */
public abstract class InspectorPart implements IInspectorPart {
	protected cObject object;
	protected IInspectorFigure figure;
	protected ISimulationListener simulationListener;
	protected boolean isSelected;

	public InspectorPart() {
		super();

		// update the inspector when something happens in the simulation
		Activator.getSimulationManager().addChangeListener(simulationListener = new ISimulationListener() {
			@Override
			public void changed() {
				update();
			}
		});
	}

	public void dispose() {
	    Activator.getSimulationManager().removeChangeListener(simulationListener);
	}

	@Override
	public cObject getObject() {
		return object;
	}

	@Override
	public IInspectorFigure getFigure() {
	    return figure;
	}

	@Override
	public boolean isSelected() {
	    return isSelected;
	}

	@Override
	public void setSelected(boolean isSelected) {
	    // update selection (this also triggers update of figure and isSelected flag)
	    if (isSelected)
	        setSelection(new StructuredSelection(this)); //XXX rather: addToSelection()!  
	    else
	        setSelection(new StructuredSelection()); //XXX rather: removeFromSelection()!
	}

	protected abstract void update();

    
    @Override
    public String toString() {
        return getClass().getSimpleName() + ":(" + object.getClassName() + ")" + object.getFullPath();
    }

}