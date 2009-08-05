package org.omnetpp.common.simulation.model;

public class ConnectionId {
	private int moduleId;

	private int gateId;
	
	public ConnectionId(int moduleId, int gateId) {
		this.moduleId = moduleId;
		this.gateId = gateId;
	}

	public int getModuleId() {
		return moduleId;
	}

	public void setModuleId(int moduleId) {
		this.moduleId = moduleId;
	}

	public int getGateId() {
		return gateId;
	}

	public void setGateId(int gateId) {
		this.gateId = gateId;
	}
	
	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + moduleId;
		result = PRIME * result + gateId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ConnectionId other = (ConnectionId) obj;
		if (moduleId != other.moduleId)
			return false;
		if (gateId != other.gateId)
			return false;
		return true;
	}
}
