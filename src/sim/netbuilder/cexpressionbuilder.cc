//==========================================================================
//   CEXPRESSIONBUILDER.CC
//
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 2002-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `terms' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <algorithm>

#include "nedelements.h"
#include "nederror.h"
#include "cexpressionbuilder.h"
#include "cfunction.h"
#include "cnedfunction.h"
#include "cparvalue.h"
#include "xmlgenerator.h"
#include "nedsupport.h"

inline bool strnull(const char *s)
{
    return !s || !s[0];
}

static void dump(NEDElement *node)
{
    generateXML(std::cout, node, false);
    std::cout.flush();
}

cExpressionBuilder::cExpressionBuilder()
{
    elems = NULL;
}

cExpressionBuilder::~cExpressionBuilder()
{
}

void cExpressionBuilder::doNode(NEDElement *node)
{
    if (pos > limit)
        throw new cRuntimeError("dynamic module builder: expression too long");
    int tagcode = node->getTagCode();
    switch (tagcode)
    {
        case NED_OPERATOR:
            doOperator((OperatorNode *)node); break;
        case NED_FUNCTION:
            doFunction((FunctionNode *)node); break;
        case NED_IDENT:
            doIdent((IdentNode *)node); break;
        case NED_LITERAL:
            doLiteral((LiteralNode *)node); break;
        default:
            throw new cRuntimeError("dynamic module builder: unexpected tag in expression: %s", node->getTagName());
    }
}

void cExpressionBuilder::doOperator(OperatorNode *node)
{
    // push args first
    for (NEDElement *op=node->getFirstChild(); op; op=op->getNextSibling())
        doNode(op);

    // determine name and arg count
    const char *name = node->getName();
    NEDElement *op1 = node->getFirstChild();
    NEDElement *op2 = op1 ? op1->getNextSibling() : NULL;
    NEDElement *op3 = op2 ? op2->getNextSibling() : NULL;

    if (!op2)
    {
        // unary:
        if (!strcmp(name,"-"))
            elems[pos++] = cDynamicExpression::NEG;
        else if (!strcmp(name,"!"))
            elems[pos++] = cDynamicExpression::NOT;
        else if (!strcmp(name,"~"))
            elems[pos++] = cDynamicExpression::BIN_NOT;
        else
            throw new cRuntimeError("dynamic module builder: unexpected operator %s", name);
    }
    else if (!op3)
    {
        // binary:

        // arithmetic
        if (!strcmp(name,"+"))
            elems[pos++] = cDynamicExpression::ADD;
        else if (!strcmp(name,"-"))
            elems[pos++] = cDynamicExpression::SUB;
        else if (!strcmp(name,"*"))
            elems[pos++] = cDynamicExpression::MUL;
        else if (!strcmp(name,"/"))
            elems[pos++] = cDynamicExpression::DIV;
        else if (!strcmp(name,"%"))
            elems[pos++] = cDynamicExpression::MOD;
        else if (!strcmp(name,"^"))
            elems[pos++] = cDynamicExpression::POW;

        // logical
        else if (!strcmp(name,"=="))
            elems[pos++] = cDynamicExpression::EQ;
        else if (!strcmp(name,"!="))
            elems[pos++] = cDynamicExpression::NE;
        else if (!strcmp(name,"<"))
            elems[pos++] = cDynamicExpression::LT;
        else if (!strcmp(name,"<="))
            elems[pos++] = cDynamicExpression::LE;
        else if (!strcmp(name,">"))
            elems[pos++] = cDynamicExpression::GT;
        else if (!strcmp(name,">="))
            elems[pos++] = cDynamicExpression::GE;
        else if (!strcmp(name,"&&"))
            elems[pos++] = cDynamicExpression::AND;
        else if (!strcmp(name,"||"))
            elems[pos++] = cDynamicExpression::OR;
        else if (!strcmp(name,"##"))
            elems[pos++] = cDynamicExpression::XOR;

        // bitwise
        else if (!strcmp(name,"&"))
            elems[pos++] = cDynamicExpression::BIN_AND;
        else if (!strcmp(name,"|"))
            elems[pos++] = cDynamicExpression::BIN_OR;
        else if (!strcmp(name,"#"))
            elems[pos++] = cDynamicExpression::BIN_XOR;
        else if (!strcmp(name,"<<"))
            elems[pos++] = cDynamicExpression::LSHIFT;
        else if (!strcmp(name,">>"))
            elems[pos++] = cDynamicExpression::RSHIFT;
        else
            throw new cRuntimeError("dynamic module builder: unexpected operator %s", name);
    }
    else
    {
        // tertiary can only be "?:"
        if (!strcmp(name,"?:"))
            elems[pos++] = cDynamicExpression::IIF;
        else
            throw new cRuntimeError("dynamic module builder: unexpected operator %s", name);
    }
}

void cExpressionBuilder::doFunction(FunctionNode *node)
{
    // get function name, arg count, args
    const char *funcname = node->getName();
    int argcount = node->getNumChildren();

    // operators should be handled specially
    if (!strcmp(funcname,"index"))
    {
        if (!inSubcomponentScope)
            throw new cRuntimeError("dynamic module builder: `index' operator is only supported on submodule parameters");
        elems[pos++] = new NEDSupport::ModuleIndex();
    }
    else if (!strcmp(funcname,"const"))
    {
        throw new cRuntimeError("dynamic module builder: `const' operator: not yet!"); //XXX
    }
    else if (!strcmp(funcname,"sizeof"))
    {
        // operands are in a child "ident" node
        IdentNode *identnode = node->getFirstIdentChild();
        ASSERT(identnode);

        const char *ident = identnode->getName();
        const char *modulename = identnode->getModule();
        bool hasChild = identnode->getFirstChild()!=NULL;

        // XXX actually we could decide here from the NED declarations
        // if it's sizeof(parentModuleGateVector) or sizeof(submoduleVector),
        // we don't have to do it at runtime in the Sizeof functor class.
        if (strnull(modulename))
            elems[pos++] = new NEDSupport::Sizeof(ident, inSubcomponentScope, false);
        else //FIXME handle "this.ident"
            //XXX elems[pos++] = new NEDSupport::Sizeof(modulename, ident, inSubcomponentScope, hasChild);
            throw new cRuntimeError("dynamic module builder: sizeof(module.ident): not yet");
    }
    else // normal function
    {
        // push args first
        for (NEDElement *child=node->getFirstChild(); child; child=child->getNextSibling())
            doNode(child);

        // normal function: find it and add to reverse Polish expression
        cMathFunction *functype = cMathFunction::find(funcname,argcount);
        cNEDFunction *nedfunctype = cNEDFunction::find(funcname,argcount);
        if (functype)
            elems[pos++] = functype;
        else if (nedfunctype)
            elems[pos++] = nedfunctype;
        else
            throw new cRuntimeError("dynamic module builder: function %s with %d args not found", funcname, argcount);
    }
}

