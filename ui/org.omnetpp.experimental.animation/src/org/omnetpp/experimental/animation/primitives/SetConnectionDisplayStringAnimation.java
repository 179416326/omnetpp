package org.omnetpp.experimental.animation.primitives;

import org.omnetpp.common.displaymodel.DisplayString;
import org.omnetpp.common.displaymodel.IDisplayString;
import org.omnetpp.common.simulation.model.ConnectionId;
import org.omnetpp.common.simulation.model.IRuntimeModule;
import org.omnetpp.experimental.animation.controller.AnimationPosition;
import org.omnetpp.experimental.animation.replay.ReplayAnimationController;
import org.omnetpp.figures.ConnectionFigure;

public class SetConnectionDisplayStringAnimation extends AbstractInfiniteAnimation {
	protected ConnectionId connectionId;

	protected IDisplayString displayString;
	
	protected IDisplayString oldDisplayString; // FIXME: this is a temproray hack to be able to undo changes

	public SetConnectionDisplayStringAnimation(ReplayAnimationController animationController,
											   AnimationPosition animationPosition,
											   ConnectionId connectionId,
											   IDisplayString displayString) {
		super(animationController, animationPosition);
		this.connectionId = connectionId;
		this.displayString = displayString;
		//System.out.println(displayString);
	}
	
	@Override
	public void redo() {
		IRuntimeModule module = getSourceModule();
		if (module != null && module.getParentModule() == animationEnvironment.getSimulation().getRootModule()) { //FIXME
			ConnectionFigure connectionFigure = (ConnectionFigure)animationEnvironment.getFigure(connectionId);
			
			// FIXME:
			if (connectionFigure != null) {
				oldDisplayString = connectionFigure.getLastDisplayString();
				if (oldDisplayString==null) oldDisplayString = new DisplayString(null, null, "");
				connectionFigure.setDisplayString(displayString);
			}
		}
	}

	@Override
	public void undo() {
		IRuntimeModule module = getSourceModule();
		if (module != null && module.getParentModule() == animationEnvironment.getSimulation().getRootModule()) { //FIXME
			ConnectionFigure connectionFigure = (ConnectionFigure)animationEnvironment.getFigure(connectionId);

			// FIXME:
			if (connectionFigure != null)
				connectionFigure.setDisplayString(oldDisplayString);// FIXME: this is a temproray hack to be able to undo changes
		}
	}

	protected IRuntimeModule getSourceModule() {
		return animationEnvironment.getSimulation().getModuleByID(connectionId.getModuleId());
	}
}
