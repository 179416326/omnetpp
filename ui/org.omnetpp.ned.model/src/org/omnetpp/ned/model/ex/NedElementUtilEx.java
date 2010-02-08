/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.model.ex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.ned.model.DisplayString;
import org.omnetpp.ned.model.INedElement;
import org.omnetpp.ned.model.NedElement;
import org.omnetpp.ned.model.NedElementConstants;
import org.omnetpp.ned.model.interfaces.IHasDisplayString;
import org.omnetpp.ned.model.interfaces.IHasName;
import org.omnetpp.ned.model.interfaces.IHasParameters;
import org.omnetpp.ned.model.interfaces.IHasProperties;
import org.omnetpp.ned.model.interfaces.IHasType;
import org.omnetpp.ned.model.interfaces.INedTypeInfo;
import org.omnetpp.ned.model.interfaces.INedTypeResolver;
import org.omnetpp.ned.model.pojo.CommentElement;
import org.omnetpp.ned.model.pojo.CompoundModuleElement;
import org.omnetpp.ned.model.pojo.ConnectionElement;
import org.omnetpp.ned.model.pojo.ConnectionGroupElement;
import org.omnetpp.ned.model.pojo.ExtendsElement;
import org.omnetpp.ned.model.pojo.ImportElement;
import org.omnetpp.ned.model.pojo.LiteralElement;
import org.omnetpp.ned.model.pojo.NedElementFactory;
import org.omnetpp.ned.model.pojo.NedElementTags;
import org.omnetpp.ned.model.pojo.ParametersElement;
import org.omnetpp.ned.model.pojo.PropertyElement;
import org.omnetpp.ned.model.pojo.PropertyKeyElement;

/**
 * TODO add documentation
 *
 * @author rhornig
 */
public class NedElementUtilEx implements NedElementTags, NedElementConstants {
	public static final String DISPLAY_PROPERTY = "display";

	//XXX move to NedElement
	public static boolean isDisplayStringUpToDate(DisplayString displayStringObject, IHasDisplayString owner) {
		// Note: cannot call owner.getDisplayString() here, as it also wants to update the fallback, etc.
		String displayStringLiteral = StringUtils.nullToEmpty(getDisplayStringLiteral(owner));
		String cachedDisplayString = displayStringObject == null ? "" : displayStringObject.toString();
		return displayStringLiteral.equals(cachedDisplayString);
	}

	/**
	 * Returns the display string of the given element (submodule, connection or
	 * toplevel type) in an unparsed form, from the NED tree.
	 *
	 * Returns null if the NED tree doesn't contain a display string.
	 */
	public static String getDisplayStringLiteral(IHasDisplayString node) {
        // for connections, we need to look inside the channelSpec node for the display string
		INedElement parametersParent = (node instanceof ConnectionElement) ?
				((ConnectionElement)node).getFirstChildWithTag(NED_CHANNEL_SPEC) : (INedElement)node;

		// look for the "display" property inside the parameters section (if it exists)
        INedElement parametersNode = parametersParent == null ? null : parametersParent.getFirstChildWithTag(NED_PARAMETERS);
        if (parametersNode != null)
        	for (INedElement currElement : parametersNode)
        		if (currElement instanceof PropertyElement) {
        			PropertyElement currProp = (PropertyElement)currElement;
        			if (DISPLAY_PROPERTY.equals(currProp.getName()))
        				return getPropertyValue(currProp);
        		}
        return null;
	}

