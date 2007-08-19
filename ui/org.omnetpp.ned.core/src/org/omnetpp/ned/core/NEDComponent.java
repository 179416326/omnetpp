package org.omnetpp.ned.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.ned.model.INEDElement;
import org.omnetpp.ned.model.NEDSourceRegion;
import org.omnetpp.ned.model.ex.CompoundModuleNodeEx;
import org.omnetpp.ned.model.ex.ConnectionNodeEx;
import org.omnetpp.ned.model.ex.SubmoduleNodeEx;
import org.omnetpp.ned.model.interfaces.IHasName;
import org.omnetpp.ned.model.interfaces.IInterfaceTypeNode;
import org.omnetpp.ned.model.interfaces.INEDTypeInfo;
import org.omnetpp.ned.model.interfaces.INEDTypeResolver;
import org.omnetpp.ned.model.interfaces.INedTypeNode;
import org.omnetpp.ned.model.notification.NEDModelEvent;
import org.omnetpp.ned.model.pojo.ExtendsNode;
import org.omnetpp.ned.model.pojo.GateNode;
import org.omnetpp.ned.model.pojo.NEDElementTags;
import org.omnetpp.ned.model.pojo.ParamNode;
import org.omnetpp.ned.model.pojo.PropertyNode;
import org.omnetpp.ned.model.pojo.SubmoduleNode;

/**
 * Default implementation of INEDComponent.
 *
 * @author rhornig, andras
 */
public class NEDComponent implements INEDTypeInfo, NEDElementTags {

	protected INEDTypeResolver resolver;

	protected INedTypeNode componentNode;
	protected IFile file;
	
	private int debugId = lastDebugId++;
	private static int lastDebugId = 0;
	private static int debugRefreshInheritedCount = 0;
	private static int debugRefreshOwnCount = 0;

	// own stuff
    protected boolean needsOwnUpdate;
	protected Set<String> ownInterfaces = new HashSet<String>();
	protected Map<String, PropertyNode> ownProperties = new LinkedHashMap<String, PropertyNode>();
    protected Map<String, ParamNode> ownParams = new LinkedHashMap<String, ParamNode>();
    protected Map<String, ParamNode> ownParamValues = new LinkedHashMap<String, ParamNode>();
	protected Map<String, GateNode> ownGates = new LinkedHashMap<String, GateNode>();
    protected Map<String, GateNode> ownGateSizes = new LinkedHashMap<String, GateNode>();
	protected Map<String, INedTypeNode> ownInnerTypes = new LinkedHashMap<String, INedTypeNode>();
	protected Map<String, SubmoduleNode> ownSubmodules = new LinkedHashMap<String, SubmoduleNode>();
    protected HashSet<String> ownUsedTypes = new HashSet<String>();

	// sum of all "own" stuff
	protected Map<String, INEDElement> ownMembers = new LinkedHashMap<String, INEDElement>();

	// own plus inherited
	protected boolean needsUpdate;
	protected List<INEDTypeInfo> extendsChain = null;
	protected Set<String> allInterfaces = new HashSet<String>();
	protected Map<String, PropertyNode> allProperties = new LinkedHashMap<String, PropertyNode>();
    protected Map<String, ParamNode> allParams = new LinkedHashMap<String, ParamNode>();
    protected Map<String, ParamNode> allParamValues = new LinkedHashMap<String, ParamNode>();
    protected Map<String, GateNode> allGates = new LinkedHashMap<String, GateNode>();
    protected Map<String, GateNode> allGateSizes = new LinkedHashMap<String, GateNode>();
	protected Map<String, INedTypeNode> allInnerTypes = new LinkedHashMap<String, INedTypeNode>();
	protected Map<String, SubmoduleNode> allSubmodules = new LinkedHashMap<String, SubmoduleNode>();

	// sum of all own+inherited stuff
	protected Map<String, INEDElement> allMembers = new LinkedHashMap<String, INEDElement>();

//    // all types which extends this component
//    protected List<INEDTypeInfo> allDerivedTypes = new ArrayList<INEDTypeInfo>();
    
    // all types that contain instances (submodule, connection) of this type
    protected List<INEDTypeInfo> allUsingTypes = new ArrayList<INEDTypeInfo>();

