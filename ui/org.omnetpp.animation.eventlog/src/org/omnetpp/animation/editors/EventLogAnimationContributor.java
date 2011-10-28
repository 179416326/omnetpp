/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.animation.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.omnetpp.animation.controller.AnimationController;
import org.omnetpp.animation.providers.EventLogAnimationPrimitiveProvider;
import org.omnetpp.animation.widgets.EventLogAnimationConfigurationDialog;

public class EventLogAnimationContributor extends AnimationContributor {
    protected AnimationAction configureAction;

	public EventLogAnimationContributor() {
        this.configureAction = createConfigureAction();
	}

	@Override
    public void menuAboutToShow(IMenuManager menuManager) {
	    super.menuAboutToShow(menuManager);
	    menuManager.add(configureAction);
	}

    private AnimationAction createConfigureAction() {
        return new AnimationAction("Configure...", Action.AS_PUSH_BUTTON) {
            @Override
            protected void doRun() {
                AnimationController animationController = animationCanvas.getAnimationController();
                EventLogAnimationPrimitiveProvider eventLogAnimationPrimitiveProvider = (EventLogAnimationPrimitiveProvider)animationController.getAnimationPrimitiveProvider();
                Dialog configurationDialog = new EventLogAnimationConfigurationDialog(Display.getCurrent().getActiveShell(), eventLogAnimationPrimitiveProvider.getAnimationParameters());
                long eventNumber = animationController.getCurrentEventNumber();
                if (configurationDialog.open() == Window.OK) {
                    // TODO: KLUDGE: the model has to be cleared and rebuilt,
                    // but this clearInternalState sux here, because
                    // gotoEventNumber has a stopAnimationInternal in it and it
                    // tries to notify the listeners without a valid internal
                    // state
                    animationController.clearInternalState();
                    animationController.gotoEventNumber(eventNumber);
                    animationPositionContribution.configureSlider();
                    EventLogAnimationContributor.this.update();
                }
            }
        };
    }
}