	/**
	 * Sets the display string of a given node in the NED tree,
	 * by creating or updating the LiteralElement that contains the
	 * "@display" property in the tree.
	 *
	 * Returns the LiteralElement which was created/updated.
	 */
	public static LiteralElement setDisplayStringLiteral(IHasDisplayString node1, String displayString) {
		INedElement node = node1;

		// the connection node is special because the display string is stored inside its
        // channel spec node, so we must create that too
        if (node instanceof ConnectionElement) {
            INedElement channelSpecNode = node.getFirstChildWithTag(NED_CHANNEL_SPEC);
            if (channelSpecNode == null) {
                channelSpecNode = NedElementFactoryEx.getInstance().createElement(NED_CHANNEL_SPEC);
                node.appendChild(channelSpecNode);
            }
            // use the new channel spec node as the container of display string
            node = channelSpecNode;
        }

		// look for the "parameters" block
		INedElement paramsNode = node.getFirstChildWithTag(NED_PARAMETERS);
		if (paramsNode == null) {
			paramsNode = NedElementFactoryEx.getInstance().createElement(NED_PARAMETERS);
            ((ParametersElement)paramsNode).setIsImplicit(true);
			node.appendChild(paramsNode);
		}

		// look for the first property parameter named "display"
		INedElement displayPropertyNode = paramsNode.getFirstChildWithAttribute(NED_PROPERTY, PropertyElement.ATT_NAME, DISPLAY_PROPERTY);
		if (displayPropertyNode == null) {
			displayPropertyNode = NedElementFactoryEx.getInstance().createElement(NED_PROPERTY);
			((PropertyElement)displayPropertyNode).setName(DISPLAY_PROPERTY);
			paramsNode.appendChild(displayPropertyNode);
		}

        // if displayString was set to "" (empty), we want to delete the whole display property node
        if ("".equals(displayString)){
            paramsNode.removeChild(displayPropertyNode);
            return null;
        }

		// look for the property key
		INedElement propertyKeyNode = displayPropertyNode.getFirstChildWithAttribute(NED_PROPERTY_KEY, PropertyKeyElement.ATT_NAME, "");
		if (propertyKeyNode == null) {
			propertyKeyNode = NedElementFactoryEx.getInstance().createElement(NED_PROPERTY_KEY);
			displayPropertyNode.appendChild(propertyKeyNode);
		}

		// look up or create the LiteralElement
		LiteralElement literalElement = (LiteralElement)propertyKeyNode.getFirstChildWithTag(NED_LITERAL);
		if (literalElement == null)
			literalElement = (LiteralElement)NedElementFactoryEx.getInstance().createElement(NED_LITERAL);

		// fill in the LiteralElement. if()'s are there to minimize the number of notifications
		if (literalElement.getType() != NED_CONST_STRING)
			literalElement.setType(NED_CONST_STRING);
		if (!displayString.equals(literalElement.getValue()))
			literalElement.setValue(displayString);
		// invalidate the text representation, so that next time the code will
		// be generated from VALUE, not from the text attribute
		if (literalElement.getText() != null)
			literalElement.setText(null);
		// if new, add it only here (also to minimize notifications)
		if (literalElement.getParent() == null)
			propertyKeyNode.appendChild(literalElement);
		return literalElement;
	}

	/**
	 * Sets the value of the given boolean property on a node. If value is true adds it to the tree otherwise removes it.
	 * @param value
	 */
	public static void setBooleanProperty(IHasParameters node, String propertyName, boolean value) {
		// look for the "parameters" block
		INedElement paramsNode = node.getFirstChildWithTag(NED_PARAMETERS);

		if (paramsNode == null) {
			if (value == false) return; // optimize away property node for default value

			paramsNode = NedElementFactoryEx.getInstance().createElement(NED_PARAMETERS);
            ((ParametersElement)paramsNode).setIsImplicit(true);
			node.appendChild(paramsNode);
		}

		// look for the first property parameter named "display"
		INedElement isBooleanPropertyNode = paramsNode.getFirstChildWithAttribute(NED_PROPERTY, PropertyElement.ATT_NAME, propertyName);

		// first we always remove the original node (so all its children will be discarded)
		if (isBooleanPropertyNode !=null)
			paramsNode.removeChild(isBooleanPropertyNode);
		// create and add a new node if needed
		if (value) {
			isBooleanPropertyNode = NedElementFactoryEx.getInstance().createElement(NED_PROPERTY);
			((PropertyElement)isBooleanPropertyNode).setIsImplicit(node instanceof CompoundModuleElement);
			((PropertyElement)isBooleanPropertyNode).setName(propertyName);
			paramsNode.appendChild(isBooleanPropertyNode);
		}
	}

