//==========================================================================
// Part of the OMNeT++/OMNEST Discrete Event Simulation System
//
// Generated from ned.dtd by dtdclassgen.pl
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 2002-2004 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

//
// THIS IS A GENERATED FILE, DO NOT EDIT!
//


#ifndef __DTDVALIDATOR_H
#define __DTDVALIDATOR_H

#include "nedelements.h"
#include "neddtdvalidatorbase.h"

/**
 * GENERATED CLASS. Validates a NEDElement tree by the DTD.
 * 
 * @ingroup Validation
 */
class NEDDTDValidator : public NEDDTDValidatorBase
{
  public:
    NEDDTDValidator() {}
    virtual ~NEDDTDValidator() {}

    /** @name Validation functions */
    //@{
    virtual void validateElement(FilesNode *node);
    virtual void validateElement(NedFileNode *node);
    virtual void validateElement(WhitespaceNode *node);
    virtual void validateElement(ImportNode *node);
    virtual void validateElement(PropertydefNode *node);
    virtual void validateElement(ExtendsNode *node);
    virtual void validateElement(InterfaceNameNode *node);
    virtual void validateElement(SimpleModuleNode *node);
    virtual void validateElement(ModuleInterfaceNode *node);
    virtual void validateElement(CompoundModuleNode *node);
    virtual void validateElement(ParametersNode *node);
    virtual void validateElement(ParamGroupNode *node);
    virtual void validateElement(ParamNode *node);
    virtual void validateElement(PropertyNode *node);
    virtual void validateElement(KeyValueNode *node);
    virtual void validateElement(GatesNode *node);
    virtual void validateElement(GateGroupNode *node);
    virtual void validateElement(GateNode *node);
    virtual void validateElement(SubmodulesNode *node);
    virtual void validateElement(SubmoduleNode *node);
    virtual void validateElement(ConnectionsNode *node);
    virtual void validateElement(ConnectionNode *node);
    virtual void validateElement(ChannelInterfaceNode *node);
    virtual void validateElement(ChannelNode *node);
    virtual void validateElement(ConnectionGroupNode *node);
    virtual void validateElement(LoopNode *node);
    virtual void validateElement(ConditionNode *node);
    virtual void validateElement(ExpressionNode *node);
    virtual void validateElement(OperatorNode *node);
    virtual void validateElement(FunctionNode *node);
    virtual void validateElement(RefNode *node);
    virtual void validateElement(ConstNode *node);
    virtual void validateElement(MsgFileNode *node);
    virtual void validateElement(CplusplusNode *node);
    virtual void validateElement(StructDeclNode *node);
    virtual void validateElement(ClassDeclNode *node);
    virtual void validateElement(MessageDeclNode *node);
    virtual void validateElement(EnumDeclNode *node);
    virtual void validateElement(EnumNode *node);
    virtual void validateElement(EnumFieldsNode *node);
    virtual void validateElement(EnumFieldNode *node);
    virtual void validateElement(MessageNode *node);
    virtual void validateElement(ClassNode *node);
    virtual void validateElement(StructNode *node);
    virtual void validateElement(FieldsNode *node);
    virtual void validateElement(FieldNode *node);
    virtual void validateElement(PropertiesNode *node);
    virtual void validateElement(MsgpropertyNode *node);
    virtual void validateElement(UnknownNode *node);
    //@}
};

#endif

