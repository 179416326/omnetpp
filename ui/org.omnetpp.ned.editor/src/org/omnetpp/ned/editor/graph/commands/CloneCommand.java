/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.
  
  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.editor.graph.commands;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.gef.commands.Command;

import org.omnetpp.ned.model.INEDElement;
import org.omnetpp.ned.model.ex.NEDElementUtilEx;
import org.omnetpp.ned.model.ex.NedFileElementEx;
import org.omnetpp.ned.model.interfaces.INedTypeElement;
import org.omnetpp.ned.model.pojo.TypesElement;

/**
 * Clones a set of INedTypeElement (only for toplevel types)
 *
 * @author rhornig
 */
public class CloneCommand extends Command {

    private List<INedTypeElement> newNodes;
    // nodes that should be cloned by the command
    private List<INedTypeElement> nodes;
    // parent node where cloned nodes should go
    private INEDElement parent;
    // sibling node before the cloned nodes should be inserted (null means insertion at the end)
    private INEDElement insertBefore;

    /**
     * @param parent The parent node where the newly cloned nodes should go
     * @param insertBefore A sibling in the parent before we should insert the cloned nodes
     *        <code>null</code> should be used to insert at the end of the child list
     */
    public CloneCommand(INEDElement parent, INEDElement insertBefore) {
        super("Clone");
        Assert.isTrue(parent instanceof NedFileElementEx || parent instanceof TypesElement, "The parent of a type must be a NedFile or a Types element");
        this.parent = parent;
        this.insertBefore = insertBefore;
        nodes = new LinkedList<INedTypeElement>();
    }

    /**
     * Add a new {@link INedTypeElement} to the cloning list.
     * @param nodeToBeCloned
     */
    public void add(INedTypeElement nodeToBeCloned) {
        nodes.add(nodeToBeCloned);
    }

    @Override
    public boolean canExecute() {
    	return parent != null && nodes.size() > 0;
    }

    // clone just a single node and insert it into the correct position
    protected INedTypeElement cloneElement(INedTypeElement oldNode) {
        // duplicate the subtree but do not add to the new parent yet
        INedTypeElement newNode = (INedTypeElement)oldNode.deepDup();

        // set a unique name
        NedFileElementEx nedFile = oldNode.getContainingNedFileElement();
        newNode.setName(NEDElementUtilEx.getUniqueNameForToplevelType(newNode.getName(), nedFile));

    	// insert into the parent at the correct position
        parent.insertChildBefore(insertBefore, newNode);

        // keep track of the new modules so we can delete them in undo
        newNodes.add(newNode);

        return newNode;
    }

    @Override
    public void execute() {
        redo();
    }

    @Override
    public void redo() {
        newNodes = new LinkedList<INedTypeElement>();
        for (INedTypeElement toBeCloned : nodes)
        	cloneElement(toBeCloned);
    }

    @Override
    public void undo() {
        for (INEDElement mod : newNodes)
            mod.removeFromParent();
    }

}