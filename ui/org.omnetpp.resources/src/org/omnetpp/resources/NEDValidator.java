package org.omnetpp.resources;

import java.util.HashMap;

import org.eclipse.core.runtime.Assert;
import org.omnetpp.ned2.model.NEDElement;
import org.omnetpp.ned2.model.NEDElementUtil;
import org.omnetpp.ned2.model.pojo.AbstractNEDValidator;
import org.omnetpp.ned2.model.pojo.ChannelInterfaceNode;
import org.omnetpp.ned2.model.pojo.ChannelNode;
import org.omnetpp.ned2.model.pojo.ChannelSpecNode;
import org.omnetpp.ned2.model.pojo.ClassDeclNode;
import org.omnetpp.ned2.model.pojo.ClassNode;
import org.omnetpp.ned2.model.pojo.CompoundModuleNode;
import org.omnetpp.ned2.model.pojo.ConditionNode;
import org.omnetpp.ned2.model.pojo.ConnectionGroupNode;
import org.omnetpp.ned2.model.pojo.ConnectionNode;
import org.omnetpp.ned2.model.pojo.ConnectionsNode;
import org.omnetpp.ned2.model.pojo.CplusplusNode;
import org.omnetpp.ned2.model.pojo.EnumDeclNode;
import org.omnetpp.ned2.model.pojo.EnumFieldNode;
import org.omnetpp.ned2.model.pojo.EnumFieldsNode;
import org.omnetpp.ned2.model.pojo.EnumNode;
import org.omnetpp.ned2.model.pojo.ExpressionNode;
import org.omnetpp.ned2.model.pojo.ExtendsNode;
import org.omnetpp.ned2.model.pojo.FieldNode;
import org.omnetpp.ned2.model.pojo.FieldsNode;
import org.omnetpp.ned2.model.pojo.FilesNode;
import org.omnetpp.ned2.model.pojo.FunctionNode;
import org.omnetpp.ned2.model.pojo.GateGroupNode;
import org.omnetpp.ned2.model.pojo.GateNode;
import org.omnetpp.ned2.model.pojo.GatesNode;
import org.omnetpp.ned2.model.pojo.IdentNode;
import org.omnetpp.ned2.model.pojo.ImportNode;
import org.omnetpp.ned2.model.pojo.InterfaceNameNode;
import org.omnetpp.ned2.model.pojo.LiteralNode;
import org.omnetpp.ned2.model.pojo.LoopNode;
import org.omnetpp.ned2.model.pojo.MessageDeclNode;
import org.omnetpp.ned2.model.pojo.MessageNode;
import org.omnetpp.ned2.model.pojo.ModuleInterfaceNode;
import org.omnetpp.ned2.model.pojo.MsgFileNode;
import org.omnetpp.ned2.model.pojo.MsgpropertyNode;
import org.omnetpp.ned2.model.pojo.NedFileNode;
import org.omnetpp.ned2.model.pojo.OperatorNode;
import org.omnetpp.ned2.model.pojo.ParamGroupNode;
import org.omnetpp.ned2.model.pojo.ParamNode;
import org.omnetpp.ned2.model.pojo.ParametersNode;
import org.omnetpp.ned2.model.pojo.PatternNode;
import org.omnetpp.ned2.model.pojo.PropertiesNode;
import org.omnetpp.ned2.model.pojo.PropertyDeclNode;
import org.omnetpp.ned2.model.pojo.PropertyKeyNode;
import org.omnetpp.ned2.model.pojo.PropertyNode;
import org.omnetpp.ned2.model.pojo.SimpleModuleNode;
import org.omnetpp.ned2.model.pojo.StructDeclNode;
import org.omnetpp.ned2.model.pojo.StructNode;
import org.omnetpp.ned2.model.pojo.SubmoduleNode;
import org.omnetpp.ned2.model.pojo.SubmodulesNode;
import org.omnetpp.ned2.model.pojo.TypesNode;
import org.omnetpp.ned2.model.pojo.UnknownNode;
import org.omnetpp.ned2.model.pojo.WhereNode;
import org.omnetpp.ned2.model.pojo.WhitespaceNode;

