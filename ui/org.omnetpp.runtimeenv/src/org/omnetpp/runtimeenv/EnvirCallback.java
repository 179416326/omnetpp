/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
 *--------------------------------------------------------------*/

package org.omnetpp.runtimeenv;

import org.omnetpp.runtime.nativelibs.IEnvirCallback;
import org.omnetpp.runtime.nativelibs.simkernel.Simkernel;
import org.omnetpp.runtime.nativelibs.simkernel.cComponent;
import org.omnetpp.runtime.nativelibs.simkernel.cGate;
import org.omnetpp.runtime.nativelibs.simkernel.cMessage;
import org.omnetpp.runtime.nativelibs.simkernel.cModule;
import org.omnetpp.runtime.nativelibs.simkernel.cObject;
import org.omnetpp.runtime.nativelibs.simkernel.cSimulation;
import org.omnetpp.runtimeenv.animation.AnimLogEntry;
import org.omnetpp.runtimeenv.animation.AnimationLog;
import org.omnetpp.runtimeenv.animation.SendMessageEntry;

public class EnvirCallback implements IEnvirCallback {
	public boolean debug = false;

	public void simulationEvent(cMessage msg) {
		if (debug)
			System.out.println("simulationEvent called");
	}

	public void messageScheduled(cMessage msg) {
		if (debug)
			System.out.println("messageScheduled called");
	}

	public void messageCancelled(cMessage msg) {
		if (debug)
			System.out.println("messageCancelled called");
	}

	public void beginSend(cMessage msg) {
		if (debug)
			System.out.println("beginSend called");
	}

	public void messageSendDirect(cMessage msg, cGate toGate, double propagationDelay, double transmissionDelay) {
		if (debug)
			System.out.println("messageSendDirect called");
	}

	public void messageSendHop(cMessage msg, cGate srcGate) {
		if (debug)
			System.out.println("messageSendHop called");
		SendMessageEntry e = new SendMessageEntry(
				srcGate.getOwnerModule().getId(), srcGate.getId(),
				srcGate.getNextGate().getOwnerModule().getId(), srcGate.getNextGate().getId(),
				Simkernel.simTime(), 0.0, 0.0);
		e.setStartEventNumber(cSimulation.getActiveSimulation().getEventNumber());
		AnimationLog.getInstance().addEntry(e);
	}

	public void messageSendHop(cMessage msg, cGate srcGate, double propagationDelay, double transmissionDelay) {
		if (debug)
			System.out.println("messageSendHop called");
		SendMessageEntry e = new SendMessageEntry(
				srcGate.getOwnerModule().getId(), srcGate.getId(),
				srcGate.getNextGate().getOwnerModule().getId(), srcGate.getNextGate().getId(),
				Simkernel.simTime(), propagationDelay, transmissionDelay);
		e.setStartEventNumber(cSimulation.getActiveSimulation().getEventNumber());
		AnimationLog.getInstance().addEntry(e);
	}

	public void endSend(cMessage msg) {
		if (debug)
			System.out.println("endSend called");
	}

	public void messageDeleted(cMessage msg) {
		if (debug)
			System.out.println("messageDeleted called");
	}

	public void moduleReparented(cModule module, cModule oldparent) {
		if (debug)
			System.out.println("moduleReparented called");
	}

	public void componentMethodBegin(cComponent from, cComponent to, String method) {
		if (debug)
			System.out.println("componentMethodBegin called");
	}

	public void componentMethodEnd() {
		if (debug)
			System.out.println("componentMethodEnd called");
	}

	public void moduleCreated(cModule newmodule) {
		if (debug)
			System.out.println("moduleCreated called");
	}

	public void moduleDeleted(cModule module) {
		if (debug)
			System.out.println("moduleDeleted called");
	}

	public void gateCreated(cGate srcgate) {
		if (debug)
			System.out.println("gateCreated called");
	}

	public void gateDeleted(cGate srcgate) {
		if (debug)
			System.out.println("gateDeleted called");
	}

	public void connectionCreated(cGate srcgate) {
		if (debug)
			System.out.println("connectionCreated called");
	}

	public void connectionDeleted(cGate srcgate) {
		if (debug)
			System.out.println("connectionDeleted called");
	}

	public void displayStringChanged(cComponent component) {
		if (debug)
			System.out.println("displayStringChanged called");
	}

	public void undisposedObject(cObject obj) {
		if (debug)
			System.out.println("undisposedObject called");
	}

	public void bubble(cModule mod, String text) {
		if (debug)
			System.out.println("bubble called");
	}

	public String gets(String prompt, String defaultreply) {
		if (debug)
			System.out.println("gets called");
		return "FIXME";
	}

	public boolean idle() {
		if (debug)
			System.out.println("idle called");
		return false;
	}
}