	// for local use
    interface IPredicate {
		public boolean matches(IHasName node);
	}

	/**
	 * Constructor
	 * @param node INEDElement tree to be wrapped
	 * @param nedfile file containing the definition
	 * @param res will be used to resolve inheritance (collect gates, params etc from base classes)
	 */
	public NEDComponent(INedTypeNode node, IFile nedfile, INEDTypeResolver res) {
		resolver = res;
		file = nedfile;
		componentNode = node;

		// register the created component in the INEDElement, so we will have access to it
        // directly from the model. We also want to listen on it, and invalidate ownMembers etc
		// if anything changes.
		INEDTypeInfo oldTypeInfo = node.getNEDTypeInfo();
		if (oldTypeInfo != null)
			node.removeNEDChangeListener(oldTypeInfo);
		node.addNEDChangeListener(this);
        node.setNEDTypeInfo(this);

        // the inherited and own members will be collected on demand
        needsOwnUpdate = true;
		needsUpdate = true;
	}

    public INEDTypeResolver getResolver() {
        return resolver;
    }

    /**
	 * Collect elements (gates, params, etc) with the given tag code (NED_PARAM, etc) 
	 * from the given section into the map.
	 */
	protected void collect(Map<String,? extends INEDElement> map, int sectionTagCode, final int tagCode) {
		collect(map, sectionTagCode, new IPredicate() {
			public boolean matches(IHasName node) {
				return node.getTagCode() == tagCode;
			}
		});
	}

    /**
	 * Collect elements (gates, params, etc) that match the predicate from the given section 
	 * (NED_PARAMETERS, NED_GATES, etc) into the map.
	 */
	@SuppressWarnings("unchecked")
	protected void collect(Map<String,? extends INEDElement> map, int sectionTagCode, IPredicate predicate) { 
		INEDElement section = componentNode.getFirstChildWithTag(sectionTagCode);
		if (section != null)
			for (INEDElement node : section) 
				if (node instanceof IHasName && predicate.matches((IHasName)node))
					((Map)map).put(((IHasName)node).getName(), node);
	}
	
	protected void collectInheritance(Set<String> set, int tagCode) {
		Assert.isTrue(tagCode==NED_INTERFACE_NAME || tagCode==NED_EXTENDS);
		for (INEDElement child : getNEDElement())
			if (child.getTagCode()==tagCode)
				set.add(child.getAttribute(ExtendsNode.ATT_NAME));
	}

	/**
     * Collects all type names that are used in this module (submodule and connection types)
     * @param result storage for the used types
     */
    protected void collectTypesInCompoundModule(Set<String> result) {
        // this is only meaningful for CompoundModules so skip the others
        if (componentNode instanceof CompoundModuleNodeEx) {
        	// look for submodule types
        	INEDElement submodules = componentNode.getFirstChildWithTag(NED_SUBMODULES);
        	if (submodules != null)
        		for (INEDElement node : submodules)
        			if (node instanceof SubmoduleNodeEx)
        				result.add(((SubmoduleNodeEx)node).getEffectiveType());

        	// look for connection types
        	INEDElement connections = componentNode.getFirstChildWithTag(NED_CONNECTIONS);
        	if (connections != null)
        		for (INEDElement node : connections)
        			if (node instanceof ConnectionNodeEx)
        				result.add(((ConnectionNodeEx)node).getEffectiveType());
        }
    }

	/**
	 * Produce a list that starts with this type, and ends with the root.
	 */
	protected List<INEDTypeInfo> computeExtendsChain() {
	    ArrayList<INEDTypeInfo> tmp = new ArrayList<INEDTypeInfo>();
    	tmp.add(this);
	    INEDTypeInfo currentComponent = this;
	    while (true) {
	    	//FIXME INedTypeNode already contains a getFirstExtendsType() method!!! use that!
	    	INedTypeNode currentComponentNode = currentComponent.getNEDElement();
	    	INEDElement extendsNode = currentComponentNode.getFirstChildWithTag(NED_EXTENDS);
	    	if (extendsNode==null)
	    		break;
	    	String extendsName = ((ExtendsNode)extendsNode).getName();
	    	if (extendsName==null)
	    		break;
	    	currentComponent = resolver.getComponent(extendsName);
	    	if (currentComponent==null)
	    		break;
	    	tmp.add(currentComponent);
	    }

	    return tmp;
	}