/**
 * Validates consistency of NED files.
 * 
 * @author andras
 */
// XXX move to org.omnetpp.ned2.model plugin? then INEDComponent,
// INEDComponentResolver etc would have to be moved as well, and that plugin
// would have to depend on org.eclipse.resources because of IFile!!!
public class NEDValidator extends AbstractNEDValidator implements NEDElementUtil {

	INEDComponentResolver resolver;

	INEDErrorStore errors;

	// the component currently being validated
	NEDElement componentNode;
	
	// non-null while we're validating a submodule
	SubmoduleNode submoduleNode;
	INEDComponent submoduleType; // null for the "like *" case(!); valid while submoduleNode!=null

	// non-null while we're validating a channelspec of a connection
	ChannelSpecNode channelSpecNode;
	INEDComponent channelSpecType; // may be null; valid while channelSpecNode!=null

	// members of the component currently being validated
	HashMap<String, NEDElement> members = new HashMap<String, NEDElement>();

	public NEDValidator(INEDComponentResolver resolver, INEDErrorStore errors) {
		this.resolver = resolver;
		this.errors = errors;
	}

	@Override
	public void validate(NEDElement node) {
		validateElement(node);
	}

	protected void validateChildren(NEDElement node) {
		for (NEDElement child : node)
			validate(child);
	}

	protected void validateElement(FilesNode node) {
		validateChildren(node);
	}

	protected void validateElement(NedFileNode node) {
		validateChildren(node);
	}

	protected void validateElement(WhitespaceNode node) {
		validateChildren(node);
	}

	protected void validateElement(ImportNode node) {
		//TODO check if file exists?
		validateChildren(node);
	}

	protected void validateElement(PropertyDeclNode node) {
		validateChildren(node);
	}

	protected void validateElement(ExtendsNode node) {
		Assert.isTrue(componentNode!=null);

		// referenced component must exist and must be the same type as this one
		String name = node.getName();
		INEDComponent e = resolver.getComponent(name);
		if (e == null) {
			errors.add(node, "no such component: '" + name+"'");
			return;
		}
		int thisType = componentNode.getTagCode();
		int extendsType = e.getNEDElement().getTagCode();
		if (thisType != extendsType) { // XXX loosen condition if "module Foo extends ASimple" is legal!
			errors.add(node, "'"+name+"' is not a "+componentNode.getTagName());
			return;
		}

		// if all OK, add inherited members to our member list
		for (String memberName : e.getMemberNames()) {
			if (members.containsKey(memberName))
				errors.add(node, "conflict: '"+memberName+"' occurs in multiple base interfaces");
			else
			    members.put(memberName, e.getMember(memberName));
		}
		
		// then process children
		validateChildren(node);
	}

	protected void validateElement(InterfaceNameNode node) {
		// nothing to do here: compliance to "like" interfaces will be checked 
		// after we finished validating the component 
		validateChildren(node);
	}

	protected void validateElement(SimpleModuleNode node) {
		doValidateComponent(node); 
	}

	protected void validateElement(ModuleInterfaceNode node) {
		doValidateComponent(node); 
	}

	protected void validateElement(CompoundModuleNode node) {
		doValidateComponent(node); 
	}

	protected void validateElement(ChannelInterfaceNode node) {
		doValidateComponent(node); 
	}

	protected void validateElement(ChannelNode node) {
        // init
		componentNode = node;
		Assert.isTrue(members.isEmpty());
		
		// for channels, we have to pretend they have three implicit parameters...
		createImplicitParameter("delay", NED_PARTYPE_DOUBLE);
		createImplicitParameter("error", NED_PARTYPE_DOUBLE);
		createImplicitParameter("datarate", NED_PARTYPE_DOUBLE);
		
		// do the work
		validateChildren(node);
		//XXX check compliance to "like" interfaces
		
		// clean up
		componentNode = null;
		members.clear();
		Assert.isTrue(members.isEmpty());
	}

