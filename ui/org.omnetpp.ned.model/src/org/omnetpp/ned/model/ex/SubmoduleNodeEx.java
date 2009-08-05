package org.omnetpp.ned.model.ex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.omnetpp.common.displaymodel.DisplayString;
import org.omnetpp.ned.model.INEDElement;
import org.omnetpp.ned.model.interfaces.IHasGates;
import org.omnetpp.ned.model.interfaces.IHasIndex;
import org.omnetpp.ned.model.interfaces.IHasParameters;
import org.omnetpp.ned.model.interfaces.IHasType;
import org.omnetpp.ned.model.interfaces.IModuleTypeNode;
import org.omnetpp.ned.model.interfaces.INEDTypeInfo;
import org.omnetpp.ned.model.interfaces.INamedGraphNode;
import org.omnetpp.ned.model.pojo.GatesNode;
import org.omnetpp.ned.model.pojo.ParametersNode;
import org.omnetpp.ned.model.pojo.SubmoduleNode;

public final class SubmoduleNodeEx extends SubmoduleNode
                            implements INamedGraphNode, IHasIndex, IHasType,
                                       IHasParameters, IHasGates {
    public static final String DEFAULT_TYPE = "Unknown";
    public static final String DEFAULT_NAME = "unnamed";

    protected DisplayString displayString = null;

    protected SubmoduleNodeEx() {
        init();
	}

    protected SubmoduleNodeEx(INEDElement parent) {
		super(parent);
        init();
	}

    private void init() {
        setName(DEFAULT_NAME);
        setType(DEFAULT_TYPE);
    }

    public String getNameWithIndex() {
        String result = getName();
        if (getVectorSize() != null && !"".equals(getVectorSize()))
            result += "["+getVectorSize()+"]";
        return result;
    }

    @Override
    public void setName(String val) {
        if (getCompoundModule() != null) {
        // if a submodule name has changed we must change all the connections in the same compound module
        // that is attached to this module (so the model will remain consistent)
            for (ConnectionNodeEx conn : getCompoundModule().getSrcConnectionsFor(getName()))
                conn.setSrcModule(val);
            for (ConnectionNodeEx conn : getCompoundModule().getDestConnectionsFor(getName()))
                conn.setDestModule(val);
        }
        // now we can change the name
        super.setName(val);
    }


	public DisplayString getDisplayString() {
		if (displayString == null) {
			displayString = new DisplayString(this, NEDElementUtilEx.getDisplayString(this));
		}
		return displayString;
	}

    public DisplayString getEffectiveDisplayString() {
        return NEDElementUtilEx.getEffectiveDisplayString(this);
    }

    /**
     * Returns the effective display string for this submodule, assuming
     * that the submodule's actual type is the compound or simple module type
     * passed in the <code>submoduleType</code> parameter. This is useful
     * when the submodule is a "like" submodule, whose the actual submodule
     * type (not the <code>likeType</code>) is known. The latter usually
     * comes from an ini file or some other source outside the INEDElement tree.
     * Used within the inifile editor.
     *
     * @param submoduleType  a CompoundModuleNodeEx or a SimpleModuleNodeEx
     */
    public DisplayString getEffectiveDisplayString(IModuleTypeNode submoduleType) {
        return NEDElementUtilEx.getEffectiveDisplayString(this, submoduleType);
    }

	/**
	 * @return The compound module containing the definition of this connection
	 */
	public CompoundModuleNodeEx getCompoundModule() {
        INEDElement parent = getParent();
        while (parent != null && !(parent instanceof CompoundModuleNodeEx))
            parent = parent.getParent();
        return (CompoundModuleNodeEx)parent;
	}

	// connection related methods

	/**
	 * @return All source connections that connect to this node and defined
     * in the parent compound module. connections defined in derived modules
     * are NOT included here
	 */
	public List<ConnectionNodeEx> getSrcConnections() {
		return getCompoundModule().getSrcConnectionsFor(getName());
	}

    /**
     * @return All connections that connect to this node and defined in the
     * parent compound module. connections defined in derived modules are
     * NOT included here
     */
	public List<ConnectionNodeEx> getDestConnections() {
		return getCompoundModule().getDestConnectionsFor(getName());
	}

	@Override
    public String debugString() {
        return "submodule: "+getName();
    }

    // type support
    public INEDTypeInfo getTypeNEDTypeInfo() {
        String typeName = getEffectiveType();
        INEDTypeInfo typeInfo = getContainerNEDTypeInfo();
        if ( typeName == null || "".equals(typeName) || typeInfo == null)
            return null;

        return typeInfo.getResolver().getComponent(typeName);
    }

    public INEDElement getEffectiveTypeRef() {
        INEDTypeInfo it = getTypeNEDTypeInfo();
        return it == null ? null : it.getNEDElement();
    }

    public String getEffectiveType() {
        String type = getLikeType();
        // if it's not specified use the likeType instead
        if (type == null || "".equals(type))
            type = getType();

        return type;
    }

    /**
     * @return All parameters assigned in this submodule's body
     */
    public List<ParamNodeEx> getOwnParams() {
        List<ParamNodeEx> result = new ArrayList<ParamNodeEx>();
        ParametersNode parametersNode = getFirstParametersChild();
        if (parametersNode == null)
            return result;
        for (INEDElement currChild : parametersNode)
            if (currChild instanceof ParamNodeEx)
                result.add((ParamNodeEx)currChild);

        return result;
    }

    // parameter query support
    public Map<String, INEDElement> getParamValues() {
        INEDTypeInfo info = getTypeNEDTypeInfo();
        Map<String, INEDElement> result =
            (info == null) ? new HashMap<String, INEDElement>() : new HashMap<String, INEDElement>(info.getParamValues());

        // add our own assigned parameters
        for (ParamNodeEx ownParam : getOwnParams())
            result.put(ownParam.getName(), ownParam);

        return result;
    }

    public Map<String, INEDElement> getParams() {
        INEDTypeInfo info = getTypeNEDTypeInfo();
        if (info == null)
            return new HashMap<String, INEDElement>();
        return info.getParams();
    }

    // gate support
    /**
     * @return All gates assigned in this submodule's body
     */
    public List<GateNodeEx> getOwnGates() {
        List<GateNodeEx> result = new ArrayList<GateNodeEx>();
        GatesNode gatesNode = getFirstGatesChild();
        if (gatesNode == null)
            return result;
        for (INEDElement currChild : gatesNode)
            if (currChild instanceof GateNodeEx)
                result.add((GateNodeEx)currChild);

        return result;
    }

    public Map<String, INEDElement> getGateSizes() {
        INEDTypeInfo info = getTypeNEDTypeInfo();
        Map<String, INEDElement> result =
            (info == null) ? new HashMap<String, INEDElement>() : new HashMap<String, INEDElement>(info.getGateSizes());

        // add our own assigned parameters
        for (GateNodeEx ownGate : getOwnGates())
            result.put(ownGate.getName(), ownGate);

        return result;
    }

    public Map<String, INEDElement> getGates() {
        INEDTypeInfo info = getTypeNEDTypeInfo();
        if (info == null)
            return new HashMap<String, INEDElement>();
        return info.getGates();
    }

}