    /**
     * Refreshes tables of own (local) members
     */
    protected void refreshOwnMembersIfNeeded() {
    	if (!needsOwnUpdate)
    		return;
    	
        ++debugRefreshOwnCount;
        // System.out.println("NEDComponent for "+getName()+" ownRefresh: " + refreshOwnCount);

        ownInterfaces.clear();
        ownProperties.clear();
        ownParams.clear();
        ownParamValues.clear();
        ownGates.clear();
        ownGateSizes.clear();
        ownSubmodules.clear();
        ownInnerTypes.clear();
        ownMembers.clear();
        ownUsedTypes.clear();

        // collect base types: interfaces extend other interfaces, modules implement interfaces
        collectInheritance(ownInterfaces, getNEDElement() instanceof IInterfaceTypeNode ? NED_EXTENDS : NED_INTERFACE_NAME);
       
        // collect members from component declaration
        collect(ownProperties, NED_PARAMETERS, NED_PROPERTY);
        collect(ownParams, NED_PARAMETERS, NED_PARAM);
        collect(ownGates, NED_GATES, NED_GATE);
        collect(ownSubmodules, NED_SUBMODULES, NED_SUBMODULE);

        collect(ownParamValues, NED_PARAMETERS, new IPredicate() {
			public boolean matches(IHasName node) {
				return node.getTagCode()==NED_PARAM && StringUtils.isNotEmpty(((ParamNode)node).getValue());
			}
        });
        collect(ownGateSizes, NED_GATES, new IPredicate() {
			public boolean matches(IHasName node) {
				return node.getTagCode()==NED_GATE && StringUtils.isNotEmpty(((GateNode)node).getVectorSize());
			}
        });
        collect(ownInnerTypes, NED_TYPES, new IPredicate() {
			public boolean matches(IHasName node) {
				return node instanceof INedTypeNode;
			}
        });

        // collect them in one common hash table as well (we assume there's no name clash --
        // that should be checked beforehand by validation!)
        ownMembers.putAll(ownProperties);
        ownMembers.putAll(ownParams);
        ownMembers.putAll(ownGates);
        ownMembers.putAll(ownSubmodules);
        ownMembers.putAll(ownInnerTypes);

        // collect the types that were used in this module (meaningful only for compound modules)
        collectTypesInCompoundModule(ownUsedTypes);

        needsOwnUpdate = false;
    }

	/**
	 * Collect all inherited parameters, gates, properties, submodules, etc.
	 */
	protected void refreshInheritedMembersIfNeeded() {
		if (!needsUpdate)
			return;
		
        ++debugRefreshInheritedCount;
        // System.out.println("NEDComponent for "+getName()+" inheritedRefresh: " + refreshInheritedCount);

        // first wee need our own members updated
        if (needsOwnUpdate)
            refreshOwnMembersIfNeeded();

        // determine extends chain
        extendsChain = computeExtendsChain();

        allInterfaces.clear();
		allProperties.clear();
		allParams.clear();
        allParamValues.clear();
		allGates.clear();
        allGateSizes.clear();
		allInnerTypes.clear();
		allSubmodules.clear();
		allMembers.clear();

		
		// collect interfaces: what our base class implements (directly 
		// or indirectly), plus our interfaces and everything they extend
		// (directly or indirectly)
		if (!(getNEDElement() instanceof IInterfaceTypeNode)) {
			INEDTypeInfo directBaseType = getNEDElement().getFirstExtendsNEDTypeInfo();
			if (directBaseType != null)
				allInterfaces.addAll(directBaseType.getInterfaces());
		}
		allInterfaces.addAll(ownInterfaces);
		for (String interfaceName : ownInterfaces) {
			INEDTypeInfo typeInfo = resolver.getComponent(interfaceName);
			if (typeInfo != null)
				allInterfaces.addAll(typeInfo.getInterfaces());
		}
		
        // collect all inherited members
		INEDTypeInfo[] forwardExtendsChain = extendsChain.toArray(new INEDTypeInfo[]{});
		ArrayUtils.reverse(forwardExtendsChain);
		for (INEDTypeInfo typeInfo : forwardExtendsChain) {
			Assert.isTrue(typeInfo instanceof NEDComponent);
			NEDComponent component = (NEDComponent)typeInfo;
			allProperties.putAll(component.getOwnProperties());
			allParams.putAll(component.getOwnParams());
            allParamValues.putAll(component.getOwnParamValues());
			allGates.putAll(component.getOwnGates());
            allGateSizes.putAll(component.getOwnGateSizes());
			allInnerTypes.putAll(component.getOwnInnerTypes());
			allSubmodules.putAll(component.getOwnSubmodules());
			allMembers.putAll(component.getOwnMembers());
		}

//        // additional tables for derived types and types using this one
//		allDerivedTypes.clear();
//
//		// collect all types that are derived from this
//        for (INEDTypeInfo currentComp : getResolver().getAllComponents()) {
//            if (currentComp == this)
//                continue;
//
//            // check for components the are extending us (directly or indirectly)
//            INEDElement element = currentComp.getNEDElement();
//            for (INEDElement child : element) {
//                if (child instanceof ExtendsNode) {
//                    String extendsName = ((ExtendsNode)child).getName();
//                    if (getName().equals(extendsName)) {
//                        allDerivedTypes.add(currentComp);
//                    }
//                }
//            }
//
//            // check for components that contain submodules, connections that use this type
//            if (currentComp.getOwnUsedTypes().contains(getName())) {
//                allUsingTypes.add(currentComp);
//            }
//        }
        needsUpdate = false;
	}