	/* utility method */
	protected void createImplicitParameter(String name, int type) {
		ParamNode param = new ParamNode();
		param.setName(name);
		param.setType(type);
		param.setIsFunction(false);
		param.setIsDefault(false);
		//TODO add default value of zero
		param.setSourceLocation("internal");
		members.put(param.getName(), param);
	}

	/* utility method */
	protected void doValidateComponent(NEDElement node) {
        // init
		componentNode = node;
		Assert.isTrue(members.isEmpty());
		
		// do the work
		validateChildren(node);
		//XXX check compliance to "like" interfaces
		
		// clean up
		componentNode = null;
		members.clear();
		Assert.isTrue(members.isEmpty());
	}

	protected void validateElement(ParametersNode node) {
		validateChildren(node);
	}

	protected void validateElement(ParamGroupNode node) {
		validateChildren(node);
	}

	protected void validateElement(ParamNode node) {
		// structural, not checked by the DTD
		if (node.getParent() instanceof ParamGroupNode) {
			// definitions not allowed inside groups
			if (node.getType() != NED_PARTYPE_NONE) {
				errors.add(node, "parameters cannot be defined inside a group");
				return;
			}
			// conditionals not allowed inside groups
			if (node.getFirstChildWithTag(NED_CONDITION) != null) {
				errors.add(node, "conditionals are not allowed inside a group");
				return;
			}
		}

		// parameter definitions
		String parname = node.getName();
		if (node.getType()!=NED_PARTYPE_NONE) {
			// check definitions: allowed here at all?
			if (submoduleNode!=null || channelSpecNode!=null) {
				errors.add(node, "new parameters can only be defined on a (module/channel) type");
				return;
			}

			// param must NOT exist already
			if (members.containsKey(parname)) {
				errors.add(node, "'"+parname+"': already defined at "+members.get(parname).getSourceLocation()); // and may not be a parameter at all...
				return;
			}
			members.put(parname, node);
		}

		// check assignments: the param must exist already, find definition
		ParamNode decl = null;
		if (submoduleNode!=null) {
			// inside a submodule's definition
			if (submoduleType==null) {
				errors.add(node, "cannot assign parameters of a submodule of unknown type");
				return;
			}
			decl = (ParamNode) submoduleType.getMember(parname);
			if (decl==null || decl.getTagCode()!=NED_PARAM) {
				errors.add(node, "'"+parname+"': type '"+submoduleType.getName()+"' has no such parameter");
				return;
			}
		}
		else if (channelSpecNode!=null) {
			// inside a connection's channel spec
			if (channelSpecType==null) {
				errors.add(node, "cannot assign parameters of a channel of unknown type");
				return;
			}
			decl = (ParamNode) channelSpecType.getMember(parname);
			if (decl==null || decl.getTagCode()!=NED_PARAM) {
				errors.add(node, "'"+parname+"': type '"+submoduleType.getName()+"' has no such parameter");
				return;
			}
		}
		else {
			// global "parameters" section of type
			if (!members.containsKey(parname)) {
				errors.add(node, "'"+parname+"': undefined parameter");
				return;
			}
			decl = (ParamNode)members.get(parname);
		}

		//XXX: check expression matches type in the declaration

		validateChildren(node);
	}

	protected void validateElement(PatternNode node) {
		validateChildren(node);
	}

	protected void validateElement(PropertyNode node) {
		validateChildren(node);
	}

	protected void validateElement(PropertyKeyNode node) {
		validateChildren(node);
	}

	protected void validateElement(GatesNode node) {
		validateChildren(node);
	}

	protected void validateElement(GateGroupNode node) {
		validateChildren(node);
	}

	protected void validateElement(GateNode node) {
		validateChildren(node);
	}

	protected void validateElement(TypesNode node) {
		validateChildren(node);
	}

	protected void validateElement(SubmodulesNode node) {
		validateChildren(node);
	}

