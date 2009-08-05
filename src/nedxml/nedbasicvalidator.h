//==========================================================================
// nedbasicvalidator.h -
//
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
// Contents:
//   class NEDBasicValidator
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 2002-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/


#ifndef __NEDBASICVALIDATOR_H
#define __NEDBASICVALIDATOR_H

#include "nedvalidator.h"


/**
 * Performs structural validation of components. Assumes the tree already
 * passed DTD validation. Every compound module and other component is
 * viewed in isolation: basic validation does NOT process imports or
 * examines validity of references to other module types, channel types, etc.
 *
 * @ingroup Validation
 */
class NEDBasicValidator : public NEDValidatorBase
{
  protected:
    bool parsedExpressions;

    // internal helper: verify that children of node with given tagcode have
    // all different values of the given attribute (e.g. "name")
    void checkUniqueness(NEDElement *node, int childtype, const char *attr);

    // internal helper: verify attributes
    void checkExpressionAttributes(NEDElement *node, const char *attrs[], bool optional[], int n);

  public:
    /**
     * Constructor. Bool argument specifies if the validator should expect
     * parsed or unparsed expressions in the tree (This affects the validation
     * process.)
     */
    NEDBasicValidator(bool parsedExpr) {parsedExpressions=parsedExpr;}

    /**
     * Destructor.
     */
    virtual ~NEDBasicValidator() {}

    /** @name Validator methods */
    //@{
    virtual void validateElement(FilesNode *node);
    virtual void validateElement(NedFileNode *node);
    virtual void validateElement(WhitespaceNode *node);
    virtual void validateElement(ImportNode *node);
    virtual void validateElement(PropertyDeclNode *node);
    virtual void validateElement(ExtendsNode *node);
    virtual void validateElement(InterfaceNameNode *node);
    virtual void validateElement(SimpleModuleNode *node);
    virtual void validateElement(ModuleInterfaceNode *node);
    virtual void validateElement(CompoundModuleNode *node);
    virtual void validateElement(ParametersNode *node);
    virtual void validateElement(ParamGroupNode *node);
    virtual void validateElement(ParamNode *node);
    virtual void validateElement(PatternNode *node);
    virtual void validateElement(PropertyNode *node);
    virtual void validateElement(PropertyKeyNode *node);
    virtual void validateElement(GatesNode *node);
    virtual void validateElement(TypesNode *node);
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
    virtual void validateElement(IdentNode *node);
    virtual void validateElement(LiteralNode *node);
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