	/**
	 * Causes information about inherited members to be discarded, and
	 * later re-built on demand.
	 */
	public void invalidate() {
        needsOwnUpdate = true;
		needsUpdate = true;
	}

	public String getName() {
		return componentNode.getName();
	}

	public INedTypeNode getNEDElement() {
		return componentNode;
	}

	public IFile getNEDFile() {
		return file;
	}

	public INEDElement[] getNEDElementsAt(int line, int column) {
		ArrayList<INEDElement> list = new ArrayList<INEDElement>();
		NEDSourceRegion region = componentNode.getSourceRegion();
		if (region!=null && region.contains(line, column)) {
			list.add(componentNode);
			collectNEDElements(componentNode, line, column, list);
			return list.toArray(new INEDElement[list.size()]);
		}
		return null;
	}

	protected void collectNEDElements(INEDElement node, int line, int column, List<INEDElement> list) {
		for (INEDElement child : node) {
			NEDSourceRegion region = child.getSourceRegion();
			if (region!=null && region.contains(line, column)) {
				list.add(child);
				collectNEDElements(child, line, column, list); // children fall inside parent's region
			}
		}
	}

    public List<INEDTypeInfo> getExtendsChain() {
    	refreshInheritedMembersIfNeeded();
		return extendsChain;
	}

	public Set<String> getOwnInterfaces() {
		refreshOwnMembersIfNeeded();
        return ownInterfaces;
	}

    public Map<String,ParamNode> getOwnParams() {
    	refreshOwnMembersIfNeeded();
        return ownParams;
    }

    public Map<String,ParamNode> getOwnParamValues() {
    	refreshOwnMembersIfNeeded();
        return ownParamValues;
    }

    public Map<String,PropertyNode> getOwnProperties() {
    	refreshOwnMembersIfNeeded();
        return ownProperties;
    }

    public Map<String,GateNode> getOwnGates() {
    	refreshOwnMembersIfNeeded();
        return ownGates;
    }

    public Map<String,GateNode> getOwnGateSizes() {
    	refreshOwnMembersIfNeeded();
        return ownGateSizes;
    }

    public Map<String,INedTypeNode> getOwnInnerTypes() {
    	refreshOwnMembersIfNeeded();
        return ownInnerTypes;
    }

    public Map<String,SubmoduleNode> getOwnSubmodules() {
    	refreshOwnMembersIfNeeded();
        return ownSubmodules;
    }

    public Map<String,INEDElement> getOwnMembers() {
    	refreshOwnMembersIfNeeded();
        return ownMembers;
    }

    public Set<String> getOwnUsedTypes() {
    	refreshOwnMembersIfNeeded();
        return ownUsedTypes;
    }