    /**
     * Returns the name of the first "extends" node (or null if none present)
     */
    public static String getFirstExtends(INedElement node) {
        ExtendsElement extendsElement = (ExtendsElement)node.getFirstChildWithTag(NED_EXTENDS);
        return extendsElement == null ? null : extendsElement.getName();
    }

    /**
     * Sets the name of object that is extended by node. if we set it to null or "" the node will be removed
     * @param node The node which extends the provided type
     * @param ext The name of the type that is extended
     */
    public static void setFirstExtends(INedElement node, String ext) {
        ExtendsElement extendsElement = (ExtendsElement)node.getFirstChildWithTag(NED_EXTENDS);
            if (extendsElement == null && ext != null && !"".equals(ext)) {
                extendsElement = (ExtendsElement)NedElementFactoryEx.getInstance().createElement(NED_EXTENDS);
                node.appendChild(extendsElement);
            }
            else if (extendsElement != null && (ext == null || "".equals(ext))) {
                // first set the name to "" so we will generate an attribute change event
                extendsElement.setName("");
                // remove the node if we would set the name to null or empty string
                node.removeChild(extendsElement);
            }
            if (extendsElement != null)
                extendsElement.setName(ext);
    }

	/**
	 * Returns the value of the given property (assigned to the first key (default value))
	 */
	public static String getPropertyValue(PropertyElement prop) {
		PropertyKeyElement pkn = prop.getFirstPropertyKeyChild();
		if (pkn == null ) return null;
		LiteralElement ln = pkn.getFirstLiteralChild();
		if (ln == null ) return null;

		return ln.getValue();
	}

    /**
     * Returns the name of component but stripped any digits from the right ie: "name123" would be "name"
     */
    private static String getNameBase(String name) {
        int i = name.length()-1;
        while (i>=0 && Character.isDigit(name.charAt(i))) --i;
        // strip away the digits at the end
        return name.substring(0,i+1);
    }

    /**
     * Calculates a unique name for the provided model element.
     */
    public static String getUniqueNameFor(String currentName, Set<String> contextCollection) {
        // if there is no name in the context with the same name we don't have to change the name
        if (!contextCollection.contains(currentName))
            return currentName;

        // there is an other module with the same name, so find a new name
        String baseName = getNameBase(currentName);
        int i = 1;
        while(contextCollection.contains(new String(baseName+i)))
            i++;

        // we've found a unique name
        return baseName+i;
    }

    /**
     * Calculates a unique name for the provided model element
     * @param namedElement
     * @param contextCollection A collection of strings which provides a context in which the name should be unique
     * @return The new unique name, or the original name if it was unique
     */
    public static String getUniqueNameFor(IHasName namedElement, Collection<? extends IHasName> contextCollection) {
        Set<String> nameSet = new HashSet<String>(contextCollection.size());
        for (IHasName sm : contextCollection)
            if (sm != namedElement)
                nameSet.add(sm.getName());
        return getUniqueNameFor(namedElement.getName(), nameSet);
    }

    /**
     * Selects a name for a toplevel type, ensuring that the name will be unique in the package.
     */
    public static String getUniqueNameForToplevelType(String currentName, NedFileElementEx nedFile) {
        INedTypeResolver res = NedElement.getDefaultNedTypeResolver();
        IProject project = res.getNedFile(nedFile).getProject();
        Set<String> reservedNames = res.getReservedQNames(project);
        String currentQName = nedFile.getQNameAsPrefix() + currentName;
        String uniqueQName = getUniqueNameFor(currentQName, reservedNames);
        return uniqueQName.contains(".") ? StringUtils.substringAfterLast(uniqueQName, ".") : uniqueQName;
    }

