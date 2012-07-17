package org.omnetpp.simulation.inspectors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.XYLayout;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.omnetpp.figures.misc.FigureUtils;
import org.omnetpp.simulation.controller.ISimulationStateListener;
import org.omnetpp.simulation.controller.SimulationController;
import org.omnetpp.simulation.model.cModule;
import org.omnetpp.simulation.model.cObject;
import org.omnetpp.simulation.model.cQueue;
import org.omnetpp.simulation.model.cSimpleModule;

/**
 *
 * @author Andras
 */
//XXX snap to grid for the move/resize?
//FIXME how to evaluate "$PARNAME" references in display strings???
//NOTE: see ModelCanvas in the old topic/guienv2 branch for scrollable version (using ScrolledComposite)
public class SimulationCanvas extends FigureCanvas implements IInspectorContainer {
    public static final String EDITOR_ID = "org.omnetpp.simulation.inspectors.SimulationCanvas";
    protected List<IInspectorPart> inspectors = new ArrayList<IInspectorPart>();
    protected SimulationController simulationController; //XXX temporary
	protected ISimulationStateListener simulationListener;

	public SimulationCanvas(SimulationController simulationController, Composite parent, int style) {
	    super(parent, style);
	    this.simulationController = simulationController;
	      
        setBackground(new Color(null, 235, 235, 235));
        setContents(new Figure());
        getContents().setLayoutManager(new XYLayout());

        // create context menu
        final MenuManager contextMenuManager = new MenuManager("#popup");
        setMenu(contextMenuManager.createContextMenu(this));
        addMenuDetectListener(new MenuDetectListener() {
            //@Override
            public void menuDetected(MenuDetectEvent e) {
                contextMenuManager.removeAll();
                Point p = toControl(e.x, e.y);
                IInspectorPart inspectorPart = InspectorPart.findInspectorPartAt(SimulationCanvas.this, p.x, p.y);
                if (inspectorPart != null)
                	inspectorPart.populateContextMenu(contextMenuManager, p);
            }
        });

        FigureUtils.addTooltipSupport(this, this.getContents());

		// update inspectors when something happens in the simulation
		simulationController.addSimulationStateListener(simulationListener = new ISimulationStateListener() {
            @Override
            public void simulationStateChanged(SimulationController controller) {
                refreshInspectors();
            }
        });
    }

	@Override
	public Composite getControl() {
	    return this;
	}
	
    @Override
    public void dispose() {
    	simulationController.removeSimulationStateListener(simulationListener);
    	for (IInspectorPart inspectorPart : inspectors.toArray(new IInspectorPart[inspectors.size()]))
    		removeInspectorPart(inspectorPart);
    	super.dispose();
    }

    public void addInspectorPart(IInspectorPart inspectorPart) {
        int lastY = getContents().getPreferredSize().height;
        addInspectorPart(inspectorPart, 0, lastY+5);
    }

    public void addInspectorPart(IInspectorPart inspectorPart, final int x, final int y) {
        final IFigure moduleFigure = inspectorPart.getFigure();
        getContents().add(moduleFigure);
        getContents().setConstraint(moduleFigure, new Rectangle(x, y, -1, -1));

        // register the inspector
        inspectors.add(inspectorPart);
        inspectorPart.setContainer(this);
        inspectorPart.refresh();

        // add move/resize/selection support
        new InspectorMouseListener(inspectorPart); //XXX
    }

    public void removeInspectorPart(IInspectorPart inspectorPart) {
    	System.out.println("removeInspectorPart: " + inspectorPart);
    	Assert.isTrue(inspectors.contains(inspectorPart));
    	inspectors.remove(inspectorPart);
    	getContents().remove(inspectorPart.getFigure());  //XXX maybe do it in the inspector??
    	inspectorPart.dispose();
    }

    protected void refreshInspectors() {
    	for (IInspectorPart inspectorPart : inspectors.toArray(new IInspectorPart[inspectors.size()]))
    		inspectorPart.refresh();
    }