    public Set<String> getInterfaces() {
    	refreshInheritedMembersIfNeeded();
        return allInterfaces;
    }

    public Map<String, ParamNode> getParams() {
    	refreshInheritedMembersIfNeeded();
        return allParams;
    }

    public Map<String, ParamNode> getParamValues() {
    	refreshInheritedMembersIfNeeded();
        return allParamValues;
    }

    public Map<String, PropertyNode> getProperties() {
    	refreshInheritedMembersIfNeeded();
        return allProperties;
    }

    public Map<String, GateNode> getGates() {
    	refreshInheritedMembersIfNeeded();
        return allGates;
    }

    public Map<String, GateNode> getGateSizes() {
    	refreshInheritedMembersIfNeeded();
        return allGateSizes;
    }

    public Map<String, INedTypeNode> getInnerTypes() {
    	refreshInheritedMembersIfNeeded();
        return allInnerTypes;
    }

    public Map<String, SubmoduleNode> getSubmodules() {
    	refreshInheritedMembersIfNeeded();
        return allSubmodules;
    }

    public Map<String, INEDElement> getMembers() {
    	refreshInheritedMembersIfNeeded();
        return allMembers;
    }

//    public List<INEDTypeInfo> getAllDerivedTypes() {
//        if (needsUpdate)
//            refreshInheritedMembers();
//        return allDerivedTypes;
//    }

    public List<INEDTypeInfo> getAllUsingTypes() {
    	refreshInheritedMembersIfNeeded();
        return allUsingTypes;
    }

	public List<ParamNode> getParameterInheritanceChain(String parameterName) {
		List<ParamNode> result = new ArrayList<ParamNode>();
		for (INEDTypeInfo type : getExtendsChain())
			if (type.getOwnParams().containsKey(parameterName))
				result.add((ParamNode) type.getOwnParams().get(parameterName));
		return result;
	}

	public List<GateNode> getGateInheritanceChain(String gateName) {
		List<GateNode> result = new ArrayList<GateNode>();
		for (INEDTypeInfo type : getExtendsChain())
			if (type.getOwnGates().containsKey(gateName))
				result.add(type.getOwnGates().get(gateName));
		return result;
	}

	public List<PropertyNode> getPropertyInheritanceChain(String propertyName) {
		List<PropertyNode> result = new ArrayList<PropertyNode>();
		for (INEDTypeInfo type : getExtendsChain())
			if (type.getOwnProperties().containsKey(propertyName))
				result.add((PropertyNode) type.getOwnProperties().get(propertyName));
		return result;
	}

	/* (non-Javadoc)
     * @see java.lang.Object#toString()
     * Displays debugging info
     */
    @Override
    public String toString() {
        return "NEDComponent for "+getNEDElement();
    }

    public void modelChanged(NEDModelEvent event) {
        invalidate();
    }

    public void debugDump() {
    	System.out.println("NEDComponent: " + getNEDElement().toString() + " debugId=" + debugId);
    	if (needsUpdate || needsOwnUpdate)
    		System.out.println(" currently invalid (needs refresh)");
    	System.out.println("  extends chain: " + StringUtils.join(getExtendsChain(), ", "));
    	System.out.println("  own interfaces: " + StringUtils.join(ownInterfaces, ", "));
    	System.out.println("  all interfaces: " + StringUtils.join(allInterfaces, ", "));
    	System.out.println("  own gates: " + StringUtils.join(ownGates.keySet(), ", "));
    	System.out.println("  all gates: " + StringUtils.join(allGates.keySet(), ", "));
    	System.out.println("  own parameters: " + StringUtils.join(ownParams.keySet(), ", "));
    	System.out.println("  all parameters: " + StringUtils.join(allParams.keySet(), ", "));
    	System.out.println("  own properties: " + StringUtils.join(ownProperties.keySet(), ", "));
    	System.out.println("  all properties: " + StringUtils.join(allProperties.keySet(), ", "));
    	System.out.println("  own submodules: " + StringUtils.join(ownSubmodules.keySet(), ", "));
    	System.out.println("  all submodules: " + StringUtils.join(allSubmodules.keySet(), ", "));
    }
}