	protected void validateElement(SubmoduleNode node) {
		// find submodule type
		String name = node.getName();  
		String typeName = node.getType();  
		String likeTypeName = node.getLikeType();  
		if (typeName!=null && !typeName.equals("")) {
			// normal case
			submoduleType = resolver.getComponent(typeName);
			if (submoduleType == null) {
				errors.add(node, "'"+typeName+"': no such module type");
				return;
			}
			int typeTag = submoduleType.getNEDElement().getTagCode();
			if (typeTag!=NED_SIMPLE_MODULE && typeTag!=NED_COMPOUND_MODULE) {
				errors.add(node, "'"+typeName+"' is not a module type");
				return;
			}
		}
		else if ("*".equals(likeTypeName)) {
			// unchecked "like"...
			submoduleType = null;
		}
		else if (likeTypeName!=null && !likeTypeName.equals("")) {
			// "like" case
			submoduleType = resolver.getComponent(likeTypeName);
			if (submoduleType == null) {
				errors.add(node, "'"+likeTypeName+"': no such module or interface type");
				return;
			}
			int typeTag = submoduleType.getNEDElement().getTagCode();
			if (typeTag!=NED_SIMPLE_MODULE && typeTag!=NED_COMPOUND_MODULE && typeTag!=NED_MODULE_INTERFACE) {
				errors.add(node, "'"+typeName+"' is not a module or interface type");
				return;
			}
		}
		else {
			errors.add(node, "no type info for '"+name+"'"); // should never happen
			return;
		}
		
		// validate contents
		submoduleNode = node;
		validateChildren(node);
		submoduleNode = null;
	}

	protected void validateElement(ConnectionsNode node) {
		validateChildren(node);
	}

	protected void validateElement(ConnectionNode node) {
		validateChildren(node);
	}

	protected void validateElement(ChannelSpecNode node) {
		channelSpecNode = node;
		validateChildren(node);
		channelSpecNode = null;
	}

	protected void validateElement(ConnectionGroupNode node) {
		validateChildren(node);
	}

	protected void validateElement(WhereNode node) {
		validateChildren(node);
	}

	protected void validateElement(LoopNode node) {
		validateChildren(node);
	}

	protected void validateElement(ConditionNode node) {
		validateChildren(node);
	}

	protected void validateElement(ExpressionNode node) {
		validateChildren(node);
	}

	protected void validateElement(OperatorNode node) {
		validateChildren(node);
	}

	protected void validateElement(FunctionNode node) {
		validateChildren(node);
	}

	protected void validateElement(IdentNode node) {
		validateChildren(node);
	}

	protected void validateElement(LiteralNode node) {
		validateChildren(node);
	}

	protected void validateElement(MsgFileNode node) {
		validateChildren(node);
	}

	protected void validateElement(CplusplusNode node) {
		validateChildren(node);
	}

	protected void validateElement(StructDeclNode node) {
		validateChildren(node);
	}

	protected void validateElement(ClassDeclNode node) {
		validateChildren(node);
	}

	protected void validateElement(MessageDeclNode node) {
		validateChildren(node);
	}

	protected void validateElement(EnumDeclNode node) {
		validateChildren(node);
	}

	protected void validateElement(EnumNode node) {
		validateChildren(node);
	}

	protected void validateElement(EnumFieldsNode node) {
		validateChildren(node);
	}

	protected void validateElement(EnumFieldNode node) {
		validateChildren(node);
	}

	protected void validateElement(MessageNode node) {
		validateChildren(node);
	}

	protected void validateElement(ClassNode node) {
		validateChildren(node);
	}

	protected void validateElement(StructNode node) {
		validateChildren(node);
	}

	protected void validateElement(FieldsNode node) {
		validateChildren(node);
	}

	protected void validateElement(FieldNode node) {
		validateChildren(node);
	}

	protected void validateElement(PropertiesNode node) {
		validateChildren(node);
	}

	protected void validateElement(MsgpropertyNode node) {
		validateChildren(node);
	}

	protected void validateElement(UnknownNode node) {
		validateChildren(node);
	}
}