    /**
     * Checks whether the provided string is a valid NED identifier
     */
    public static boolean isValidIdentifier(String str) {
        return str != null && str.matches("[a-zA-Z_][a-zA-Z0-9_]*");
    }

    /**
     * When the user renames a submodule, we need to update the connections in the
     * same compound module (and its subclasses) accordingly, so that the model
     * will remain consistent. This method performs this change, for one compound
     * module.
     */
    public static void renameSubmoduleInConnections(CompoundModuleElementEx compoundModule, String oldSubmoduleName, String newSubmoduleName) {
		INedElement connectionsNode = compoundModule.getFirstConnectionsChild();
		if (connectionsNode != null)
			doRenameSubmoduleInConnections(connectionsNode, oldSubmoduleName, newSubmoduleName);
	}

	protected static void doRenameSubmoduleInConnections(INedElement parent, String oldName, String newName) {
		for (INedElement child : parent) {
			if (child instanceof ConnectionElementEx) {
				ConnectionElementEx conn = (ConnectionElementEx) child;
				if (conn.getSrcModule().equals(oldName))
					conn.setSrcModule(newName);
				if (conn.getDestModule().equals(oldName))
					conn.setDestModule(newName);
			}
			else if (child instanceof ConnectionGroupElement) {
				doRenameSubmoduleInConnections(child, oldName, newName);
			}
		}
	}

    /**
     * Given a submodule or connection whose type name is a fully qualified name,
     * this method replaces it with the simple name plus an import in the NED file
     * if needed/possible.
     *
     * Returns the newly created ImportElement, or null if no import got added.
     * (I.e. it returns null as well if an existing import already covered this type.)
     */
	public static ImportElement addImportFor(IHasType submoduleOrConnection) {
	    String effectiveType = submoduleOrConnection.getEffectiveType();
        if (StringUtils.isEmpty(effectiveType))
	            return null;

		ImportElement theImport = null;
		CompoundModuleElementEx parent = (CompoundModuleElementEx) submoduleOrConnection.getEnclosingTypeElement();

		if (effectiveType.contains(".")) {
			String fullyQualifiedTypeName = effectiveType;
        	String simpleTypeName = StringUtils.substringAfterLast(fullyQualifiedTypeName, ".");
			INedTypeInfo existingSimilarType = NedElement.getDefaultNedTypeResolver().lookupNedType(simpleTypeName, parent);
			if (existingSimilarType == null) {
				// add import
				theImport = parent.getContainingNedFileElement().addImport(fullyQualifiedTypeName);
				setEffectiveType(submoduleOrConnection, simpleTypeName);
			}
			else if (existingSimilarType.getFullyQualifiedName().equals(fullyQualifiedTypeName)) {
				// import not needed, this type is already visible: just use short name
				setEffectiveType(submoduleOrConnection, simpleTypeName);
			}
			else {
				// do nothing: another module with the same simple name already imported, so leave fully qualified name
			}

        }
		return theImport;
	}

	/**
	 * Sets whichever of "type" and "like-type" is already set on the element
	 */
	public static void setEffectiveType(IHasType submoduleOrConnection, String value) {
		if (StringUtils.isNotEmpty(submoduleOrConnection.getLikeType()))
			submoduleOrConnection.setLikeType(value);
		else
			submoduleOrConnection.setType(value);
	}

	/**
	 * Given an import that contains "*" or "**" as wildcard, this method returns
	 * the corresponding regex that can be used to check whether a fully qualified
	 * type name matches the import.
	 */
	public static String importToRegex(String importSpec) {
		return importSpec.replace(".", "\\.").replace("**", ".*").replace("*", "[^.]*");
	}

	public interface INedElementVisitor {
		void visit(INedElement element);
	}

	public static void visitNedTree(INedElement element, INedElementVisitor visitor) {
		visitor.visit(element);
		for (INedElement child : element)
			visitNedTree(child, visitor);
	}

	public static CommentElement createCommentElement(String locId, String content) {
		CommentElement comment = (CommentElement)NedElementFactoryEx.getInstance().createElement(NedElementTags.NED_COMMENT);
		comment.setLocid(locId);
		comment.setContent(content);
		return comment;
	}