	public void close(IInspectorPart inspectorPart) {
		removeInspectorPart(inspectorPart);
	}

	protected IInspectorPart findInspectorFor(cObject object) {
	    for (IInspectorPart inspector : inspectors)
	        if (inspector.getObject() == object)
	            return inspector;
	    return null;
    }
	
	public void inspect(cObject object) {
	    Assert.isNotNull(object);

	    IInspectorPart inspector = findInspectorFor(object);
	    if (inspector != null) {
	        reveal(inspector);
	    }
	    else {	        
	        inspector = createInspectorFor(object);
	        addInspectorPart(inspector);
	        
	        // Note: the following layout() call doesn't work to reveal the inspector (looks like 
	        // it doesn't cause the scrollbar or getContents().getBounds() to be updated); as a 
	        // workaround, we call reveal() in an asyncExec().
	        //getContents().getLayoutManager().layout(getContents());  
            //reveal(finalInspector);
	        
	        final IInspectorPart finalInspector = inspector;
	        Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    reveal(finalInspector);
                }
            });
	    }
	}

    protected IInspectorPart createInspectorFor(cObject object) {
        //TODO more dynamic inspector type selection
	    //TODO move inspector creation out of SimulationCanvas!!!
        IInspectorPart inspector = null;
        if (object instanceof cModule && !(object instanceof cSimpleModule))
            inspector = new GraphicalModulePart((cModule)object);
//        else if (object instanceof cMessage)
//            inspectorPart = new MessageInspectorPart((cMessage)object);
        else if (object instanceof cQueue)
            inspector = new QueueInspectorPart((cQueue)object);
        else // fallback
            inspector = new InfoTextInspectorPart(object);
        return inspector;
    }
	
    public void reveal(IInspectorPart inspector) {
        Rectangle bounds = inspector.getFigure().getBounds();
        scrollSmoothTo(bounds.x, bounds.y);  // scrolls so that inspector is at the top of the screen (behavior could be improved)
    }

// TODO revive this block!
//    
//	@SuppressWarnings("unchecked")
//	public void select(cObject object, boolean removeOthers) {
//		if (removeOthers) {
//			fireSelectionChange(new StructuredSelection(object));
//		}
//		else {
//			IStructuredSelection selection = (IStructuredSelection)getSite().getSelectionProvider().getSelection();
//			if (!selection.toList().contains(object)) {
//				List list = new ArrayList(selection.toList());
//				list.add(object);
//				fireSelectionChange(new StructuredSelection(list));
//			}
//		}
//	}
//
//	public void toggleSelection(cObject object) {
//		IStructuredSelection selection = (IStructuredSelection)getSite().getSelectionProvider().getSelection();
//		if (selection.toList().contains(object))
//			deselect(object);
//		else
//			select(object, false);
//	}
//
//	@SuppressWarnings("unchecked")
//	public void deselect(cObject object) {
//		IStructuredSelection selection = (IStructuredSelection)getSite().getSelectionProvider().getSelection();
//		if (selection.toList().contains(object)) {
//			List list = new ArrayList(selection.toList());
//			list.remove(object);
//			fireSelectionChange(new StructuredSelection(list));
//		}
//	}
//
//	//@Override
//	public void deselectAll() {
//		fireSelectionChange(new StructuredSelection());
//	}
//
//    protected void fireSelectionChange(IStructuredSelection selection) {
//    	System.out.println("SimulationCanvas: distributing selection " + selection);
//    	getSite().getSelectionProvider().setSelection(selection);
//    	for (IInspectorPart inspector : inspectors)
//    		inspector.selectionChanged(selection);
//    }

    public void deselect(cObject object) {
        //TODO
    }

    @Override
    public void select(cObject object, boolean deselectOthers) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void toggleSelection(cObject object) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void deselectAll() {
        // TODO Auto-generated method stub
        
    }

}