bool cExpressionBuilder::isLoopVar(const char *parname)
{
    const char **varNames = NEDSupport::LoopVar::getVarNames();
    int n = NEDSupport::LoopVar::getNumVars();
    for (int i=0; i<n; i++)
        if (strcmp(varNames[i], parname)==0)
            return true;
    return false;
}

void cExpressionBuilder::doIdent(IdentNode *node)
{
    const char *parname = node->getName();
    const char *modulename = node->getModule();
    bool hasChild = node->getFirstChild()!=NULL;

//FIXME handle "this." prefix!
    if (strnull(modulename) && isLoopVar(parname))
        elems[pos++] = new NEDSupport::LoopVar(parname);
    else if (strnull(modulename))
        elems[pos++] = new NEDSupport::ParameterRef(parname, inSubcomponentScope, false);
    else
        elems[pos++] = new NEDSupport::SiblingModuleParameterRef(modulename, parname, inSubcomponentScope, hasChild);
}

void cExpressionBuilder::doLiteral(LiteralNode *node)
{
    const char *val = node->getValue();
    switch (node->getType())
    {
        case NED_CONST_BOOL:   elems[pos++] = !strcmp(val,"true"); break;
        case NED_CONST_INT:    elems[pos++] = strtol(node->getValue(), NULL, 0); break; // this handles hex as well
        case NED_CONST_DOUBLE: elems[pos++] = atof(node->getValue()); break;
        case NED_CONST_UNIT:   elems[pos++] = strToSimtime(node->getValue()); break;//XXX
        case NED_CONST_STRING: elems[pos++] = node->getValue(); break;
        default: throw new cRuntimeError("dynamic module builder: evaluate: internal error: wrong constant type");
    }
}

cDynamicExpression *cExpressionBuilder::process(ExpressionNode *node,
                                                bool inSubcomponentScope)
{
    // create dynamically evaluated expression (reverse Polish).
    // we don't know the size in advance, so first collect it in elems[1000],
    // then make a copy
    this->inSubcomponentScope = inSubcomponentScope;
    elems = new cDynamicExpression::Elem[1000];
    pos = 0;
    limit = 990;

    doNode(node->getFirstChild());

    int n = pos;
    cDynamicExpression::Elem *newElems = new cDynamicExpression::Elem[n];
    for (int i=0; i<n; i++)
        newElems[i] = elems[i];

    cDynamicExpression *ret = new cDynamicExpression();
    ret->setExpression(newElems, n);

    delete [] elems;
    elems = NULL;

std::cout << "    nedelement to expr returning: " << ret->toString() << "\n"; //XXX

    return ret;
}

void cExpressionBuilder::assign(cParValue *par, cDynamicExpression *expr)
{
    //FIXME todo optimize: if it's a single constant, single XML value etc!

    par->setExpression(expr);
}