    public static Map<String, Map<String, PropertyElementEx>> collectProperties(INedElement node, Map<String, Map<String, PropertyElementEx>> map) {
        INedElement section = node.getFirstChildWithTag(NED_PARAMETERS);
        if (section != null) {
            for (INedElement child : section) {
                if (child instanceof IHasName && child.getTagCode()==NED_PROPERTY) {
                    String name = ((IHasName)child).getName();
                    Map<String, PropertyElementEx> propertyMap = map.get(name);
                    if (propertyMap == null) {
                        propertyMap = new HashMap<String, PropertyElementEx>();
                        map.put(name, propertyMap);
                    }
                    PropertyElementEx property = (PropertyElementEx)child;
                    propertyMap.put(property.getIndex(), property);
                }
            }
        }
        return map;
    }

    public static PropertyElementEx getProperty(IHasProperties element, String name, String index) {
        Map<String, PropertyElementEx> propertyMap = element.getProperties().get(name);
        if (propertyMap == null)
            return null;
        else
            return propertyMap.get(index == null ? PropertyElementEx.DEFAULT_PROPERTY_INDEX : index);
    }

    public static ArrayList<String> getPropertyValues(IHasProperties element, String propertyName) {
        PropertyElementEx propertyElement = getProperty(element, propertyName, null);
        ArrayList<String> properties = new ArrayList<String>();

        if (propertyElement != null)
            for (PropertyKeyElement propertyKey = propertyElement.getFirstPropertyKeyChild(); propertyKey != null; propertyKey = propertyKey.getNextPropertyKeySibling())
                for (LiteralElement literal = propertyKey.getFirstLiteralChild(); literal != null; literal = literal.getNextLiteralSibling())
                    properties.add(literal.getValue());

        return properties;
    }

    public static void setPropertyValues(IHasProperties element, String propertyName, Collection<String> values) {
        NedElementFactory factory = NedElementFactoryEx.getInstance();
        PropertyElementEx propertyElement = NedElementUtilEx.getProperty(element, propertyName, null);

        if (propertyElement == null) {
            propertyElement = (PropertyElementEx)factory.createElement(NED_PROPERTY);
            propertyElement.setName(propertyName);
            ((INedElement)element).appendChild(propertyElement);
        }
        else
            propertyElement.removeAllChildren();

        addPropertyValues(element, propertyName, values);
    }

    public static void addPropertyValues(IHasProperties element, String propertyName, Collection<String> values) {
        NedElementFactory factory = NedElementFactoryEx.getInstance();
        PropertyElementEx propertyElement = NedElementUtilEx.getProperty(element, propertyName, null);

        if (propertyElement == null) {
            propertyElement = (PropertyElementEx)factory.createElement(NED_PROPERTY);
            propertyElement.setName(propertyName);
            ((INedElement)element).appendChild(propertyElement);
        }

        PropertyKeyElement propertyKey = (PropertyKeyElement)propertyElement.getFirstChildWithAttribute(NED_PROPERTY_KEY, "name", "");

        if (propertyKey == null) {
            propertyKey = (PropertyKeyElement)factory.createElement(NED_PROPERTY_KEY);
            propertyElement.appendChild(propertyKey);
        }

        for (String value : values) {
            LiteralElement literal = (LiteralElement)factory.createElement(NED_LITERAL);
            literal.setValue(value);
            literal.setText(value);
            literal.setType(NED_CONST_STRING);
            propertyKey.appendChild(literal);
        }
    }

    public static ArrayList<String> getLabels(IHasProperties element) {
        return getPropertyValues(element, "labels");
    }

    public static void setLabels(IHasProperties element, Collection<String> labels) {
        setPropertyValues(element, "labels", labels);
    }

    public static void addLabels(IHasProperties element, Collection<String> labels) {
        addPropertyValues(element, "labels", labels);
    }
}
